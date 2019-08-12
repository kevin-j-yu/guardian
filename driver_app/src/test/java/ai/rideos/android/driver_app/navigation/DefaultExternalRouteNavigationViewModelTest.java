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
package ai.rideos.android.driver_app.navigation;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import com.goebl.simplify.Simplify;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultExternalRouteNavigationViewModelTest {
    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(new LatLng(0, 1), 0);
    private static final LocationAndHeading DESTINATION = new LocationAndHeading(new LatLng(0, 2), 0);
    private static final List<LatLng> MOCK_ROUTE = Arrays.asList(CURRENT_LOCATION.getLatLng(), DESTINATION.getLatLng());
    private static final List<LatLng> SIMPLE_ROUTE = Arrays.asList(DESTINATION.getLatLng());
    private static final int MAX_COORDINATES = 4;
    private static final float TOLERANCE = 1.0f;

    private DefaultExternalRouteNavigationViewModel viewModelUnderTest;
    private DeviceLocator deviceLocator;
    private RouteInteractor routeInteractor;
    private Simplify<LatLng> simplifier;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.getLastKnownLocation()).thenReturn(
            Single.just(CURRENT_LOCATION)
        );

        routeInteractor = Mockito.mock(RouteInteractor.class);
        simplifier = Mockito.mock(Simplify.class);
        Mockito.when(simplifier.simplify(MOCK_ROUTE.toArray(new LatLng[0]), TOLERANCE, false))
            .thenReturn(SIMPLE_ROUTE.toArray(new LatLng[0]));

        viewModelUnderTest = new DefaultExternalRouteNavigationViewModel(
            routeInteractor,
            deviceLocator,
            MAX_COORDINATES,
            TOLERANCE,
            simplifier,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testSetDestinationTriggersRouteRequest() {
        Mockito.when(routeInteractor.getRoute(CURRENT_LOCATION, DESTINATION))
            .thenReturn(Observable.just(new RouteInfoModel(MOCK_ROUTE, 0, 0)));

        viewModelUnderTest.setDestination(DESTINATION.getLatLng());

        viewModelUnderTest.getRoute().test()
            .assertValueCount(1)
            .assertValueAt(0, result -> result.get().equals(MOCK_ROUTE));
    }

    @Test
    public void testSetDestinationSimplifiesRouteWhenTooLong() {
        final List<LatLng> longRoute = Arrays.asList(
            CURRENT_LOCATION.getLatLng(),
            new LatLng(4, 4),
            new LatLng(5, 5),
            new LatLng(6, 6),
            DESTINATION.getLatLng()
        );
        Mockito.when(routeInteractor.getRoute(CURRENT_LOCATION, DESTINATION))
            .thenReturn(Observable.just(new RouteInfoModel(longRoute, 0, 0)));
        Mockito.when(simplifier.simplify(longRoute.toArray(new LatLng[0]), TOLERANCE, false))
            .thenReturn(SIMPLE_ROUTE.toArray(new LatLng[0]));

        viewModelUnderTest.setDestination(DESTINATION.getLatLng());

        viewModelUnderTest.getRoute().test()
            .assertValueCount(1)
            .assertValueAt(0, result -> result.get().equals(SIMPLE_ROUTE));
    }

    @Test
    public void testDidGoOffRouteTriggersRouteRequest() {
        final LocationAndHeading navLocation = new LocationAndHeading(new LatLng(4, 4), 10);
        final List<LatLng> reRoute = Arrays.asList(
            navLocation.getLatLng(),
            new LatLng(0, 3),
            DESTINATION.getLatLng()
        );
        Mockito.when(routeInteractor.getRoute(CURRENT_LOCATION, DESTINATION))
            .thenReturn(Observable.just(new RouteInfoModel(MOCK_ROUTE, 0, 0)));

        Mockito.when(routeInteractor.getRoute(navLocation, DESTINATION))
            .thenReturn(Observable.just(new RouteInfoModel(reRoute, 0, 0)));

        final TestObserver<Result<List<LatLng>>> testObserver = viewModelUnderTest.getRoute().test();

        viewModelUnderTest.setDestination(DESTINATION.getLatLng());
        viewModelUnderTest.didGoOffRoute(navLocation);
        testObserver.assertValueCount(2)
            .assertValueAt(0, result -> result.get().equals(MOCK_ROUTE))
            .assertValueAt(1, result -> result.get().equals(reRoute));
    }

    @Test
    public void testDidGoOffRouteDoesNotTriggerRouteUntilDestinationSet() {
        viewModelUnderTest.didGoOffRoute(CURRENT_LOCATION);
        viewModelUnderTest.getRoute().test().assertEmpty();
    }
}
