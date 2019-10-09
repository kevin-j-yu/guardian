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
import ai.rideos.android.common.utils.Polylines.GMSPolylineDecoder;
import ai.rideos.android.common.utils.Polylines.PolylineDecoder;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.model.TripStateModel.CancellationReason;
import ai.rideos.android.model.TripStateModel.CancellationReason.Source;
import ai.rideos.android.model.TripStateModel.Stage;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.AssignedVehicle;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.Stop;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.Stop.TypeCase;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripDefinition;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.CancelSource;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.Canceled;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.TripStateCase;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Plan;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.RouteLeg;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.VehicleActionCase;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.FindPredefinedStopRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetTripDefinitionRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetTripStateRequestRC;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.StopSearchParameters;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc.RideHailRiderServiceFutureStub;
import androidx.core.util.Pair;
import io.grpc.ManagedChannel;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import timber.log.Timber;

public class DefaultRiderTripStateInteractor
    extends GrpcServerInteractor<RideHailRiderServiceFutureStub>
    implements RiderTripStateInteractor {
    private final PolylineDecoder polylineDecoder;

    public DefaultRiderTripStateInteractor(final Supplier<ManagedChannel> channelSupplier,
                                           final User user) {
        this(channelSupplier, user, new GMSPolylineDecoder(), new DefaultSchedulerProvider());
    }

    public DefaultRiderTripStateInteractor(final Supplier<ManagedChannel> channelSupplier,
                                           final User user,
                                           final PolylineDecoder polylineDecoder,
                                           final SchedulerProvider schedulerProvider) {
        super(RideHailRiderServiceGrpc::newFutureStub, channelSupplier, user, schedulerProvider);
        this.polylineDecoder = polylineDecoder;
    }

    @Override
    public Single<TripStateModel> getTripState(final String tripId, final String fleetId) {
        return fetchAuthorizedStub()
            .flatMap(rideHailStub -> Single.zip(
                Single.fromFuture(rideHailStub.getTripStateRC(
                    GetTripStateRequestRC.newBuilder()
                        .setId(tripId)
                        .build()
                )),
                // TODO we can probably just call this once in whatever interactor requires the pickup/drop-off location
                Single.fromFuture(rideHailStub.getTripDefinition(
                    GetTripDefinitionRequest.newBuilder()
                        .setId(tripId)
                        .build()
                ))
                    .flatMap(response -> resolveStopsToPickupDropOff(response.getDefinition(), fleetId)),
                (stateResponse, pickupAndDropOff) -> new CompleteTripInfo(
                    stateResponse.getState(),
                    pickupAndDropOff.first,
                    pickupAndDropOff.second
                )
            ))
            .map(tripInfo -> {
                final TripState tripState = tripInfo.tripState;
                final LatLng pickup = tripInfo.pickup;
                final LatLng dropOff = tripInfo.dropOff;
                switch (tripState.getTripStateCase()) {
                    case WAITING_FOR_ASSIGNMENT:
                        return new TripStateModel(
                            Stage.WAITING_FOR_ASSIGNMENT,
                            null,
                            null,
                            null,
                            pickup,
                            dropOff,
                            Collections.emptyList(),
                            null
                        );
                    case DRIVING_TO_PICKUP:
                        final AssignedVehicle pickupVehicle = tripState.getDrivingToPickup().getAssignedVehicle();
                        return new TripStateModel(
                            Stage.DRIVING_TO_PICKUP,
                            getRouteInfoFromTrip(pickupVehicle, tripState.getTripStateCase(), tripId),
                            getVehicleInfoFromTrip(pickupVehicle.getInfo()),
                            getVehiclePosition(pickupVehicle),
                            pickup,
                            dropOff,
                            getWaypoints(pickupVehicle.getPlanThroughTripEnd(), tripState.getTripStateCase(), tripId),
                            null
                        );
                    case WAITING_FOR_PICKUP:
                        final AssignedVehicle waitingVehicle = tripState.getWaitingForPickup().getAssignedVehicle();
                        return new TripStateModel(
                            Stage.WAITING_FOR_PICKUP,
                            null,
                            getVehicleInfoFromTrip(waitingVehicle.getInfo()),
                            getVehiclePosition(waitingVehicle),
                            pickup,
                            dropOff,
                            Collections.emptyList(),
                            null
                        );
                    case DRIVING_TO_DROPOFF:
                        final AssignedVehicle dropOffVehicle = tripState.getDrivingToDropoff().getAssignedVehicle();
                        return new TripStateModel(
                            Stage.DRIVING_TO_DROP_OFF,
                            getRouteInfoFromTrip(dropOffVehicle, tripState.getTripStateCase(), tripId),
                            getVehicleInfoFromTrip(tripState.getDrivingToDropoff().getAssignedVehicle().getInfo()),
                            getVehiclePosition(dropOffVehicle),
                            pickup,
                            dropOff,
                            getWaypoints(dropOffVehicle.getPlanThroughTripEnd(), tripState.getTripStateCase(), tripId),
                            null
                        );
                    case COMPLETED:
                        return new TripStateModel(
                            Stage.COMPLETED,
                            null,
                            null,
                            null,
                            pickup,
                            dropOff,
                            Collections.emptyList(),
                            null
                        );
                    case CANCELED:
                        return new TripStateModel(
                            Stage.CANCELLED,
                            null,
                            null,
                            null,
                            pickup,
                            dropOff,
                            Collections.emptyList(),
                            getCancellationReason(tripState.getCanceled())
                        );
                    default:
                        return new TripStateModel(
                            Stage.UNKNOWN,
                            null,
                            null,
                            null,
                            pickup,
                            dropOff,
                            Collections.emptyList(),
                            null
                        );
                }
            });
    }

    private RouteInfoModel getRouteInfoFromTrip(final AssignedVehicle assignedVehicle,
                                                final TripStateCase state,
                                                final String tripId) {
        final List<Step> steps = stepsUntilCurrent(
            assignedVehicle.getPlanThroughTripEnd().getStepList(),
            tripId,
            state
        );

        final List<RouteLeg> legs = steps.stream()
            .filter(step -> step.getVehicleActionCase() == VehicleActionCase.DRIVE_TO_LOCATION)
            .map(step -> step.getDriveToLocation().getRoute())
            .collect(Collectors.toList());

        return new RouteInfoModel(
            legs.stream()
                .map(leg -> polylineDecoder.decode(leg.getPolyline()))
                .flatMap(List::stream)
                .collect(Collectors.toList()),
            legs.stream()
                .map(leg -> leg.getTravelTimeInSeconds() * 1000)
                .mapToLong(Double::longValue)
                .sum(),
            legs.stream()
                .map(RouteLeg::getDistanceInMeters)
                .mapToDouble(Double::doubleValue)
                .sum()
        );
    }

    /**
     * The ride-hail api gives all the steps until the end of the trip. For display purposes, we would only like to
     * show the steps until the current step, and nothing beyond.
     */
    private static List<Step> stepsUntilCurrent(final List<Step> steps, final String tripId, final TripStateCase stateCase) {
        if (stateCase == TripStateCase.DRIVING_TO_DROPOFF) {
            return steps;
        }
        final OptionalInt indexOpt = IntStream.range(0, steps.size())
            .filter(i -> steps.get(i).getTripId().equals(tripId)
                && steps.get(i).getVehicleActionCase() == VehicleActionCase.PICKUP_RIDER
            )
            .findFirst();
        if (!indexOpt.isPresent()) {
            Timber.e("Invalid plan: Could not find DRIVING_TO_DROP_OFF step in trip %s", tripId);
            return Collections.emptyList();
        }
        return steps.subList(0, indexOpt.getAsInt() + 1);
    }

    private static List<LatLng> getWaypoints(final Plan plan, final TripStateCase state, final String tripId) {
        final List<Step> steps = stepsUntilCurrent(plan.getStepList(), tripId, state);
        return steps.stream()
            .filter(step -> isDisplayableWaypoint(step, tripId))
            .map(Step::getPosition)
            .map(Locations::fromRideOsPosition)
            .collect(Collectors.toList());
    }

    /**
     * A displayable waypoint should have the following 2 traits:
     * 1. It should not be part of the rider's trip
     * 2. It should be a drive-to step
     * This filter basically leaves displayable points along the rider's route to show other trips that need to take
     * place along the way.
     */
    private static boolean isDisplayableWaypoint(final Step step, final String tripId) {
        return !step.getTripId().equals(tripId) && step.getVehicleActionCase() == VehicleActionCase.DRIVE_TO_LOCATION;
    }

    private static LocationAndHeading getVehiclePosition(final AssignedVehicle assignedVehicle) {
        return new LocationAndHeading(
            assignedVehicle.hasPosition()
                ? Locations.fromRideOsPosition(assignedVehicle.getPosition())
                : new LatLng(0, 0),
            assignedVehicle.hasHeading() ? assignedVehicle.getHeading().getValue() : 0f
        );
    }

    private static VehicleInfo getVehicleInfoFromTrip(final RideHailCommons.VehicleInfo assignedVehicle) {
        final ContactInfo contactInfo;
        if (assignedVehicle.getDriverInfo().hasContactInfo()) {
            contactInfo = new ContactInfo(
                assignedVehicle.getDriverInfo().getContactInfo().getName(),
                assignedVehicle.getDriverInfo().getContactInfo().getPhoneNumber(),
                assignedVehicle.getDriverInfo().getContactInfo().getContactUrl()
            );
        } else {
            contactInfo = new ContactInfo("", "", "");
        }
        return new VehicleInfo(assignedVehicle.getLicensePlate(), contactInfo);
    }

    private static CancellationReason getCancellationReason(final Canceled canceledState) {
        return new CancellationReason(
            translateCancellationSource(canceledState.getSource()),
            canceledState.getDescription()
        );
    }

    private static CancellationReason.Source translateCancellationSource(final CancelSource source) {
        switch (source) {
            case RIDER:
                return Source.RIDER;
            case DRIVER:
                return Source.DRIVER;
            default:
                return Source.INTERNAL;
        }
    }

    private Single<Pair<LatLng, LatLng>> resolveStopsToPickupDropOff(final TripDefinition tripDefinition,
                                                                     final String fleetId) {
        return Single.zip(
            getLocationFromStop(tripDefinition.getPickupDropoff().getPickup(), fleetId),
            getLocationFromStop(tripDefinition.getPickupDropoff().getDropoff(), fleetId),
            Pair::create
        );
    }

    private Single<LatLng> getLocationFromStop(final Stop stop, final String fleetId) {
        if (stop.getTypeCase() == TypeCase.POSITION) {
            return Single.just(Locations.fromRideOsPosition(stop.getPosition()));
        }
        return fetchAuthorizedStubAndExecute(stub -> stub.findPredefinedStop(
            FindPredefinedStopRequest.newBuilder()
                .setFleetId(fleetId)
                .setSearchParameters(StopSearchParameters.newBuilder().setStopId(stop.getPredefinedStopId()))
                .build()
        ))
            .firstOrError()
            .map(stopResponse -> Locations.fromRideOsPosition(stopResponse.getPredefinedStop(0).getPosition()));
    }

    private static class CompleteTripInfo {
        private final TripState tripState;
        private final LatLng pickup;
        private final LatLng dropOff;

        private CompleteTripInfo(final TripState tripState,
                                 final LatLng pickup,
                                 final LatLng dropOff) {
            this.tripState = tripState;
            this.pickup = pickup;
            this.dropOff = dropOff;
        }
    }
}
