/**
 * Copyright 2018-2019 rideOS, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.rideos.android.interactors;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.GrpcServerInteractor;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.model.ContactInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.PickupDropoff;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.RiderInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.Stop;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripDefinition;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleFilter;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.CancelTripRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.ChangeTripDefinitionRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.ChangeTripDefinitionRequest.ChangePickup;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.DispatchParameters;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetActiveTripIdRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetActiveTripIdResponse.TypeCase;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.RequestTripRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc.RideHailRiderServiceFutureStub;
import io.grpc.ManagedChannel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class DefaultRiderTripInteractor
    extends GrpcServerInteractor<RideHailRiderServiceFutureStub>
    implements RiderTripInteractor {
    private final Supplier<String> tripIdSupplier;

    public DefaultRiderTripInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        this(
            channelSupplier,
            user,
            () -> UUID.randomUUID().toString(),
            new DefaultSchedulerProvider()
        );
    }

    public DefaultRiderTripInteractor(final Supplier<ManagedChannel> channelSupplier,
                                      final User user,
                                      final Supplier<String> tripIdSupplier,
                                      final SchedulerProvider schedulerProvider) {
        super(RideHailRiderServiceGrpc::newFutureStub, channelSupplier, user, schedulerProvider);
        this.tripIdSupplier = tripIdSupplier;
    }

    @Override
    public Observable<String> createTripForPassenger(final String passengerId,
                                                     final ContactInfo contactInfo,
                                                     final String fleetId,
                                                     final int numPassengers,
                                                     final TaskLocation pickupLocation,
                                                     final TaskLocation dropOffLocation) {
        return createTripWithParameters(
            passengerId,
            contactInfo,
            fleetId,
            numPassengers,
            pickupLocation,
            dropOffLocation,
            DispatchParameters.getDefaultInstance()
        );
    }

    @Override
    public Observable<String> createTripForPassengerAndVehicle(final String passengerId,
                                                               final ContactInfo contactInfo,
                                                               final String vehicleId,
                                                               final String fleetId,
                                                               final int numPassengers,
                                                               final TaskLocation pickupLocation,
                                                               final TaskLocation dropOffLocation) {
        return createTripWithParameters(
            passengerId,
            contactInfo,
            fleetId,
            numPassengers,
            pickupLocation,
            dropOffLocation,
            DispatchParameters.newBuilder()
                .addVehicleFilter(VehicleFilter.newBuilder().setVehicleId(vehicleId))
                .build()
        );
    }

    private Observable<String> createTripWithParameters(final String passengerId,
                                                        final ContactInfo contactInfo,
                                                        final String fleetId,
                                                        final int numPassengers,
                                                        final TaskLocation pickupLocation,
                                                        final TaskLocation dropOffLocation,
                                                        final DispatchParameters dispatchParameters) {
        final String tripId = tripIdSupplier.get();
        return fetchAuthorizedStubAndExecute(taskStub -> taskStub.requestTrip(
            RequestTripRequest.newBuilder()
                .setId(tripId)
                .setFleetId(fleetId)
                .setRiderId(passengerId)
                .setDefinition(
                    TripDefinition.newBuilder()
                        .setPickupDropoff(
                            PickupDropoff.newBuilder()
                                .setPickup(buildStop(pickupLocation))
                                .setDropoff(buildStop(dropOffLocation))
                                .setRiderCount(numPassengers)
                        )
                )
                .setInfo(
                    TripInfo.newBuilder().setRiderInfo(
                        RiderInfo.newBuilder().setContactInfo(
                            RideHailCommons.ContactInfo.newBuilder()
                                .setName(contactInfo.getName())
                        )
                    )
                )
                .setDispatchParameters(dispatchParameters)
                .build()
        ))
            .map(response -> tripId);
    }

    @Override
    public Completable cancelTrip(final String ignoredPassengerId, final String tripId) {
        return fetchAuthorizedStubAndExecute(taskStub -> taskStub.cancelTrip(
            CancelTripRequest.newBuilder()
                .setId(tripId)
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Observable<String> editPickup(final String tripId, final TaskLocation newPickupLocation) {
        final String newTripId = tripIdSupplier.get();
        return fetchAuthorizedStubAndExecute(taskStub -> taskStub.changeTripDefinition(
            ChangeTripDefinitionRequest.newBuilder()
                .setTripId(tripId)
                .setReplacementTripId(newTripId)
                .setChangePickup(
                    ChangePickup.newBuilder().setNewPickup(buildStop(newPickupLocation))
                )
                .build()
        ))
            .map(response -> newTripId);
    }

    @Override
    public Observable<Optional<String>> getCurrentTripForPassenger(final String passengerId) {
        return fetchAuthorizedStubAndExecute(taskStub -> taskStub.getActiveTripId(
            GetActiveTripIdRequest.newBuilder()
                .setRiderId(passengerId)
                .build()
        ))
            .map(response -> {
                if (response.getTypeCase() == TypeCase.ACTIVE_TRIP) {
                    return Optional.of(response.getActiveTrip().getId());
                }
                return Optional.<String>empty();
            })
            .onErrorReturnItem(Optional.empty());
    }

    private static Stop buildStop(final TaskLocation taskLocation) {
        if (taskLocation.getLocationId().isPresent()) {
            return Stop.newBuilder()
                .setPredefinedStopId(taskLocation.getLocationId().get())
                .build();
        }
        return Stop.newBuilder()
            .setPosition(Locations.toRideOsPosition(taskLocation.getLatLng()))
            .build();
    }
}
