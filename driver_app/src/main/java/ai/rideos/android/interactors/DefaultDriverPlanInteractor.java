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
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.ContactInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.VehicleActionCase;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.GetVehicleStateRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriverServiceGrpc;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriverServiceGrpc.RideHailDriverServiceFutureStub;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import timber.log.Timber;

public class DefaultDriverPlanInteractor
    extends GrpcServerInteractor<RideHailDriverServiceFutureStub>
    implements DriverPlanInteractor {

    public DefaultDriverPlanInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        this(channelSupplier, user, new DefaultSchedulerProvider());
    }

    public DefaultDriverPlanInteractor(final Supplier<ManagedChannel> channelSupplier,
                                       final User user,
                                       final SchedulerProvider schedulerProvider) {
        super(RideHailDriverServiceGrpc::newFutureStub, channelSupplier, user, schedulerProvider);
    }

    @Override
    public Observable<VehiclePlan> getPlanForVehicle(final String vehicleId) {
        return fetchAuthorizedStubAndExecute(stub -> stub.getVehicleState(
            GetVehicleStateRequest.newBuilder()
                .setId(vehicleId)
                .build()
        ))
            .map(response -> {
                final List<Step> steps = response.getState().getPlan().getStepList();
                final List<Waypoint> waypoints = new ArrayList<>();

                int stepIndex = 0;
                while (stepIndex < steps.size()) {
                    final Step step = steps.get(stepIndex);
                    switch (step.getVehicleActionCase()) {
                        case DRIVE_TO_LOCATION:
                            if (stepIndex + 1 >= steps.size()) {
                                Timber.e(
                                    "Received DRIVE_TO_LOCATION step without pickup or drop-off step after. Plan: %s",
                                    response.getState().getPlan().toString()
                                );
                                break;
                            }
                            final Step nextStep = steps.get(stepIndex + 1);
                            if (!nextStep.getTripId().equals(step.getTripId())) {
                                Timber.e(
                                    "Step after DRIVE_TO_LOCATION does not have same trip id. Plan: %s",
                                    response.getState().getPlan().toString()
                                );
                                break;
                            }
                            if (nextStep.getVehicleActionCase() == VehicleActionCase.PICKUP_RIDER) {
                                waypoints.add(getWaypointForStep(
                                    step,
                                    ActionType.DRIVE_TO_PICKUP,
                                    nextStep.getPickupRider().getRiderCount(),
                                    nextStep.getPickupRider().getRiderInfo().getContactInfo()
                                ));
                            } else {
                                waypoints.add(getWaypointForStep(
                                    step,
                                    ActionType.DRIVE_TO_DROP_OFF,
                                    nextStep.getDropoffRider().getRiderCount(),
                                    nextStep.getDropoffRider().getRiderInfo().getContactInfo(),
                                    nextStep.getId()
                                ));
                                // Skip over the drop-off step, since it has been accounted for as part of processing
                                // the drive to location step. This differs from how we handle the pickup step because,
                                // currently, we represent the action of picking up a rider
                                // (VehiclePlan.Action.ActionType.LOAD_RESOURCE), but do not have a corresponding
                                // representation for the action of dropping off a passenger (e.g., something like
                                // VehiclePlan.Action.ActionType.UNLOAD_RESOURCE). This representation could
                                // change in the future.
                                stepIndex++;
                            }
                            break;
                        case PICKUP_RIDER:
                            waypoints.add(getWaypointForStep(
                                step,
                                ActionType.LOAD_RESOURCE,
                                step.getPickupRider().getRiderCount(),
                                step.getPickupRider().getRiderInfo().getContactInfo()
                            ));
                            break;
                        case DROPOFF_RIDER:
                            // In the event we receive a drop-off rider without a drive-to-location before it, treat it
                            // as a drive to drop-off
                            waypoints.add(getWaypointForStep(
                                step,
                                ActionType.DRIVE_TO_DROP_OFF,
                                step.getDropoffRider().getRiderCount(),
                                step.getDropoffRider().getRiderInfo().getContactInfo()
                            ));
                            break;
                    }
                    stepIndex++;
                }

                return new VehiclePlan(waypoints);
            });
    }

    private static Waypoint getWaypointForStep(final Step step,
                                               final ActionType actionType,
                                               final int riderCount,
                                               final ContactInfo contactInfo,
                                               final String... additionalStepsIdsInWaypoint) {
        final LinkedHashSet<String> uniqueStepIds = new LinkedHashSet<>();
        uniqueStepIds.add(step.getId());
        uniqueStepIds.addAll(Arrays.asList(additionalStepsIdsInWaypoint));

        final String phoneNumber = contactInfo.getPhoneNumber().isEmpty() ? null : contactInfo.getPhoneNumber();

        return new Waypoint(
            step.getTripId(),
            new ArrayList<>(uniqueStepIds),
            new VehiclePlan.Action(
                Locations.fromRideOsPosition(step.getPosition()),
                actionType,
                new TripResourceInfo(riderCount, contactInfo.getName(), phoneNumber)
            )
        );
    }
}
