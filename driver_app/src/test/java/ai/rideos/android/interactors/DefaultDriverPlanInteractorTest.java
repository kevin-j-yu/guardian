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
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Plan;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.DriveToLocation;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.DropoffRider;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.PickupRider;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.GetVehicleStateResponse;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriverServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultDriverPlanInteractorTest {
    private static final String API_TOKEN = "token";
    private static final String VEHICLE_ID = "vehicle-1";

    private static final String TRIP_0 = "trip-0";
    private static final TripResourceInfo TRIP_0_RESOURCE = new TripResourceInfo(1);
    private static final LatLng PICKUP_0 = new LatLng(1, 2);
    private static final LatLng DROP_OFF_0 = new LatLng(3, 4);

    private static final String TRIP_1 = "trip-1";
    private static final TripResourceInfo TRIP_1_RESOURCE = new TripResourceInfo(2);
    private static final LatLng DROP_OFF_1 = new LatLng(7, 8);

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final RideHailDriverServiceGrpc.RideHailDriverServiceImplBase mockBase = Mockito.mock(
        RideHailDriverServiceGrpc.RideHailDriverServiceImplBase.class
    );
    private DefaultDriverPlanInteractor interactorUnderTest;

    @Before
    public void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
            .forName(serverName).directExecutor().addService(mockBase).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        final ManagedChannel channel = grpcCleanup.register(
            InProcessChannelBuilder.forName(serverName).directExecutor().build());

        final User user = Mockito.mock(User.class);
        Mockito.when(user.fetchUserToken()).thenReturn(Single.just(API_TOKEN));

        interactorUnderTest = new DefaultDriverPlanInteractor(
            () -> channel,
            user,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testGetPlanWithNoSteps() {
        mockPlanResponse(GetVehicleStateResponse.getDefaultInstance());

        interactorUnderTest.getPlanForVehicle(VEHICLE_ID).test()
            .assertValueAt(0, new VehiclePlan(Collections.emptyList()));
    }

    @Test
    public void testGetPlanWithPickupAndDropOffForSingleTrip() {
        final GetVehicleStateResponse response = GetVehicleStateResponse.newBuilder()
            .setState(VehicleState.newBuilder().setPlan(
                Plan.newBuilder()
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-0")
                            .setPosition(Locations.toRideOsPosition(PICKUP_0))
                            .setDriveToLocation(DriveToLocation.getDefaultInstance())
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-1")
                            .setPosition(Locations.toRideOsPosition(PICKUP_0))
                            .setPickupRider(PickupRider.newBuilder().setRiderCount(TRIP_0_RESOURCE.getNumPassengers()))
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-2")
                            .setPosition(Locations.toRideOsPosition(DROP_OFF_0))
                            .setDriveToLocation(DriveToLocation.getDefaultInstance())
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-3")
                            .setPosition(Locations.toRideOsPosition(DROP_OFF_0))
                            .setDropoffRider(DropoffRider.newBuilder().setRiderCount(TRIP_0_RESOURCE.getNumPassengers()))
                    )
            ))
            .build();

        mockPlanResponse(response);

        interactorUnderTest.getPlanForVehicle(VEHICLE_ID).test()
            .assertValueAt(0, new VehiclePlan(Arrays.asList(
                new Waypoint(
                    TRIP_0,
                    Collections.singletonList("step-0"),
                    new VehiclePlan.Action(PICKUP_0, ActionType.DRIVE_TO_PICKUP, TRIP_0_RESOURCE)
                ),
                new Waypoint(
                    TRIP_0,
                    Collections.singletonList("step-1"),
                    new VehiclePlan.Action(PICKUP_0, ActionType.LOAD_RESOURCE, TRIP_0_RESOURCE)
                ),
                new Waypoint(
                    TRIP_0,
                    Arrays.asList("step-2", "step-3"),
                    new VehiclePlan.Action(DROP_OFF_0, ActionType.DRIVE_TO_DROP_OFF, TRIP_0_RESOURCE)
                )
            )));
    }

    @Test
    public void testGetPlanWithPickupAndDropOffForMultipleTrips() {
        final GetVehicleStateResponse response = GetVehicleStateResponse.newBuilder()
            .setState(VehicleState.newBuilder().setPlan(
                Plan.newBuilder()
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-0")
                            .setPosition(Locations.toRideOsPosition(PICKUP_0))
                            .setDriveToLocation(DriveToLocation.getDefaultInstance())
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-1")
                            .setPosition(Locations.toRideOsPosition(PICKUP_0))
                            .setPickupRider(PickupRider.newBuilder().setRiderCount(TRIP_0_RESOURCE.getNumPassengers()))
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_1)
                            .setId("step-0")
                            .setPosition(Locations.toRideOsPosition(DROP_OFF_1))
                            .setDriveToLocation(DriveToLocation.getDefaultInstance())
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_1)
                            .setId("step-0")
                            .setPosition(Locations.toRideOsPosition(DROP_OFF_1))
                            .setDropoffRider(DropoffRider.newBuilder().setRiderCount(TRIP_1_RESOURCE.getNumPassengers()))
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-2")
                            .setPosition(Locations.toRideOsPosition(DROP_OFF_0))
                            .setDriveToLocation(DriveToLocation.getDefaultInstance())
                    )
                    .addStep(
                        Step.newBuilder()
                            .setTripId(TRIP_0)
                            .setId("step-2")
                            .setPosition(Locations.toRideOsPosition(DROP_OFF_0))
                            .setDropoffRider(DropoffRider.newBuilder().setRiderCount(TRIP_0_RESOURCE.getNumPassengers()))
                    )
            ))
            .build();

        mockPlanResponse(response);

        interactorUnderTest.getPlanForVehicle(VEHICLE_ID).test()
            .assertValueAt(0, new VehiclePlan(Arrays.asList(
                new Waypoint(
                    TRIP_0,
                    Collections.singletonList("step-0"),
                    new VehiclePlan.Action(PICKUP_0, ActionType.DRIVE_TO_PICKUP, TRIP_0_RESOURCE)
                ),
                new Waypoint(
                    TRIP_0,
                    Collections.singletonList("step-1"),
                    new VehiclePlan.Action(PICKUP_0, ActionType.LOAD_RESOURCE, TRIP_0_RESOURCE)
                ),
                new Waypoint(
                    TRIP_1,
                    Collections.singletonList("step-0"),
                    new VehiclePlan.Action(DROP_OFF_1, ActionType.DRIVE_TO_DROP_OFF, TRIP_1_RESOURCE)
                ),
                new Waypoint(
                    TRIP_0,
                    Collections.singletonList("step-2"),
                    new VehiclePlan.Action(DROP_OFF_0, ActionType.DRIVE_TO_DROP_OFF, TRIP_0_RESOURCE)
                )
            )));
    }

    @SuppressWarnings("unchecked")
    private void mockPlanResponse(final GetVehicleStateResponse response) {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetVehicleStateResponse> observer =
                (StreamObserver<GetVehicleStateResponse>) invocation.getArguments()[1];
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(mockBase).getVehicleState(Mockito.any(), Mockito.any());
    }
}
