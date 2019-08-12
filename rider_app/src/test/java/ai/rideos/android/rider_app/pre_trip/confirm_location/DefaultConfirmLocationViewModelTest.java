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

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.location.DistanceCalculator;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProviders.TestSchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.reactive.TestUtils;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationViewModel.ReverseGeocodingStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultConfirmLocationViewModelTest {
    private static final int DRAWABLE_PIN = 1;
    private static final LatLng INITIAL_LAT_LNG = new LatLng(0, 1);
    private static final NamedTaskLocation INITIAL_LOCATION = new NamedTaskLocation("initial", INITIAL_LAT_LNG);
    private static final LatLng NEW_LAT_LNG = new LatLng(2, 3);
    private static final NamedTaskLocation GEOCODED_LOCATION = new NamedTaskLocation("place", NEW_LAT_LNG);
    private static final String CURRENT_LOCATION_STRING = "Current Location";

    private static final int RETRY_COUNT = 1;

    private static final DistanceCalculator mockDistanceCalculator = (origin, destination) -> {
        if (origin.equals(destination)) {
            return 0;
        }
        return 1000000;
    };

    private TestScheduler testScheduler;
    private GeocodeInteractor geocodeInteractor;
    private ConfirmLocationListener listener;
    private ResourceProvider resourceProvider;
    private DefaultConfirmLocationViewModel viewModelUnderTest;

    @Before
    public void setUp() {
        testScheduler = new TestScheduler();
        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        listener = Mockito.mock(ConfirmLocationListener.class);
        resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getString(R.string.current_location_search_option))
            .thenReturn(CURRENT_LOCATION_STRING);
        viewModelUnderTest = new DefaultConfirmLocationViewModel(
            listener::confirmPickup,
            Mockito.mock(DeviceLocator.class),
            resourceProvider,
            INITIAL_LOCATION,
            DRAWABLE_PIN,
            geocodeInteractor,
            new TestSchedulerProvider(testScheduler),
            RetryBehaviors.retryAtMost(RETRY_COUNT),
            mockDistanceCalculator
        );

        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(Mockito.eq(NEW_LAT_LNG)))
            .thenReturn(Observable.just(Result.success(GEOCODED_LOCATION)));
    }

    @Test
    public void testCameraUpdates() {
        viewModelUnderTest = new DefaultConfirmLocationViewModel(
            listener::confirmPickup,
            Mockito.mock(DeviceLocator.class),
            resourceProvider,
            INITIAL_LOCATION,
            DRAWABLE_PIN,
            geocodeInteractor,
            new TrampolineSchedulerProvider(),
            RetryBehaviors.retryAtMost(RETRY_COUNT),
            mockDistanceCalculator
        );
        TestObserver<CameraUpdate> cameraObserver = viewModelUnderTest
            .getCameraUpdates()
            .test();

        cameraObserver.assertValueCount(1);
        cameraObserver.assertValueAt(0, cameraUpdate -> cameraUpdate.getNewCenter().equals(INITIAL_LAT_LNG));
    }

    @Test
    public void testOtherMapState() {
        viewModelUnderTest.getMarkers().test().assertValueAt(0, Collections.emptyMap());
        viewModelUnderTest.getPaths().test().assertValueAt(0, Collections.emptyList());
        viewModelUnderTest.getMapSettings().test().assertValueAt(0, settings ->
            settings.shouldShowUserLocation() && settings.getCenterPin().getDrawablePin() > 0
        );
    }

    @Test
    public void testReverseGeocodedLocationsStartWithInitialLocation() {
        TestObserver<String> reverseGeoObserver = viewModelUnderTest
            .getReverseGeocodedLocation()
            .test();

        // Single known result
        testScheduler.triggerActions();
        reverseGeoObserver
            .assertValueCount(1)
            .assertValueAt(0, INITIAL_LOCATION.getDisplayName());
        Mockito.verify(geocodeInteractor, Mockito.never()).getBestReverseGeocodeResult(Mockito.any());
    }

    @Test
    public void testReverseGeocodedLocationsUpdateWhenLocationUpdates() {
        TestObserver<String> reverseGeoObserver = viewModelUnderTest
            .getReverseGeocodedLocation()
            .test();

        // Single known result
        viewModelUnderTest.onCameraMoved(NEW_LAT_LNG);
        testScheduler.triggerActions();
        reverseGeoObserver
            .assertValueCount(2)
            .assertValueAt(1, GEOCODED_LOCATION.getDisplayName());
    }

    @Test
    public void testReverseGeocodeWhenInitialLocationIsTheCurrentLocation() {
        viewModelUnderTest = new DefaultConfirmLocationViewModel(
            listener::confirmPickup,
            Mockito.mock(DeviceLocator.class),
            resourceProvider,
            new NamedTaskLocation(CURRENT_LOCATION_STRING, NEW_LAT_LNG),
            DRAWABLE_PIN,
            geocodeInteractor,
            new TrampolineSchedulerProvider(),
            RetryBehaviors.retryAtMost(RETRY_COUNT),
            mockDistanceCalculator
        );

        TestObserver<String> reverseGeoObserver = viewModelUnderTest
            .getReverseGeocodedLocation()
            .test();

        // Single known result
        testScheduler.triggerActions();
        reverseGeoObserver
            .assertValueCount(1)
            .assertValueAt(0, GEOCODED_LOCATION.getDisplayName());
    }

    @Test
    public void getUnknownLocationOnReverseGeocodeFailure() {
        TestObserver<String> reverseGeoObserver = viewModelUnderTest
            .getReverseGeocodedLocation()
            .test();

        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(Mockito.eq(NEW_LAT_LNG)))
            .thenReturn(Observable.error(new IOException("oops")));

        viewModelUnderTest.onCameraMoved(NEW_LAT_LNG);
        testScheduler.triggerActions();
        reverseGeoObserver
            .assertValueCount(2)
            .assertValueAt(1, location -> !location.isEmpty());
    }

    @Test
    public void getCorrectLocationOnRetryOfReverseGeocode() {
        TestObserver<String> reverseGeoObserver = viewModelUnderTest
            .getReverseGeocodedLocation()
            .test();

        final Result<NamedTaskLocation> success = Result.success(GEOCODED_LOCATION);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(Mockito.eq(NEW_LAT_LNG)))
            .thenReturn(TestUtils.throwUntilLastRetryObservable(RETRY_COUNT, success, new IOException("oops")));

        viewModelUnderTest.onCameraMoved(NEW_LAT_LNG);
        testScheduler.triggerActions();
        reverseGeoObserver
            .assertValueCount(2)
            .assertValueAt(1, GEOCODED_LOCATION.getDisplayName());
    }

    @Test
    public void testConfirmButtonClickedSendsSelectedLocation() {
        viewModelUnderTest = new DefaultConfirmLocationViewModel(
            listener::confirmPickup,
            Mockito.mock(DeviceLocator.class),
            resourceProvider,
            INITIAL_LOCATION,
            DRAWABLE_PIN,
            geocodeInteractor,
            new TrampolineSchedulerProvider(), // use this scheduler to not deal with delays
            RetryBehaviors.retryAtMost(RETRY_COUNT),
            mockDistanceCalculator
        );

        viewModelUnderTest.confirmLocation();

        Mockito.verify(listener).confirmPickup(Mockito.any());
    }

    @Test
    public void testDefaultInitialPositionToCurrentLocationIfNoneGiven() {
        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.getLastKnownLocation())
            .thenReturn(Single.just(new LocationAndHeading(INITIAL_LAT_LNG, 0)));
        viewModelUnderTest = new DefaultConfirmLocationViewModel(
            listener::confirmPickup,
            deviceLocator,
            resourceProvider,
            null,
            DRAWABLE_PIN,
            geocodeInteractor,
            new TrampolineSchedulerProvider(), // use this scheduler to not deal with delays
            RetryBehaviors.retryAtMost(RETRY_COUNT),
            mockDistanceCalculator
        );

        viewModelUnderTest.getCameraUpdates().test()
            .assertValueAt(0, cameraUpdate -> cameraUpdate.getNewCenter().equals(INITIAL_LAT_LNG));
    }

    @Test
    public void testStatusUpdatesToNotInProgressAsReverseGeocodeRequestResolves() {
        // Pretend it takes a long time to get the result
        final int geocodeDelayMs = 500;
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(Mockito.eq(NEW_LAT_LNG)))
            .thenReturn(Observable
                .just(Result.success(GEOCODED_LOCATION))
                .delay(geocodeDelayMs, TimeUnit.MILLISECONDS, testScheduler)
            );
        final TestObserver<ReverseGeocodingStatus> statusObserver = viewModelUnderTest.getReverseGeocodingStatus().test();

        viewModelUnderTest.onCameraMoved(NEW_LAT_LNG);
        testScheduler.triggerActions();
        statusObserver.assertValueCount(3);
        statusObserver.assertValueAt(2, ReverseGeocodingStatus.IN_PROGRESS);

        testScheduler.advanceTimeBy(geocodeDelayMs, TimeUnit.MILLISECONDS);
        statusObserver.assertValueAt(3, ReverseGeocodingStatus.IDLE);
    }

    @Test
    public void testStatusUpdatesToErrorWhenReverseGeocodeFails() {
        // Pretend it takes a long time to get the result
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(Mockito.eq(NEW_LAT_LNG)))
            .thenReturn(Observable.error(new IOException()));
        final TestObserver<ReverseGeocodingStatus> statusObserver = viewModelUnderTest.getReverseGeocodingStatus().test();

        viewModelUnderTest.onCameraMoved(NEW_LAT_LNG);
        testScheduler.triggerActions();
        statusObserver.assertValueCount(4)
            .assertValueAt(2, ReverseGeocodingStatus.IN_PROGRESS)
            .assertValueAt(3, ReverseGeocodingStatus.ERROR);
    }

    @Test
    public void testReverseGeocodingStopsWhenRequestsQueue() {
        // Pretend it takes a long time to get the result
        final int geocodeDelayMs = 500;
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(Mockito.any()))
            .thenReturn(Observable
                .just(Result.success(GEOCODED_LOCATION))
                .delay(geocodeDelayMs, TimeUnit.MILLISECONDS, testScheduler)
            );
        final TestObserver<String> geocodeObserver = viewModelUnderTest.getReverseGeocodedLocation().test();

        final LatLng location1 = new LatLng(3, 3);
        final LatLng location2 = new LatLng(4, 4);

        viewModelUnderTest.onCameraMoved(NEW_LAT_LNG);
        testScheduler.triggerActions();
        geocodeObserver.assertValueCount(1);

        viewModelUnderTest.onCameraMoved(location1);
        viewModelUnderTest.onCameraMoved(location2);
        testScheduler.triggerActions();
        geocodeObserver.assertValueCount(1);

        testScheduler.advanceTimeBy(geocodeDelayMs, TimeUnit.MILLISECONDS);
        geocodeObserver.assertValueCount(2);
        testScheduler.advanceTimeBy(geocodeDelayMs, TimeUnit.MILLISECONDS);
        geocodeObserver.assertValueCount(3);
        Mockito.verify(geocodeInteractor).getBestReverseGeocodeResult(NEW_LAT_LNG);
        Mockito.verify(geocodeInteractor).getBestReverseGeocodeResult(location2);
        Mockito.verify(geocodeInteractor, Mockito.never()).getBestReverseGeocodeResult(location1);
    }
}
