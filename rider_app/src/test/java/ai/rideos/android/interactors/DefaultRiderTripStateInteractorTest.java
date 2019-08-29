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
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.common.utils.Polylines.PolylineDecoder;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.model.TripStateModel.CancellationReason;
import ai.rideos.android.model.TripStateModel.CancellationReason.Source;
import ai.rideos.android.model.TripStateModel.Stage;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.AssignedVehicle;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.ContactInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.DriverInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.PickupDropoff;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.PredefinedStop;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.Stop;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripDefinition;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.CancelSource;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.Canceled;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.Completed;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.DrivingToDropoff;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.DrivingToPickup;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.WaitingForAssignment;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripState.WaitingForPickup;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Plan;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.DriveToLocation;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.DropoffRider;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.PickupRider;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.RouteLeg;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.FindPredefinedStopRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.FindPredefinedStopResponse;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetTripDefinitionResponse;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetTripStateResponseRC;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.StopSearchParameters;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc;
import com.google.protobuf.FloatValue;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRiderTripStateInteractorTest {
    private static final String API_TOKEN = "token";
    private static final String TASK_ID = "task-1";
    private static final String VEHICLE_ID = "vehicle-1";
    private static final String FLEET_ID = "fleet-1";
    private static final VehicleInfo VEHICLE_INFO = VehicleInfo.newBuilder()
        .setLicensePlate("license-1")
        .setDriverInfo(DriverInfo.newBuilder().setContactInfo(
            ContactInfo.newBuilder().setContactUrl("https://rideos.ai")
        ))
        .build();
    private static final LatLng ORIGIN = new LatLng(0, 0);
    private static final LatLng DESTINATION = new LatLng(1, 1);

    private static final RouteLeg ROUTE_TO_PICKUP = RouteLeg.newBuilder()
        .setDistanceInMeters(1000)
        .setPolyline("abcdefg")
        .setTravelTimeInSeconds(60)
        .build();
    private static final RouteLeg ROUTE_TO_DROP_OFF = RouteLeg.newBuilder()
        .setDistanceInMeters(2000)
        .setPolyline("hijklmno")
        .setTravelTimeInSeconds(120)
        .build();

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final RideHailRiderServiceGrpc.RideHailRiderServiceImplBase mockBase = Mockito.mock(
        RideHailRiderServiceGrpc.RideHailRiderServiceImplBase.class
    );
    private DefaultRiderTripStateInteractor interactorUnderTest;

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

        final PolylineDecoder polylineDecoder = Mockito.mock(PolylineDecoder.class);
        Mockito.when(polylineDecoder.decode(ROUTE_TO_PICKUP.getPolyline()))
            .thenReturn(Collections.singletonList(ORIGIN));
        Mockito.when(polylineDecoder.decode(ROUTE_TO_DROP_OFF.getPolyline()))
            .thenReturn(Collections.singletonList(DESTINATION));

        interactorUnderTest = new DefaultRiderTripStateInteractor(
            () -> channel,
            user,
            polylineDecoder,
            new TrampolineSchedulerProvider()
        );

        mockDefinitionResponse(GetTripDefinitionResponse.newBuilder()
            .setDefinition(TripDefinition.newBuilder()
                .setPickupDropoff(PickupDropoff.newBuilder()
                    .setPickup(Stop.newBuilder().setPosition(Locations.toRideOsPosition(ORIGIN)))
                    .setDropoff(Stop.newBuilder().setPosition(Locations.toRideOsPosition(DESTINATION)))
                )
            )
            .build()
        );
    }

    @Test
    public void testCanGetWaitingForAssignmentState() {
        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setWaitingForAssignment(WaitingForAssignment.getDefaultInstance())
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.WAITING_FOR_ASSIGNMENT,
            null,
            null,
            null,
            ORIGIN,
            DESTINATION,
            Collections.emptyList(),
            null
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
    }

    @Test
    public void testCanGetDrivingToPickupState() {
        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setDrivingToPickup(DrivingToPickup.newBuilder()
                .setAssignedVehicle(AssignedVehicle.newBuilder()
                    .setId(VEHICLE_ID)
                    .setHeading(FloatValue.newBuilder().setValue(15.0f))
                    .setPosition(Locations.toRideOsPosition(ORIGIN))
                    .setInfo(VEHICLE_INFO)
                    .setPlanThroughTripEnd(Plan.newBuilder()
                        .addStep(Step.newBuilder()
                            .setId("step-0")
                            .setTripId(TASK_ID)
                            .setDriveToLocation(DriveToLocation.newBuilder().setRoute(ROUTE_TO_PICKUP))
                        )
                        .addStep(Step.newBuilder()
                            .setId("step-1")
                            .setTripId(TASK_ID)
                            .setPickupRider(PickupRider.getDefaultInstance())
                        )
                        .addStep(Step.newBuilder()
                            .setId("step-2")
                            .setTripId(TASK_ID)
                            .setDriveToLocation(DriveToLocation.newBuilder().setRoute(ROUTE_TO_DROP_OFF))
                        )
                        .addStep(Step.newBuilder()
                            .setId("step-3")
                            .setTripId(TASK_ID)
                            .setDropoffRider(DropoffRider.getDefaultInstance())
                        )
                    )
                )
            )
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.DRIVING_TO_PICKUP,
            new RouteInfoModel(
                Collections.singletonList(ORIGIN),
                (long) ROUTE_TO_PICKUP.getTravelTimeInSeconds() * 1000,
                ROUTE_TO_PICKUP.getDistanceInMeters()
            ),
            new ai.rideos.android.model.VehicleInfo(
                VEHICLE_INFO.getLicensePlate(),
                new ai.rideos.android.model.VehicleInfo.ContactInfo(
                    VEHICLE_INFO.getDriverInfo().getContactInfo().getContactUrl()
                )
            ),
            new LocationAndHeading(ORIGIN, 15.0f),
            ORIGIN,
            DESTINATION,
            Collections.emptyList(),
            null
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
    }

    @Test
    public void testCanGetWaitingForPickupState() {
        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setWaitingForPickup(WaitingForPickup.newBuilder()
                .setAssignedVehicle(AssignedVehicle.newBuilder()
                    .setId(VEHICLE_ID)
                    .setHeading(FloatValue.newBuilder().setValue(15.0f))
                    .setPosition(Locations.toRideOsPosition(DESTINATION))
                    .setInfo(VEHICLE_INFO)
                    .setPlanThroughTripEnd(Plan.newBuilder()
                        .addStep(Step.newBuilder()
                            .setId("step-1")
                            .setTripId(TASK_ID)
                            .setPickupRider(PickupRider.getDefaultInstance())
                        )
                        .addStep(Step.newBuilder()
                            .setId("step-0")
                            .setTripId(TASK_ID)
                            .setDriveToLocation(DriveToLocation.newBuilder().setRoute(ROUTE_TO_DROP_OFF))
                        )
                        .addStep(Step.newBuilder()
                            .setId("step-1")
                            .setTripId(TASK_ID)
                            .setDropoffRider(DropoffRider.getDefaultInstance())
                        )
                    )
                )
            )
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.WAITING_FOR_PICKUP,
            null,
            new ai.rideos.android.model.VehicleInfo(
                VEHICLE_INFO.getLicensePlate(),
                new ai.rideos.android.model.VehicleInfo.ContactInfo(
                    VEHICLE_INFO.getDriverInfo().getContactInfo().getContactUrl()
                )
            ),
            new LocationAndHeading(DESTINATION, 15.0f),
            ORIGIN,
            DESTINATION,
            Collections.emptyList(),
            null
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
    }

    @Test
    public void testCanGetDrivingToDropOffState() {
        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setDrivingToDropoff(DrivingToDropoff.newBuilder()
                .setAssignedVehicle(AssignedVehicle.newBuilder()
                    .setId(VEHICLE_ID)
                    .setHeading(FloatValue.newBuilder().setValue(15.0f))
                    .setPosition(Locations.toRideOsPosition(DESTINATION))
                    .setInfo(VEHICLE_INFO)
                    .setPlanThroughTripEnd(Plan.newBuilder()
                        .addStep(Step.newBuilder()
                            .setId("step-0")
                            .setTripId(TASK_ID)
                            .setDriveToLocation(DriveToLocation.newBuilder().setRoute(ROUTE_TO_DROP_OFF))
                        )
                        .addStep(Step.newBuilder()
                            .setId("step-1")
                            .setTripId(TASK_ID)
                            .setDropoffRider(DropoffRider.getDefaultInstance())
                        )
                    )
                )
            )
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.DRIVING_TO_DROP_OFF,
            new RouteInfoModel(
                Collections.singletonList(DESTINATION),
                (long) ROUTE_TO_DROP_OFF.getTravelTimeInSeconds() * 1000,
                ROUTE_TO_DROP_OFF.getDistanceInMeters()
            ),
            new ai.rideos.android.model.VehicleInfo(
                VEHICLE_INFO.getLicensePlate(),
                new ai.rideos.android.model.VehicleInfo.ContactInfo(
                    VEHICLE_INFO.getDriverInfo().getContactInfo().getContactUrl()
                )
            ),
            new LocationAndHeading(DESTINATION, 15.0f),
            ORIGIN,
            DESTINATION,
            Collections.emptyList(),
            null
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
    }

    @Test
    public void testCanGetCompletedState() {
        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setCompleted(Completed.getDefaultInstance())
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.COMPLETED,
            null,
            null,
            null,
            ORIGIN,
            DESTINATION,
            Collections.emptyList(),
            null
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
    }

    @Test
    public void testCanGetCancelledState() {
        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setCanceled(Canceled.newBuilder()
                .setSource(CancelSource.RIDER)
                .setDescription("description")
            )
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.CANCELLED,
            null,
            null,
            null,
            ORIGIN,
            DESTINATION,
            Collections.emptyList(),
            new CancellationReason(Source.RIDER, "description")
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanResolvePickupAndDropOffWhenGivenAsStopIds() {
        mockDefinitionResponse(GetTripDefinitionResponse.newBuilder()
            .setDefinition(TripDefinition.newBuilder()
                .setPickupDropoff(PickupDropoff.newBuilder()
                    .setPickup(Stop.newBuilder().setPredefinedStopId("pickup-stop"))
                    .setDropoff(Stop.newBuilder().setPosition(Locations.toRideOsPosition(DESTINATION)))
                )
            )
            .build()
        );
        Mockito.doAnswer(invocation -> {
            final StreamObserver<FindPredefinedStopResponse> observer =
                (StreamObserver<FindPredefinedStopResponse>) invocation.getArguments()[1];
            observer.onNext(FindPredefinedStopResponse.newBuilder()
                .addPredefinedStop(PredefinedStop.newBuilder()
                    .setPosition(Locations.toRideOsPosition(new LatLng(2, 2)))
                )
                .build()
            );
            observer.onCompleted();
            return null;
        }).when(mockBase).findPredefinedStop(Mockito.any(), Mockito.any());

        final GetTripStateResponseRC responseToReturn = GetTripStateResponseRC.newBuilder().setState(
            TripState.newBuilder().setCompleted(Completed.getDefaultInstance())
        )
            .build();
        final TripStateModel expectedModel = new TripStateModel(
            Stage.COMPLETED,
            null,
            null,
            null,
            new LatLng(2, 2),
            DESTINATION,
            Collections.emptyList(),
            null
        );

        mockStateResponse(responseToReturn);
        interactorUnderTest.getTripState(TASK_ID, FLEET_ID).test()
            .assertValueAt(0, expectedModel);
        final FindPredefinedStopRequest expectedStopRequest = FindPredefinedStopRequest.newBuilder()
            .setFleetId(FLEET_ID)
            .setSearchParameters(StopSearchParameters.newBuilder().setStopId("pickup-stop"))
            .build();
        Mockito.verify(mockBase).findPredefinedStop(Mockito.eq(expectedStopRequest), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    private void mockStateResponse(final GetTripStateResponseRC response) {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetTripStateResponseRC> observer =
                (StreamObserver<GetTripStateResponseRC>) invocation.getArguments()[1];
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(mockBase).getTripStateRC(Mockito.any(), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    private void mockDefinitionResponse(final GetTripDefinitionResponse response) {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetTripDefinitionResponse> observer =
                (StreamObserver<GetTripDefinitionResponse>) invocation.getArguments()[1];
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(mockBase).getTripDefinition(Mockito.any(), Mockito.any());
    }

}
