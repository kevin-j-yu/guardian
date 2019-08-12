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
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.interactors.DefaultDriverVehicleInteractor.PolylineEncoder;
import ai.rideos.android.model.VehicleDisplayRouteLeg;
import ai.rideos.android.model.VehicleRegistration;
import ai.rideos.android.model.VehicleStatus;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.ContactInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.DriverInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleDefinition;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleState.Step.RouteLeg;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.CreateVehicleRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.CreateVehicleResponse;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.GetVehicleStateResponse;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.SetRouteLegs;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateRequest.SetRouteLegs.LegDefinition;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriver.UpdateVehicleStateResponse;
import ai.rideos.api.ride_hail_driver.v1.RideHailDriverServiceGrpc;
import androidx.core.util.Pair;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultDriverVehicleInteractorTest {
    private static final String API_TOKEN = "token";
    private static final String VEHICLE_ID = "vehicle-1";
    private static final String FLEET_ID = "fleet-1";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final RideHailDriverServiceGrpc.RideHailDriverServiceImplBase mockBase = Mockito.mock(
        RideHailDriverServiceGrpc.RideHailDriverServiceImplBase.class
    );
    private DefaultDriverVehicleInteractor interactorUnderTest;
    private PolylineEncoder polylineEncoder;

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
        polylineEncoder = Mockito.mock(PolylineEncoder.class);

        interactorUnderTest = new DefaultDriverVehicleInteractor(
            () -> channel,
            user,
            polylineEncoder,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testGetStatusWhenVehicleExistsAndIsReady() {
        mockStateResponse(GetVehicleStateResponse.newBuilder()
            .setState(VehicleState.newBuilder().setReadiness(true))
            .build()
        );

        interactorUnderTest.getVehicleStatus(VEHICLE_ID).test()
            .assertValueAt(0, VehicleStatus.READY);
    }

    @Test
    public void testGetStatusWhenVehicleExistsAndIsNotReady() {
        mockStateResponse(GetVehicleStateResponse.newBuilder()
            .setState(VehicleState.newBuilder().setReadiness(false))
            .build()
        );

        interactorUnderTest.getVehicleStatus(VEHICLE_ID).test()
            .assertValueAt(0, VehicleStatus.NOT_READY);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetStatusWhenVehicleDoesNotExist() {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetVehicleStateResponse> observer =
                (StreamObserver<GetVehicleStateResponse>) invocation.getArguments()[1];
            observer.onError(new StatusRuntimeException(Status.NOT_FOUND));
            return null;
        }).when(mockBase).getVehicleState(Mockito.any(), Mockito.any());

        interactorUnderTest.getVehicleStatus(VEHICLE_ID).test()
            .assertValueAt(0, VehicleStatus.UNREGISTERED)
            .assertNoErrors();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetStatusFailsWhenErrorThrownOtherThanStatusNotFound() {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetVehicleStateResponse> observer =
                (StreamObserver<GetVehicleStateResponse>) invocation.getArguments()[1];
            observer.onError(new IOException());
            return null;
        }).when(mockBase).getVehicleState(Mockito.any(), Mockito.any());

        interactorUnderTest.getVehicleStatus(VEHICLE_ID).test()
            .assertError(Exception.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanCreateVehicleFromRegistration() {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<CreateVehicleResponse> observer =
                (StreamObserver<CreateVehicleResponse>) invocation.getArguments()[1];
            observer.onNext(CreateVehicleResponse.getDefaultInstance());
            observer.onCompleted();
            return null;
        }).when(mockBase).createVehicle(Mockito.any(), Mockito.any());

        interactorUnderTest.createVehicle(
            VEHICLE_ID,
            FLEET_ID,
            new VehicleRegistration("name", "1234567", "abc123", 4)
        ).blockingAwait();

        Mockito.verify(mockBase).createVehicle(
            Mockito.eq(CreateVehicleRequest.newBuilder()
                .setId(VEHICLE_ID)
                .setInfo(
                    VehicleInfo.newBuilder()
                        .setDriverInfo(DriverInfo.newBuilder().setContactInfo(
                            ContactInfo.newBuilder().setName("name").setPhoneNumber("1234567")
                        ))
                        .setLicensePlate("abc123")
                )
                .setFleetId(FLEET_ID)
                .setDefinition(VehicleDefinition.newBuilder().setRiderCapacity(4))
                .build()),
            Mockito.any()
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanUpdateVehicleRouteFromListOfLegs() {
        final List<LatLng> route0 = Arrays.asList(new LatLng(0, 0), new LatLng(1, 1));
        Mockito.when(polylineEncoder.encodePolyline(route0)).thenReturn("route0");
        final List<LatLng> route1 = Arrays.asList(new LatLng(1, 1), new LatLng(2, 2));
        Mockito.when(polylineEncoder.encodePolyline(route1)).thenReturn("route1");
        final List<LatLng> route2 = Arrays.asList(new LatLng(2, 2), new LatLng(3, 3));
        Mockito.when(polylineEncoder.encodePolyline(route2)).thenReturn("route2");

        final List<VehicleDisplayRouteLeg> legsToSync = Arrays.asList(
            new VehicleDisplayRouteLeg(null, Pair.create("trip-0", "step-0"), new RouteInfoModel(route0, 6000, 100)),
            new VehicleDisplayRouteLeg(
                Pair.create("trip-0", "step-0"),
                Pair.create("trip-1", "step-0"),
                new RouteInfoModel(route1, 7000, 200)
            ),
            new VehicleDisplayRouteLeg(
                Pair.create("trip-1", "step-0"),
                Pair.create("trip-0", "step-1"),
                new RouteInfoModel(route2, 8000, 300)
            )
        );

        Mockito.when(polylineEncoder.encodePolyline(route0)).thenReturn("route0");

        final UpdateVehicleStateRequest expectedRequest = UpdateVehicleStateRequest.newBuilder()
            .setId(VEHICLE_ID)
            .setSetRouteLegs(SetRouteLegs.newBuilder()
                .addLegDefinition(
                    LegDefinition.newBuilder()
                        .setToStepId("step-0")
                        .setToTripId("trip-0")
                        .setRouteLeg(RouteLeg.newBuilder()
                            .setDistanceInMeters(100)
                            .setTravelTimeInSeconds(6)
                            .setPolyline("route0")
                        )
                )
                .addLegDefinition(
                    LegDefinition.newBuilder()
                        .setFromStepId("step-0")
                        .setFromTripId("trip-0")
                        .setToStepId("step-0")
                        .setToTripId("trip-1")
                        .setRouteLeg(RouteLeg.newBuilder()
                            .setDistanceInMeters(200)
                            .setTravelTimeInSeconds(7)
                            .setPolyline("route1")
                        )
                )
                .addLegDefinition(
                    LegDefinition.newBuilder()
                        .setFromStepId("step-0")
                        .setFromTripId("trip-1")
                        .setToStepId("step-1")
                        .setToTripId("trip-0")
                        .setRouteLeg(RouteLeg.newBuilder()
                            .setDistanceInMeters(300)
                            .setTravelTimeInSeconds(8)
                            .setPolyline("route2")
                        )
                )
            )
            .build();

        Mockito.doAnswer(invocation -> {
            final StreamObserver<UpdateVehicleStateResponse> observer =
                (StreamObserver<UpdateVehicleStateResponse>) invocation.getArguments()[1];
            observer.onNext(UpdateVehicleStateResponse.getDefaultInstance());
            observer.onCompleted();
            return null;
        }).when(mockBase).updateVehicleState(Mockito.any(), Mockito.any());

        interactorUnderTest.updateVehicleRoute(VEHICLE_ID, legsToSync).blockingAwait();

        Mockito.verify(mockBase).updateVehicleState(Mockito.eq(expectedRequest), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    private void mockStateResponse(final GetVehicleStateResponse response) {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetVehicleStateResponse> observer =
                (StreamObserver<GetVehicleStateResponse>) invocation.getArguments()[1];
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(mockBase).getVehicleState(Mockito.any(), Mockito.any());
    }
}
