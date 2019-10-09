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
package ai.rideos.android.rider_app.on_trip.follow_trip;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.location.Distance;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.common.model.VehicleInfo.ContactInfo;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TestSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.interactors.RiderTripStateInteractor;
import ai.rideos.android.model.FollowTripDisplayState;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.model.TripStateModel.Stage;
import ai.rideos.android.rider_app.on_trip.current_trip.CurrentTripListener;
import ai.rideos.android.rider_app.on_trip.current_trip.CurrentTripViewModel;
import ai.rideos.android.rider_app.on_trip.current_trip.DefaultCurrentTripViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultCurrentTripViewModelTest {
    private static final int POLL_INTERVAL_MILLI = 100;
    private static final String PASSENGER_ID = "passenger-1";
    private static final String TASK_ID = "task-1";
    private static final String FLEET_ID = "fleet-1";
    private static final long ROUTE_DURATION_MINUTES = 4;
    private static final long ROUTE_DURATION_MILLI = ROUTE_DURATION_MINUTES * 60 * 1000;
    private static final double ROUTE_DISTANCE_METERS = Distance.milesToMeters(1.0);
    private static final NamedTaskLocation PICKUP = new NamedTaskLocation("pickup", new LatLng(0, 0));
    private static final NamedTaskLocation DROP_OFF = new NamedTaskLocation("drop-off", new LatLng(1, 1));
    private static final LocationAndHeading VEHICLE_POSITION = new LocationAndHeading(
        new LatLng(2, 2),
        0.0f
    );
    private static final VehicleInfo VEHICLE_INFO = new VehicleInfo("123abc", new ContactInfo("", "", ""));

    private TestScheduler testScheduler;
    private CurrentTripViewModel viewModelUnderTest;
    private RiderTripStateInteractor tripStateInteractor;
    private GeocodeInteractor geocodeInteractor;
    private CurrentTripListener listener;

    @Before
    public void setUp() {
        testScheduler = new TestScheduler();
        tripStateInteractor = Mockito.mock(RiderTripStateInteractor.class);

        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(PICKUP.getLocation().getLatLng()))
            .thenReturn(Observable.just(Result.success(PICKUP)));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(DROP_OFF.getLocation().getLatLng()))
            .thenReturn(Observable.just(Result.success(DROP_OFF)));

        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(PASSENGER_ID);

        listener = Mockito.mock(CurrentTripListener.class);

        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getString(Mockito.anyInt())).thenReturn("");

        viewModelUnderTest = new DefaultCurrentTripViewModel(
            listener,
            tripStateInteractor,
            geocodeInteractor,
            user,
            Observable.just(new FleetInfo(FLEET_ID)),
            state -> {},
            new TestSchedulerProvider(testScheduler),
            POLL_INTERVAL_MILLI
        );
        viewModelUnderTest.initialize(TASK_ID);
    }

    @Test
    public void testInitialDisplayStateHasGeocodedLocations() {
        setCurrentStage(tripStateInteractor, Stage.WAITING_FOR_ASSIGNMENT);
        final TestObserver<FollowTripDisplayState> displayObserver = viewModelUnderTest.getDisplay().test();
        testScheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS);
        displayObserver
            .assertValueCount(1)
            .assertValueAt(0, displayState -> displayState.hasStageChanged()
                && displayState.getNamedPickupDropOff().getPickup().equals(PICKUP)
                && displayState.getNamedPickupDropOff().getDropOff().equals(DROP_OFF)
            );
    }

    @Test
    public void testHasStageChangeIsFalseWhenStageDoesNotUpdate() {
        setCurrentStage(tripStateInteractor, Stage.WAITING_FOR_ASSIGNMENT);
        final TestObserver<FollowTripDisplayState> displayObserver = viewModelUnderTest.getDisplay().test();
        testScheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS);
        displayObserver.assertValueCount(1);
        testScheduler.advanceTimeBy(POLL_INTERVAL_MILLI, TimeUnit.MILLISECONDS);
        displayObserver.assertValueCount(2)
            .assertValueAt(1, displayState -> !displayState.hasStageChanged());
    }

    @Test
    public void testGeocodedLocationsDoNotUpdateWhenLatLngIsSame() {
        setCurrentStage(tripStateInteractor, Stage.WAITING_FOR_ASSIGNMENT);
        final TestObserver<FollowTripDisplayState> displayObserver = viewModelUnderTest.getDisplay().test();
        testScheduler.advanceTimeBy(POLL_INTERVAL_MILLI, TimeUnit.MILLISECONDS);
        displayObserver.assertValueCount(2);
        Mockito.verify(geocodeInteractor, Mockito.times(1))
            .getBestReverseGeocodeResult(PICKUP.getLocation().getLatLng());
        Mockito.verify(geocodeInteractor, Mockito.times(1))
            .getBestReverseGeocodeResult(DROP_OFF.getLocation().getLatLng());
    }

    @Test
    public void testGeocodedLocationsUpdateWhenLatLngHasChanged() {
        final NamedTaskLocation changedPickup = new NamedTaskLocation("pickup2", new LatLng(4, 4));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(changedPickup.getLocation().getLatLng()))
            .thenReturn(Observable.just(Result.success(changedPickup)));

        setCurrentStage(tripStateInteractor, Stage.WAITING_FOR_ASSIGNMENT);
        final TestObserver<FollowTripDisplayState> displayObserver = viewModelUnderTest.getDisplay().test();
        testScheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS);

        // Change pickup location
        Mockito.when(tripStateInteractor.getTripState(TASK_ID, FLEET_ID))
            .thenReturn(Single.just(new TripStateModel(
                Stage.WAITING_FOR_ASSIGNMENT,
                new RouteInfoModel(
                    Arrays.asList(changedPickup.getLocation().getLatLng(), DROP_OFF.getLocation().getLatLng()),
                    ROUTE_DURATION_MILLI,
                    ROUTE_DISTANCE_METERS
                ),
                VEHICLE_INFO,
                VEHICLE_POSITION,
                changedPickup.getLocation().getLatLng(),
                DROP_OFF.getLocation().getLatLng(),
                Collections.emptyList(),
                null
            )));

        testScheduler.advanceTimeBy(POLL_INTERVAL_MILLI, TimeUnit.MILLISECONDS);
        displayObserver.assertValueCount(2);
        Mockito.verify(geocodeInteractor, Mockito.times(1))
            .getBestReverseGeocodeResult(PICKUP.getLocation().getLatLng());
        Mockito.verify(geocodeInteractor, Mockito.times(1))
            .getBestReverseGeocodeResult(changedPickup.getLocation().getLatLng());
        Mockito.verify(geocodeInteractor, Mockito.times(1))
            .getBestReverseGeocodeResult(DROP_OFF.getLocation().getLatLng());
    }

    private static void setCurrentStage(final RiderTripStateInteractor tripStateInteractor, final Stage stage) {
        Mockito.when(tripStateInteractor.getTripState(TASK_ID, FLEET_ID))
            .thenReturn(Single.just(new TripStateModel(
                stage,
                new RouteInfoModel(
                    Arrays.asList(PICKUP.getLocation().getLatLng(), DROP_OFF.getLocation().getLatLng()),
                    ROUTE_DURATION_MILLI,
                    ROUTE_DISTANCE_METERS
                ),
                VEHICLE_INFO,
                VEHICLE_POSITION,
                PICKUP.getLocation().getLatLng(),
                DROP_OFF.getLocation().getLatLng(),
                Collections.emptyList(),
                null
            )));
    }
}
