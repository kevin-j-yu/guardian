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
package ai.rideos.android.rider_app.pre_trip.confirm_location;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.model.map.CameraUpdate.UpdateType;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TestSchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.StopInteractor;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.Stop;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FixedLocationConfirmLocationViewModelTest {
    private static final String FLEET_ID = "fleet-1";
    private static final int CENTER_PIN = 1;
    private static final int FIXED_LOCATION_PIN = 2;
    private static final LatLng CURRENT_LOCATION = new LatLng(1, 1);
    private static final Stop PUDOL = new Stop(new LatLng(2, 2), "pudol-1");
    private static final TaskLocation PUDOL_LOCATION = new TaskLocation(PUDOL.getLatLng(), PUDOL.getId());
    private static final NamedTaskLocation NAMED_PUDOL = new NamedTaskLocation("pudol", PUDOL_LOCATION);
    private static final NamedTaskLocation NAMED_CURRENT_LOCATION = new NamedTaskLocation("current place", CURRENT_LOCATION);

    private FixedLocationConfirmLocationViewModel viewModelUnderTest;
    private StopInteractor pudolInteractor;
    private GeocodeInteractor geocodeInteractor;
    private ConfirmLocationListener listener;

    @Before
    public void setUp() {
        pudolInteractor = Mockito.mock(StopInteractor.class);
        Mockito.when(pudolInteractor.getBestStop(FLEET_ID, CURRENT_LOCATION))
            .thenReturn(Observable.just(PUDOL));

        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(PUDOL_LOCATION.getLatLng()))
            .thenReturn(Observable.just(Result.success(NAMED_PUDOL)));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(CURRENT_LOCATION))
            .thenReturn(Observable.just(Result.success(NAMED_CURRENT_LOCATION)));

        listener = Mockito.mock(ConfirmLocationListener.class);

        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getColor(Mockito.anyInt())).thenReturn(0);

        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);

        viewModelUnderTest = new FixedLocationConfirmLocationViewModel(
            listener::confirmPickup,
            pudolInteractor,
            geocodeInteractor,
            deviceLocator,
            Observable.just(new FleetInfo(FLEET_ID)),
            resourceProvider,
            CENTER_PIN,
            FIXED_LOCATION_PIN,
            CURRENT_LOCATION,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testInitialLocationUpdatesCamera() {
        viewModelUnderTest.getCameraUpdates().test()
            .assertValueCount(1)
            .assertValueAt(0, update -> update.getUpdateType() == UpdateType.CENTER_AND_ZOOM
                && update.getNewCenter().equals(CURRENT_LOCATION)
            );
    }

    @Test
    public void testInitialLocationTriggersPudolLookupAndGeocode() {
        viewModelUnderTest.getReverseGeocodedLocation().test()
            .assertValueCount(1)
            .assertValueAt(0, NAMED_PUDOL.getDisplayName());

        Mockito.verify(pudolInteractor).getBestStop(FLEET_ID, CURRENT_LOCATION);
    }

    @Test
    public void testCameraMovementUpdatesPudolLookupAndGeocode() {
        final LatLng newLocation = new LatLng(3, 3);
        final Stop newPudol = new Stop(new LatLng(4, 4), "another-pudol");
        final NamedTaskLocation geocodedPudol = new NamedTaskLocation(
            "place2",
            new TaskLocation(newPudol.getLatLng(), newPudol.getId())
        );

        Mockito.when(pudolInteractor.getBestStop(FLEET_ID, newLocation))
            .thenReturn(Observable.just(newPudol));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(newPudol.getLatLng()))
            .thenReturn(Observable.just(Result.success(geocodedPudol)));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(newLocation))
            .thenReturn(Observable.just(Result.success(NAMED_CURRENT_LOCATION)));

        final TestObserver<String> observer = viewModelUnderTest.getReverseGeocodedLocation().test();

        viewModelUnderTest.onCameraMoved(newLocation);
        observer.assertValueCount(2)
            .assertValueAt(1, geocodedPudol.getDisplayName());
    }

    @Test
    public void testGetMarkersReturnsClosestPudol() {
        viewModelUnderTest.getMarkers().test()
            .assertValueAt(0, markerMap -> markerMap.size() == 1
                // Check if only entry is for the closest PUDOL
                && markerMap.entrySet().iterator().next().getValue().getPosition().equals(PUDOL_LOCATION.getLatLng())
            );
    }

    @Test
    public void testGetPathsReturnsPathBetweenCurrentLocationAndClosestPudol() {
        viewModelUnderTest.getPaths().test()
            .assertValueAt(0, pathList -> pathList.size() == 1
                && pathList.get(0).getCoordinates().equals(Arrays.asList(CURRENT_LOCATION, PUDOL_LOCATION.getLatLng()))
            );
    }

    @Test
    public void testGetPathsReturnsNoPathsWhenCameraStartsMoving() {
        final TestObserver<List<DrawablePath>> testObserver = viewModelUnderTest.getPaths().test();
        viewModelUnderTest.onCameraStartedMoving();
        testObserver
            .assertValueCount(2)
            .assertValueAt(0, pathList -> pathList.size() == 1)
            .assertValueAt(1, List::isEmpty);
    }

    @Test
    public void testConfirmLocationUsesLatestGeocodedPudol() {
        viewModelUnderTest.confirmLocation();
        Mockito.verify(listener)
            .confirmPickup(new DesiredAndAssignedLocation(NAMED_CURRENT_LOCATION, NAMED_PUDOL));
    }

    @Test
    public void testStatusUpdatesToNotInProgressAsReverseGeocodeRequestResolves() {
        final TestScheduler testScheduler = new TestScheduler();
        final int delayMs = 500;
        pudolInteractor = Mockito.mock(StopInteractor.class);
        Mockito.when(pudolInteractor.getBestStop(FLEET_ID, CURRENT_LOCATION))
            .thenReturn(Observable.just(PUDOL).delay(delayMs, TimeUnit.MILLISECONDS, testScheduler));

        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(PUDOL_LOCATION.getLatLng()))
            .thenReturn(
                Observable.just(Result.success(NAMED_PUDOL))
                    .delay(delayMs, TimeUnit.MILLISECONDS, testScheduler)
            );
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(CURRENT_LOCATION))
            .thenReturn(Observable.just(Result.success(NAMED_CURRENT_LOCATION)));

        viewModelUnderTest = new FixedLocationConfirmLocationViewModel(
            Mockito.mock(ConfirmLocationListener.class)::confirmPickup,
            pudolInteractor,
            geocodeInteractor,
            Mockito.mock(DeviceLocator.class),
            Observable.just(new FleetInfo(FLEET_ID)),
            Mockito.mock(ResourceProvider.class),
            CENTER_PIN,
            FIXED_LOCATION_PIN,
            CURRENT_LOCATION,
            new TestSchedulerProvider(testScheduler)
        );

        final TestObserver<ProgressState> progressObserver = viewModelUnderTest.getReverseGeocodingProgress().test();
        testScheduler.triggerActions();
        progressObserver.assertValueCount(1).assertValueAt(0, ProgressState.LOADING);

        // trigger pudol lookup
        testScheduler.advanceTimeBy(delayMs, TimeUnit.MILLISECONDS);
        progressObserver.assertValueCount(1);

        // trigger geocode lookup
        testScheduler.advanceTimeBy(delayMs, TimeUnit.MILLISECONDS);
        progressObserver.assertValueCount(2).assertValueAt(1, ProgressState.IDLE);
    }

    @Test
    public void testStatusUpdatesToErrorWhenPudolLookupFails() {
        pudolInteractor = Mockito.mock(StopInteractor.class);
        Mockito.when(pudolInteractor.getBestStop(FLEET_ID, CURRENT_LOCATION))
            .thenReturn(Observable.error(new IOException()));

        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(PUDOL_LOCATION.getLatLng()))
            .thenReturn(Observable.just(Result.success(NAMED_PUDOL)));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(CURRENT_LOCATION))
            .thenReturn(Observable.just(Result.success(NAMED_CURRENT_LOCATION)));

        viewModelUnderTest = new FixedLocationConfirmLocationViewModel(
            Mockito.mock(ConfirmLocationListener.class)::confirmPickup,
            pudolInteractor,
            geocodeInteractor,
            Mockito.mock(DeviceLocator.class),
            Observable.just(new FleetInfo(FLEET_ID)),
            Mockito.mock(ResourceProvider.class),
            CENTER_PIN,
            FIXED_LOCATION_PIN,
            CURRENT_LOCATION,
            new TrampolineSchedulerProvider()
        );

        final TestObserver<ProgressState> progressObserver = viewModelUnderTest.getReverseGeocodingProgress().test();
        progressObserver.assertValueAt(0, ProgressState.FAILED);
    }

    @Test
    public void testLocationStopsUpdatingWhenRequestsTakingTooLong() {
        final TestScheduler testScheduler = new TestScheduler();
        final int delayMs = 500;
        pudolInteractor = Mockito.mock(StopInteractor.class);
        Mockito.when(pudolInteractor.getBestStop(eq(FLEET_ID), any()))
            .thenReturn(Observable.just(PUDOL).delay(delayMs, TimeUnit.MILLISECONDS, testScheduler));

        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(PUDOL_LOCATION.getLatLng()))
            .thenReturn(Observable.just(Result.success(NAMED_PUDOL)));
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(any()))
            .thenReturn(Observable.just(Result.success(NAMED_CURRENT_LOCATION)));

        viewModelUnderTest = new FixedLocationConfirmLocationViewModel(
            Mockito.mock(ConfirmLocationListener.class)::confirmPickup,
            pudolInteractor,
            geocodeInteractor,
            Mockito.mock(DeviceLocator.class),
            Observable.just(new FleetInfo(FLEET_ID)),
            Mockito.mock(ResourceProvider.class),
            CENTER_PIN,
            FIXED_LOCATION_PIN,
            CURRENT_LOCATION,
            new TestSchedulerProvider(testScheduler)
        );

        final LatLng location1 = new LatLng(3, 3);
        final LatLng location2 = new LatLng(4, 4);

        final TestObserver<String> locationObserver = viewModelUnderTest.getReverseGeocodedLocation().test();
        testScheduler.triggerActions();
        locationObserver.assertValueCount(0);

        viewModelUnderTest.onCameraMoved(location1); // This should be ignored because of backpressure
        viewModelUnderTest.onCameraMoved(location2);
        testScheduler.triggerActions();
        locationObserver.assertValueCount(0);

        testScheduler.advanceTimeBy(delayMs, TimeUnit.MILLISECONDS);
        locationObserver.assertValueCount(1);
        testScheduler.advanceTimeBy(delayMs, TimeUnit.MILLISECONDS);
        locationObserver.assertValueCount(2);
        Mockito.verify(pudolInteractor).getBestStop(FLEET_ID, CURRENT_LOCATION);
        Mockito.verify(pudolInteractor).getBestStop(FLEET_ID, location2);
        Mockito.verify(pudolInteractor, Mockito.never()).getBestStop(FLEET_ID, location1);
    }
}
