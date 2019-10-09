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
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.common.model.VehicleInfo.ContactInfo;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.model.VehicleDisplayRouteLeg;
import ai.rideos.android.model.VehicleRegistration;
import ai.rideos.android.model.VehicleStatus;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.DriverInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleDefinition;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleInfoUpdate;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.RouteLeg;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.CancelTripRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.CompleteStepRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.CreateVehicleRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.GetVehicleInfoRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.GetVehicleInfoResponse;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.GetVehicleStateRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.RejectTripRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.SetRouteLegs;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.SetRouteLegs.LegDefinition;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.SetToNotReady;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.SetToReady;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.UpdatePosition;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriverServiceGrpc;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriverServiceGrpc.RideHailDriverServiceFutureStub;
import androidx.core.util.Pair;
import com.google.maps.android.PolyUtil;
import com.google.protobuf.FloatValue;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultDriverVehicleInteractor
    extends GrpcServerInteractor<RideHailDriverServiceFutureStub>
    implements DriverVehicleInteractor {
    public interface PolylineEncoder {
        String encodePolyline(final List<LatLng> route);
    }

    private final PolylineEncoder polylineEncoder;

    public DefaultDriverVehicleInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        this(channelSupplier, user, new DefaultPolylineEncoder(), new DefaultSchedulerProvider());
    }

    public DefaultDriverVehicleInteractor(final Supplier<ManagedChannel> channelSupplier,
                                          final User user,
                                          final PolylineEncoder polylineEncoder,
                                          final SchedulerProvider schedulerProvider) {
        super(RideHailDriverServiceGrpc::newFutureStub, channelSupplier, user, schedulerProvider);
        this.polylineEncoder = polylineEncoder;
    }

    @Override
    public Single<VehicleStatus> getVehicleStatus(final String vehicleId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.getVehicleState(
            GetVehicleStateRequest.newBuilder().setId(vehicleId).build()
        ))
            .map(stateResponse ->
                stateResponse.getState().getReadiness() ? VehicleStatus.READY : VehicleStatus.NOT_READY
            )
            .firstOrError()
            .onErrorResumeNext(e -> {
                final Throwable cause = e.getCause();
                if (cause instanceof StatusRuntimeException) {
                    if (((StatusRuntimeException) cause).getStatus().getCode() == Code.NOT_FOUND) {
                        return Single.just(VehicleStatus.UNREGISTERED);
                    }
                }
                return Single.error(e);
            });
    }

    @Override
    public Completable createVehicle(final String vehicleId, final String fleetId, final VehicleRegistration vehicleRegistration) {
        return fetchAuthorizedStubAndExecute(stub -> stub.createVehicle(
            CreateVehicleRequest.newBuilder()
                .setId(vehicleId)
                .setInfo(
                    RideHailCommons.VehicleInfo.newBuilder()
                        .setDriverInfo(DriverInfo.newBuilder().setContactInfo(
                            RideHailCommons.ContactInfo.newBuilder()
                                .setName(vehicleRegistration.getPreferredName())
                                .setPhoneNumber(vehicleRegistration.getPhoneNumber())
                        ))
                        .setLicensePlate(vehicleRegistration.getLicensePlate())
                )
                .setFleetId(fleetId)
                .setDefinition(
                    VehicleDefinition.newBuilder().setRiderCapacity(vehicleRegistration.getRiderCapacity())
                )
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Completable markVehicleReady(final String vehicleId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.updateVehicleState(
            UpdateVehicleStateRequest.newBuilder()
                .setId(vehicleId)
                .setSetToReady(SetToReady.getDefaultInstance())
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Completable markVehicleNotReady(final String vehicleId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.updateVehicleState(
            UpdateVehicleStateRequest.newBuilder()
                .setId(vehicleId)
                .setSetToNotReady(SetToNotReady.getDefaultInstance())
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Completable finishSteps(final String vehicleId, final String taskId, final List<String> stepIds) {
        return Observable.fromIterable(stepIds)
            .concatMapCompletable(stepId ->
                fetchAuthorizedStubAndExecute(stub -> stub.completeStep(
                    CompleteStepRequest.newBuilder()
                        .setVehicleId(vehicleId)
                        .setTripId(taskId)
                        .setStepId(stepId)
                        .build()
                ))
                    .ignoreElements()
            );
    }

    @Override
    public Completable rejectTrip(final String vehicleId, final String tripId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.rejectTrip(RejectTripRequest.newBuilder()
            .setVehicleId(vehicleId)
            .setTripId(tripId)
            .build())
        )
            .ignoreElements();
    }

    @Override
    public Completable cancelTrip(final String tripId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.cancelTrip(CancelTripRequest.newBuilder()
            .setId(tripId)
            .build())
        )
            .ignoreElements();
    }

    @Override
    public Completable updateVehicleLocation(final String vehicleId, final LocationAndHeading locationAndHeading) {
        return fetchAuthorizedStubAndExecute(stub -> stub.updateVehicleState(
            UpdateVehicleStateRequest.newBuilder()
                .setId(vehicleId)
                .setUpdatePosition(
                    UpdatePosition.newBuilder()
                        .setUpdatedHeading(FloatValue.newBuilder().setValue(locationAndHeading.getHeading()))
                        .setUpdatedPosition(Locations.toRideOsPosition(locationAndHeading.getLatLng()))
                )
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Completable updateVehicleRoute(final String vehicleId, final List<VehicleDisplayRouteLeg> updatedLegs) {
        final List<LegDefinition> legDefinitions = updatedLegs.stream()
            .map(displayLeg -> {
                final Pair<String, String> prevTripAndStep = displayLeg.getPreviousTripAndStep()
                    .orElse(Pair.create("", ""));
                return LegDefinition.newBuilder()
                    .setFromTripId(prevTripAndStep.first)
                    .setFromStepId(prevTripAndStep.second)
                    .setToTripId(displayLeg.getRoutableTripAndStep().first)
                    .setToStepId(displayLeg.getRoutableTripAndStep().second)
                    .setRouteLeg(getRouteLegFromRouteInfo(displayLeg.getRoute()))
                    .build();
            })
            .collect(Collectors.toList());
        return fetchAuthorizedStubAndExecute(stub -> stub.updateVehicleState(
            UpdateVehicleStateRequest.newBuilder()
                .setId(vehicleId)
                .setSetRouteLegs(SetRouteLegs.newBuilder().addAllLegDefinition(legDefinitions))
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Completable updateContactInfo(final String vehicleId, final ContactInfo contactInfo) {
        return fetchAuthorizedStubAndExecute(stub -> stub.updateVehicle(
            UpdateVehicleRequest.newBuilder()
                .setId(vehicleId)
                .setUpdatedVehicleInfo(
                    VehicleInfoUpdate.newBuilder()
                        .setDriverInfo(DriverInfo.newBuilder().setContactInfo(
                            RideHailCommons.ContactInfo.newBuilder()
                                .setName(contactInfo.getName())
                                .setPhoneNumber(contactInfo.getPhoneNumber())
                                .setContactUrl(contactInfo.getUrl())
                        ))
                )
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Completable updateLicensePlate(final String vehicleId, final String licensePlate) {
        return fetchAuthorizedStubAndExecute(stub -> stub.updateVehicle(
            UpdateVehicleRequest.newBuilder()
                .setId(vehicleId)
                .setUpdatedVehicleInfo(
                    VehicleInfoUpdate.newBuilder()
                        .setLicensePlate(StringValue.newBuilder().setValue(licensePlate))
                )
                .build()
        ))
            .ignoreElements();
    }

    @Override
    public Single<VehicleInfo> getVehicleInfo(final String vehicleId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.getVehicleInfo(
            GetVehicleInfoRequest.newBuilder()
                .setId(vehicleId)
                .build()
        ))
            .map(GetVehicleInfoResponse::getInfo)
            .map(info -> new VehicleInfo(
                info.getLicensePlate(),
                new VehicleInfo.ContactInfo(
                    info.getDriverInfo().getContactInfo().getName(),
                    info.getDriverInfo().getContactInfo().getPhoneNumber(),
                    info.getDriverInfo().getContactInfo().getContactUrl()
                )
            ))
            .firstOrError();
    }

    private RouteLeg getRouteLegFromRouteInfo(final RouteInfoModel routeInfo) {
        return RouteLeg.newBuilder()
            .setPolyline(polylineEncoder.encodePolyline(routeInfo.getRoute()))
            .setDistanceInMeters(routeInfo.getTravelDistanceMeters())
            .setTravelTimeInSeconds(((double) routeInfo.getTravelTimeMillis()) / 1000)
            .build();
    }

    private static class DefaultPolylineEncoder implements PolylineEncoder {
        @Override
        public String encodePolyline(final List<LatLng> route) {
            return PolyUtil.encode(
                route.stream()
                    .map(Locations::toGoogleLatLng)
                    .collect(Collectors.toList())
            );
        }
    }
}
