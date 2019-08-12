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

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.model.ContactInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.PickupDropoff;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.RiderInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.Stop;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripDefinition;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.TripInfo;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.VehicleFilter;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.DispatchParameters;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetActiveTripIdResponse;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetActiveTripIdResponse.ActiveTrip;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetActiveTripIdResponse.NoActiveTrip;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.RequestTripRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.RequestTripResponse;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class DefaultRiderTripInteractorTest {
    private static final String API_TOKEN = "token";
    private static final String PASSENGER_ID = "passenger-1";
    private static final ContactInfo CONTACT_INFO = new ContactInfo("Passenger 1");
    private static final String FLEET_ID = "fleet-1";
    private static final String TRIP_ID = "task-1";
    private static final LatLng ORIGIN = new LatLng(0, 0);
    private static final LatLng DESTINATION = new LatLng(1, 1);
    private static final String VEHICLE_ID = "vehicle-1";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final RideHailRiderServiceGrpc.RideHailRiderServiceImplBase mockBase = Mockito.mock(
        RideHailRiderServiceGrpc.RideHailRiderServiceImplBase.class
    );
    private DefaultRiderTripInteractor interactorUnderTest;

    @SuppressWarnings("unchecked")
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

        Mockito.doAnswer(invocation -> {
            final StreamObserver<RequestTripResponse> observer =
                (StreamObserver<RequestTripResponse>) invocation.getArguments()[1];
            observer.onNext(RequestTripResponse.getDefaultInstance());
            observer.onCompleted();
            return null;
        }).when(mockBase).requestTrip(Mockito.any(), Mockito.any());

        interactorUnderTest = new DefaultRiderTripInteractor(
            () -> channel,
            user,
            () -> TRIP_ID,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testCanCreateTaskForPassenger() {
        final RequestTripRequest expectedRequest = RequestTripRequest.newBuilder()
            .setId(TRIP_ID)
            .setFleetId(FLEET_ID)
            .setRiderId(PASSENGER_ID)
            .setDefinition(
                TripDefinition.newBuilder()
                    .setPickupDropoff(
                        PickupDropoff.newBuilder()
                            .setPickup(Stop.newBuilder().setPosition(Locations.toRideOsPosition(ORIGIN)))
                            .setDropoff(Stop.newBuilder().setPosition(Locations.toRideOsPosition(DESTINATION)))
                            .setRiderCount(1)
                    )
            )
            .setInfo(
                TripInfo.newBuilder().setRiderInfo(
                    RiderInfo.newBuilder().setContactInfo(
                        RideHailCommons.ContactInfo.newBuilder()
                            .setName(CONTACT_INFO.getName())
                    )
                )
            )
            .setDispatchParameters(DispatchParameters.getDefaultInstance())
            .build();

        final TestObserver<String> testObserver = interactorUnderTest.createTripForPassenger(
            PASSENGER_ID,
            CONTACT_INFO,
            FLEET_ID,
            1,
            new TaskLocation(ORIGIN),
            new TaskLocation(DESTINATION)
        ).test();

        final ArgumentCaptor<RequestTripRequest> callbackCaptor = ArgumentCaptor.forClass(RequestTripRequest.class);
        Mockito.verify(mockBase).requestTrip(callbackCaptor.capture(), any());

        final RequestTripRequest actualRequest = callbackCaptor.getValue();
        assertEquals(expectedRequest, actualRequest);
    }

    @Test
    public void testCanCreateTaskForPassengerAndVehicle() {
        final RequestTripRequest expectedRequest = RequestTripRequest.newBuilder()
            .setId(TRIP_ID)
            .setFleetId(FLEET_ID)
            .setRiderId(PASSENGER_ID)
            .setDefinition(
                TripDefinition.newBuilder()
                    .setPickupDropoff(
                        PickupDropoff.newBuilder()
                            .setPickup(Stop.newBuilder().setPosition(Locations.toRideOsPosition(ORIGIN)))
                            .setDropoff(Stop.newBuilder().setPosition(Locations.toRideOsPosition(DESTINATION)))
                            .setRiderCount(1)
                    )
            )
            .setInfo(
                TripInfo.newBuilder().setRiderInfo(
                    RiderInfo.newBuilder().setContactInfo(
                        RideHailCommons.ContactInfo.newBuilder()
                            .setName(CONTACT_INFO.getName())
                    )
                )
            )
            .setDispatchParameters(
                DispatchParameters.newBuilder()
                    .addVehicleFilter(VehicleFilter.newBuilder().setVehicleId(VEHICLE_ID))
            )
            .build();

        final TestObserver<String> testObserver = interactorUnderTest.createTripForPassengerAndVehicle(
            PASSENGER_ID,
            CONTACT_INFO,
            VEHICLE_ID,
            FLEET_ID,
            1,
            new TaskLocation(ORIGIN),
            new TaskLocation(DESTINATION)
        ).test();

        final ArgumentCaptor<RequestTripRequest> callbackCaptor = ArgumentCaptor.forClass(RequestTripRequest.class);
        Mockito.verify(mockBase).requestTrip(callbackCaptor.capture(), any());

        final RequestTripRequest actualRequest = callbackCaptor.getValue();
        assertEquals(expectedRequest, actualRequest);
    }

    @Test
    public void testGetCurrentTripForPassengerWhenTripIsActive() {
        final GetActiveTripIdResponse tripResponse = GetActiveTripIdResponse.newBuilder()
            .setActiveTrip(ActiveTrip.newBuilder().setId(TRIP_ID))
            .build();
        setActiveTripResponse(tripResponse);

        interactorUnderTest.getCurrentTripForPassenger(PASSENGER_ID).test()
            .assertValueCount(1)
            .assertValueAt(0, maybeId -> maybeId.isPresent() && maybeId.get().equals(TRIP_ID));
    }

    @Test
    public void testNoCurrentTripReturnedWhenNoTripsActive() {
        final GetActiveTripIdResponse tripResponse = GetActiveTripIdResponse.newBuilder()
            .setNoActiveTrip(NoActiveTrip.getDefaultInstance())
            .build();
        setActiveTripResponse(tripResponse);

        interactorUnderTest.getCurrentTripForPassenger(PASSENGER_ID).test()
            .assertValueCount(1)
            .assertValueAt(0, maybeId -> !maybeId.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNoCurrentTripReturnedWhenCallFails() {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetActiveTripIdResponse> observer =
                (StreamObserver<GetActiveTripIdResponse>) invocation.getArguments()[1];
            observer.onError(new IOException());
            return null;
        }).when(mockBase).getActiveTripId(Mockito.any(), Mockito.any());

        interactorUnderTest.getCurrentTripForPassenger(PASSENGER_ID).test()
            .assertValueCount(1)
            .assertValueAt(0, maybeId -> !maybeId.isPresent());
    }

    @SuppressWarnings("unchecked")
    private void setActiveTripResponse(final GetActiveTripIdResponse response) {
        Mockito.doAnswer(invocation -> {
            final StreamObserver<GetActiveTripIdResponse> observer =
                (StreamObserver<GetActiveTripIdResponse>) invocation.getArguments()[1];
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(mockBase).getActiveTripId(Mockito.any(), Mockito.any());
    }
}
