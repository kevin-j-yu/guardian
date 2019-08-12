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
package ai.rideos.android.rider_app.start_screen;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.VehiclePosition;
import ai.rideos.android.common.model.map.CameraUpdate.UpdateType;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.reactive.TestUtils;
import ai.rideos.android.interactors.PreviewVehicleInteractor;
import ai.rideos.android.rider_app.R;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultStartScreenViewModelTest {

    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(
        new LatLng(0, 0),
        0.0f
    );
    private static final LocationAndHeading VEHICLE_LOCATION = new LocationAndHeading(
        new LatLng(0, 1),
        0.0f
    );
    private static final String VEHICLE_ID = "vehicle-1";
    private static final String FLEET_ID = "fleet-1";
    private static final int POLLING_INTERVAL_MILLI = 100;
    private static final int RETRY_COUNT = 1;

    private PreviewVehicleInteractor mockVehicleInteractor;
    private StartScreenListener listener;

    @Before
    public void setUp() {
        mockVehicleInteractor = Mockito.mock(PreviewVehicleInteractor.class);
        listener = Mockito.mock(StartScreenListener.class);

        Mockito.when(mockVehicleInteractor.getVehiclesInVicinity(CURRENT_LOCATION.getLatLng(), FLEET_ID))
            .thenReturn(Observable.just(Collections.singletonList(
                new VehiclePosition(VEHICLE_ID, VEHICLE_LOCATION.getLatLng(), VEHICLE_LOCATION.getHeading())
            )));
    }

    @Test
    public void testGetStaticMapSettings() {
        final DefaultStartScreenViewModel viewModelUnderTest = createViewModelForScheduler(
            new SchedulerProviders.TrampolineSchedulerProvider()
        );
        viewModelUnderTest.getMapSettings().test()
            .assertValueAt(0, new MapSettings(true, CenterPin.hidden()));
    }

    @Test
    public void testGetCameraUpdatesAfterCurrentLocationReported() {
        final DefaultStartScreenViewModel viewModelUnderTest = createViewModelForScheduler(
            new SchedulerProviders.TrampolineSchedulerProvider()
        );

        viewModelUnderTest.getCameraUpdates().test()
            .assertValueAt(0, update -> update.getUpdateType() == UpdateType.CENTER_AND_ZOOM
                && update.getNewCenter().equals(CURRENT_LOCATION.getLatLng())
            );
    }

    @Test
    public void testGetMarkersWaitsForMapCenter() {
        final TestScheduler testScheduler = new TestScheduler();
        final DefaultStartScreenViewModel viewModelUnderTest = createViewModelForScheduler(
            new SchedulerProviders.TestSchedulerProvider(testScheduler)
        );

        final TestObserver<Map<String, DrawableMarker>> markerObserver = viewModelUnderTest.getMarkers().test();

        markerObserver.assertEmpty();
        testScheduler.advanceTimeBy(POLLING_INTERVAL_MILLI, TimeUnit.MILLISECONDS);
        markerObserver.assertEmpty();
    }

    @Test
    public void testGetMarkersWithMapCenter() {
        final TestScheduler testScheduler = new TestScheduler();
        final DefaultStartScreenViewModel viewModelUnderTest = createViewModelForScheduler(
            new SchedulerProviders.TestSchedulerProvider(testScheduler)
        );

        final TestObserver<Map<String, DrawableMarker>> markerObserver = viewModelUnderTest.getMarkers().test();

        viewModelUnderTest.setCurrentMapCenter(CURRENT_LOCATION.getLatLng());
        testScheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS); // wait for initial delay of 0

        markerObserver.assertValueCount(1);
        final Map<String, DrawableMarker> expectedMarkers = new HashMap<>();
        expectedMarkers.put(
            VEHICLE_ID,
            new DrawableMarker(VEHICLE_LOCATION.getLatLng(), VEHICLE_LOCATION.getHeading(), R.mipmap.car, Anchor.CENTER)
        );
        markerObserver.assertValueAt(0, expectedMarkers);
    }

    @Test
    public void testGetMarkersAfterRetries() {
        final TestScheduler testScheduler = new TestScheduler();
        final DefaultStartScreenViewModel viewModelUnderTest = createViewModelForScheduler(
            new SchedulerProviders.TestSchedulerProvider(testScheduler)
        );
        final List<VehiclePosition> vehiclePositions = Collections.singletonList(
            new VehiclePosition(VEHICLE_ID, VEHICLE_LOCATION.getLatLng(), VEHICLE_LOCATION.getHeading())
        );

        Mockito.when(mockVehicleInteractor.getVehiclesInVicinity(CURRENT_LOCATION.getLatLng(), FLEET_ID))
            .thenReturn(TestUtils.throwUntilLastRetryObservable(RETRY_COUNT, vehiclePositions, new IOException()));

        final TestObserver<Map<String, DrawableMarker>> markerObserver = viewModelUnderTest.getMarkers().test();

        viewModelUnderTest.setCurrentMapCenter(CURRENT_LOCATION.getLatLng());
        testScheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS); // wait for initial delay of 0
        markerObserver.assertEmpty();

        testScheduler.advanceTimeBy(POLLING_INTERVAL_MILLI, TimeUnit.MILLISECONDS);
        markerObserver.assertValueCount(1);
    }

    @Test
    public void testSearchDestinationClickedStartsPreTripFlow() {
        final DefaultStartScreenViewModel viewModelUnderTest = createViewModelForScheduler(
            new TrampolineSchedulerProvider()
        );
        viewModelUnderTest.startDestinationSearch();
        Mockito.verify(listener).startPreTripFlow();
    }

    private DefaultStartScreenViewModel createViewModelForScheduler(final SchedulerProvider schedulerProvider) {
        final DeviceLocator deviceLocation = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocation.observeCurrentLocation(Mockito.anyInt()))
            .thenAnswer(i -> Observable.just(CURRENT_LOCATION));
        return new DefaultStartScreenViewModel(
            listener,
            deviceLocation,
            mockVehicleInteractor,
            Observable.just(new FleetInfo(FLEET_ID)),
            schedulerProvider,
            POLLING_INTERVAL_MILLI
        );
    }

}
