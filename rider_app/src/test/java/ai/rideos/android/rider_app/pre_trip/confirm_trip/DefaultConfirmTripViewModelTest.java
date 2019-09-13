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
package ai.rideos.android.rider_app.pre_trip.confirm_trip;

import static junit.framework.TestCase.assertFalse;
import static org.mockito.Matchers.any;

import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.common.app.MetadataReader.MetadataResult;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.location.Distance;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProviders;
import ai.rideos.android.common.reactive.SchedulerProviders.TestSchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.reactive.TestUtils;
import ai.rideos.android.common.utils.DrawablePaths;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.view.strings.RouteFormatter;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.model.RouteTimeDistanceDisplay;
import ai.rideos.android.settings.RiderMetadataKeys;
import io.grpc.StatusRuntimeException;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultConfirmTripViewModelTest {
    private static final LatLng ORIGIN = new LatLng(0, 0);
    private static final LatLng DESTINATION = new LatLng(1, 1);
    private static final int ROUTE_COLOR = 1;
    private static final int RETRY_COUNT = 1;
    private static final long DURATION_MILLI = 4 * 60 * 1000;
    private static final double DURATION_METERS = Distance.milesToMeters(1.0);
    private static final String SUCCESSFUL_DISTANCE = "10 miles";
    private static final String SUCCESSFUL_TIME = "10 minutes";

    private RouteInteractor mockRouteInteractor;
    private DefaultConfirmTripViewModel viewModelUnderTest;
    private ConfirmTripListener listener;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockRouteInteractor = Mockito.mock(RouteInteractor.class);
        listener = Mockito.mock(ConfirmTripListener.class);
        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getColor(Mockito.anyInt())).thenReturn(ROUTE_COLOR);

        final RouteFormatter routeFormatter = Mockito.mock(RouteFormatter.class);
        Mockito.when(routeFormatter.getTravelDistanceDisplayString(any()))
            .thenReturn(SUCCESSFUL_DISTANCE);
        Mockito.when(routeFormatter.getTravelTimeDisplayString(any()))
            .thenReturn(SUCCESSFUL_TIME);

        final MetadataReader metadataReader = Mockito.mock(MetadataReader.class);
        final MetadataResult<Boolean> result = Mockito.mock(MetadataResult.class);
        Mockito.when(metadataReader.getBooleanMetadata(RiderMetadataKeys.DISABLE_SEAT_SELECTION_KEY))
            .thenReturn(result);
        Mockito.when(result.getOrDefault(Mockito.any())).thenReturn(false);

        viewModelUnderTest = new DefaultConfirmTripViewModel(
            listener,
            mockRouteInteractor,
            resourceProvider,
            metadataReader,
            new SchedulerProviders.TrampolineSchedulerProvider(),
            routeFormatter,
            RetryBehaviors.retryAtMost(RETRY_COUNT)
        );

        Mockito.when(mockRouteInteractor.getRoute(ORIGIN, DESTINATION))
            .thenReturn(Observable.just(
                new RouteInfoModel(
                    Arrays.asList(ORIGIN, DESTINATION),
                    DURATION_MILLI,
                    DURATION_METERS
                )
            ));
    }

    @Test
    public void testGetRouteInformation() {
        final TestObserver<RouteTimeDistanceDisplay> routeInfoObservable = viewModelUnderTest
            .getRouteInformation()
            .test();

        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        routeInfoObservable.assertValueCount(1)
            .assertValueAt(0, info -> info.getDistance().length() > 0 && info.getTime().length() > 0);
        Mockito.verify(mockRouteInteractor).getRoute(ORIGIN, DESTINATION);
    }

    @Test
    public void testGetStaticMapSettings() {
        viewModelUnderTest.getMapSettings().test()
            .assertValueAt(0, new MapSettings(false, CenterPin.hidden()));
    }

    @Test
    public void testGetCameraUpdates() {
        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        viewModelUnderTest.getCameraUpdates().test()
            .assertValueAt(0, CameraUpdate.fitToBounds(new LatLngBounds(ORIGIN, DESTINATION)));
    }

    @Test
    public void testGetMarkers() {
        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        viewModelUnderTest.getMarkers().test()
            .assertValueAt(0, markers -> markers.containsKey(Markers.PICKUP_MARKER_KEY)
                && markers.containsKey(Markers.DROP_OFF_MARKER_KEY)
            );
    }

    @Test
    public void getNoRouteInformationOnRouteFailure() {
        Mockito.when(mockRouteInteractor.getRoute(ORIGIN, DESTINATION))
            .thenReturn(Observable.error(new StatusRuntimeException(io.grpc.Status.INTERNAL)));

        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        viewModelUnderTest.getRouteInformation().test().assertValueCount(0);
    }

    @Test
    public void getCorrectRouteInformationAfterRetries() {
        final RouteInfoModel success = new RouteInfoModel(
            Arrays.asList(ORIGIN, DESTINATION),
            DURATION_MILLI,
            DURATION_METERS
        );
        Mockito.when(mockRouteInteractor.getRoute(ORIGIN, DESTINATION))
            .thenReturn(TestUtils.throwUntilLastRetryObservable(RETRY_COUNT, success, new IOException("oops")));

        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        viewModelUnderTest.getRouteInformation().test()
            .assertValueAt(
                0,
                timeAndDistance -> timeAndDistance.getTime().equals(SUCCESSFUL_TIME)
                    && timeAndDistance.getDistance().equals(SUCCESSFUL_DISTANCE)
            );
    }

    @Test
    public void testGetPaths() {
        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        final List<DrawablePath> expectedPaths = Collections.singletonList(new DrawablePath(
            Arrays.asList(ORIGIN, DESTINATION),
            DrawablePaths.DEFAULT_PATH_WIDTH,
            ROUTE_COLOR
        ));
        viewModelUnderTest.getPaths().test()
            .assertValueAt(0, expectedPaths);
    }

    @Test
    public void testConfirmButtonClickedCallsListener() {
        viewModelUnderTest.confirmTrip(1);
        Mockito.verify(listener).confirmTrip(1);
    }

    @Test
    public void testRouteStatusIsIdleWhenRouteIsResolved() {
        final RouteInteractor routeInteractor = Mockito.mock(RouteInteractor.class);
        final int routeDelayMs = 1000;
        final TestScheduler testScheduler = new TestScheduler();

        Mockito.when(routeInteractor.getRoute(ORIGIN, DESTINATION))
            .thenReturn(
                Observable.just(new RouteInfoModel(
                    Arrays.asList(ORIGIN, DESTINATION),
                    DURATION_MILLI,
                    DURATION_METERS
                ))
                    .delay(routeDelayMs, TimeUnit.MILLISECONDS, testScheduler)
            );

        viewModelUnderTest = new DefaultConfirmTripViewModel(
            Mockito.mock(ConfirmTripListener.class),
            routeInteractor,
            Mockito.mock(ResourceProvider.class),
            Mockito.mock(MetadataReader.class),
            new TestSchedulerProvider(testScheduler),
            Mockito.mock(RouteFormatter.class),
            RetryBehaviors.neverRetry()
        );

        final TestObserver<ProgressState> statusObserver = viewModelUnderTest.getFetchingRouteProgress().test();
        testScheduler.triggerActions();
        statusObserver.assertValueCount(1)
            .assertValueAt(0, ProgressState.LOADING);

        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        statusObserver.assertValueCount(1);

        testScheduler.advanceTimeBy(routeDelayMs, TimeUnit.MILLISECONDS);
        statusObserver.assertValueCount(2)
            .assertValueAt(1, ProgressState.SUCCEEDED);
    }

    @Test
    public void testRouteStatusIsErrorWhenRouteFails() {
        final RouteInteractor routeInteractor = Mockito.mock(RouteInteractor.class);

        Mockito.when(routeInteractor.getRoute(ORIGIN, DESTINATION))
            .thenReturn(Observable.error(new IOException()));

        viewModelUnderTest = new DefaultConfirmTripViewModel(
            Mockito.mock(ConfirmTripListener.class),
            routeInteractor,
            Mockito.mock(ResourceProvider.class),
            Mockito.mock(MetadataReader.class),
            new TrampolineSchedulerProvider(),
            Mockito.mock(RouteFormatter.class),
            RetryBehaviors.neverRetry()
        );

        viewModelUnderTest.setOriginAndDestination(ORIGIN, DESTINATION);
        viewModelUnderTest.getFetchingRouteProgress().test()
            .assertValueAt(0, ProgressState.FAILED);
    }

    @Test
    public void testIsSeatSelectDisabledReadsFromMetadata() {
        assertFalse(viewModelUnderTest.isSeatSelectionDisabled());
    }
}
