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
package ai.rideos.android.common.interactors;

import ai.rideos.android.common.interactors.RideOsRouteInteractor.RouteException;
import ai.rideos.android.common.interactors.mapbox.MapboxApiInteractor;
import ai.rideos.android.common.interactors.mapbox.MapboxRouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Polylines.PolylineDecoder;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MapboxRouteInteractorTest {
    private static final LatLng ORIGIN = new LatLng(0, 1);
    private static final LatLng DESTINATION = new LatLng(2, 3);
    private static final LatLng WAYPOINT = new LatLng(4, 5);
    private static final String GEOMETRY = "abcdefg";

    private MapboxRouteInteractor interactorUnderTest;
    private PolylineDecoder polyLineDecoder;
    private DirectionsRoute mapboxRoute;

    @Before
    public void setUp() {
        polyLineDecoder = Mockito.mock(PolylineDecoder.class);
        final MapboxApiInteractor apiInteractor = Mockito.mock(MapboxApiInteractor.class);

        mapboxRoute = Mockito.mock(DirectionsRoute.class);
        Mockito.when(apiInteractor.getDirectionsToDestination(
            Point.fromLngLat(ORIGIN.getLongitude(), ORIGIN.getLatitude()),
            Point.fromLngLat(DESTINATION.getLongitude(), DESTINATION.getLatitude())
        )).thenReturn(Observable.just(mapboxRoute));
        Mockito.when(apiInteractor.getDirectionsToDestination(
            Point.fromLngLat(ORIGIN.getLongitude(), ORIGIN.getLatitude()),
            Point.fromLngLat(DESTINATION.getLongitude(), DESTINATION.getLatitude()),
            Collections.singletonList(Point.fromLngLat(WAYPOINT.getLongitude(), WAYPOINT.getLatitude()))
        )).thenReturn(Observable.just(mapboxRoute));

        interactorUnderTest = new MapboxRouteInteractor(apiInteractor, polyLineDecoder, new TrampolineSchedulerProvider());
        Mockito.when(polyLineDecoder.decode(GEOMETRY)).thenReturn(Arrays.asList(ORIGIN, DESTINATION));
    }

    @Test
    public void testCanGetValidRouteFromMapboxApi() {
        Mockito.when(mapboxRoute.geometry()).thenReturn(GEOMETRY);
        Mockito.when(mapboxRoute.duration()).thenReturn(60d);
        Mockito.when(mapboxRoute.distance()).thenReturn(120d);

        interactorUnderTest.getRoute(ORIGIN, DESTINATION).test()
            .assertValueCount(1)
            .assertValueAt(0, new RouteInfoModel(
                Arrays.asList(ORIGIN, DESTINATION),
                60 * 1000,
                120
            ));
    }

    @Test
    public void testInvalidMapboxRouteEmitsRouteException() {
        Mockito.when(mapboxRoute.geometry()).thenReturn(null);
        interactorUnderTest.getRoute(ORIGIN, DESTINATION).test()
            .assertError(RouteException.class);
    }

    @Test
    public void testCanGetValidWaypointRouteFromMapboxApi() {
        final LatLng latLng0 = new LatLng(0.5, 1.5);
        final LatLng latLng1 = new LatLng(2.5, 3.5);
        final RouteInfoModel expectedFirstLeg = new RouteInfoModel(
            Arrays.asList(ORIGIN, latLng0, latLng0, WAYPOINT),
            60000,
            120
        );
        final RouteInfoModel expectedSecondLeg = new RouteInfoModel(
            Arrays.asList(WAYPOINT, latLng1, latLng1, DESTINATION),
            60000,
            120
        );

        final RouteLeg firstLeg = Mockito.mock(RouteLeg.class);
        Mockito.when(firstLeg.duration()).thenReturn((double) expectedFirstLeg.getTravelTimeMillis() / 1000);
        Mockito.when(firstLeg.distance()).thenReturn(expectedFirstLeg.getTravelDistanceMeters());

        final LegStep firstLegFirstStep = Mockito.mock(LegStep.class);
        Mockito.when(firstLegFirstStep.geometry()).thenReturn("firstLegFirstStep");
        Mockito.when(polyLineDecoder.decode("firstLegFirstStep"))
            .thenReturn(Arrays.asList(ORIGIN, latLng0));

        final LegStep firstLegSecondStep = Mockito.mock(LegStep.class);
        Mockito.when(firstLegSecondStep.geometry()).thenReturn("firstLegSecondStep");
        Mockito.when(polyLineDecoder.decode("firstLegSecondStep"))
            .thenReturn(Arrays.asList(latLng0, WAYPOINT));

        Mockito.when(firstLeg.steps()).thenReturn(Arrays.asList(firstLegFirstStep, firstLegSecondStep));

        final RouteLeg secondLeg = Mockito.mock(RouteLeg.class);
        Mockito.when(secondLeg.duration()).thenReturn((double) expectedSecondLeg.getTravelTimeMillis() / 1000);
        Mockito.when(secondLeg.distance()).thenReturn(expectedSecondLeg.getTravelDistanceMeters());

        final LegStep secondLegFirstStep = Mockito.mock(LegStep.class);
        Mockito.when(secondLegFirstStep.geometry()).thenReturn("secondLegFirstStep");
        Mockito.when(polyLineDecoder.decode("secondLegFirstStep"))
            .thenReturn(Arrays.asList(WAYPOINT, latLng1));

        final LegStep secondLegSecondStep = Mockito.mock(LegStep.class);
        Mockito.when(secondLegSecondStep.geometry()).thenReturn("secondLegSecondStep");
        Mockito.when(polyLineDecoder.decode("secondLegSecondStep"))
            .thenReturn(Arrays.asList(latLng1, DESTINATION));

        Mockito.when(secondLeg.steps()).thenReturn(Arrays.asList(secondLegFirstStep, secondLegSecondStep));

        Mockito.when(mapboxRoute.legs()).thenReturn(Arrays.asList(firstLeg, secondLeg));

        interactorUnderTest.getRouteForWaypoints(Arrays.asList(ORIGIN, WAYPOINT, DESTINATION)).test()
            .assertValueCount(1)
            .assertValueAt(0, Arrays.asList(expectedFirstLeg, expectedSecondLeg));
    }

    @Test
    public void testIncorrectLegCountEmitsRouteException() {
        Mockito.when(mapboxRoute.legs()).thenReturn(Collections.emptyList());

        interactorUnderTest.getRouteForWaypoints(Arrays.asList(ORIGIN, WAYPOINT, DESTINATION)).test()
            .assertError(RouteException.class);
    }

    @Test
    public void testInvalidLegEmitsRouteException() {
        final RouteLeg firstLeg = Mockito.mock(RouteLeg.class);
        Mockito.when(firstLeg.steps()).thenReturn(Collections.emptyList());
        Mockito.when(mapboxRoute.legs()).thenReturn(Collections.singletonList(firstLeg));
        interactorUnderTest.getRouteForWaypoints(Arrays.asList(ORIGIN, WAYPOINT, DESTINATION)).test()
            .assertError(RouteException.class);
    }
}
