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
package ai.rideos.android.common.interactors.mapbox;

import static com.mapbox.core.constants.Constants.PRECISION_6;

import ai.rideos.android.common.interactors.RideOsRouteInteractor.RouteException;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Polylines.PolylineDecoder;
import android.content.Context;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import io.reactivex.Observable;
import java.util.List;
import java.util.stream.Collectors;

public class MapboxRouteInteractor implements RouteInteractor {
    private final MapboxApiInteractor apiInteractor;
    private final SchedulerProvider schedulerProvider;
    private final PolylineDecoder polyLineDecoder;

    public MapboxRouteInteractor(final Context context) {
        this(new MapboxApiInteractor(context), new MapboxPolylineDecoder(), new DefaultSchedulerProvider());
    }

    public MapboxRouteInteractor(final MapboxApiInteractor apiInteractor,
                                 final PolylineDecoder polyLineDecoder,
                                 final SchedulerProvider schedulerProvider) {
        this.apiInteractor = apiInteractor;
        this.polyLineDecoder = polyLineDecoder;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<RouteInfoModel> getRoute(final LatLng origin, final LatLng destination) {
        return apiInteractor.getDirectionsToDestination(
            Point.fromLngLat(origin.getLongitude(), origin.getLatitude()),
            Point.fromLngLat(destination.getLongitude(), destination.getLatitude())
        )
            .observeOn(schedulerProvider.computation())
            .map(directionsRoute -> {
                validateDirectionsRoute(directionsRoute);
                return new RouteInfoModel(
                    polyLineDecoder.decode(directionsRoute.geometry()),
                    (long) (directionsRoute.duration() * 1000),
                    directionsRoute.distance()
                );
            });
    }

    @Override
    public Observable<RouteInfoModel> getRoute(final LocationAndHeading origin, final LocationAndHeading destination) {
        return getRoute(origin.getLatLng(), destination.getLatLng());
    }

    @Override
    public Observable<List<RouteInfoModel>> getRouteForWaypoints(final List<LatLng> waypoints) {
        if (waypoints.size() < 2) {
            return Observable.error(new RouteException("There must be at least 2 waypoints to call this API"));
        }
        final LatLng origin = waypoints.get(0);
        final LatLng destination = waypoints.get(waypoints.size() - 1);
        final List<Point> intermediatePoints = waypoints.subList(1, waypoints.size() - 1).stream()
            .map(latLng -> Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()))
            .collect(Collectors.toList());
        return apiInteractor.getDirectionsToDestination(
            Point.fromLngLat(origin.getLongitude(), origin.getLatitude()),
            Point.fromLngLat(destination.getLongitude(), destination.getLatitude()),
            intermediatePoints
        )
            .observeOn(schedulerProvider.computation())
            .map(directionsRoute -> {
                validateWaypointRoute(directionsRoute, waypoints);
                return directionsRoute.legs().stream()
                    .map(leg -> {
                        // Individual route legs don't have geometry, but the steps do. So, get the geometry from each
                        // step, decode to waypoints, and flatten into a list.
                        final List<LatLng> legRoute = leg.steps().stream()
                            .map(LegStep::geometry)
                            .map(polyLineDecoder::decode)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
                        return new RouteInfoModel(
                            legRoute,
                            (long) (leg.duration() * 1000),
                            leg.distance()
                        );
                    })
                    .collect(Collectors.toList());
            });
    }

    private static void validateDirectionsRoute(final DirectionsRoute directionsRoute) throws RouteException {
        if (directionsRoute.geometry() == null || directionsRoute.duration() == null || directionsRoute.distance() == null) {
            throw new RouteException("Invalid route");
        }
    }

    private static void validateWaypointRoute(final DirectionsRoute directionsRoute,
                                              final List<LatLng> waypoints) throws RouteException {
        if (directionsRoute.legs() == null || directionsRoute.legs().size() != waypoints.size() - 1) {
            throw new RouteException("# legs returned by Mapbox doesn't match the number of waypoints");
        }
        for (final RouteLeg leg : directionsRoute.legs()) {
            validateLeg(leg);
        }
    }

    private static void validateLeg(final RouteLeg leg) throws RouteException {
        if (leg.steps() == null || leg.steps().size() == 0 || leg.duration() == null || leg.distance() == null) {
            throw new RouteException("Invalid leg");
        }
    }

    @Override
    public void shutDown() {

    }

    /**
     * Mapbox orders its coordinates lng,lat so use their decoder for polylines.
     */
    private static class MapboxPolylineDecoder implements PolylineDecoder {
        @Override
        public List<LatLng> decode(final String polyline) {
            // Mapbox returns the geometry in lng,lat order so we have to use their polyline decoder
            return LineString.fromPolyline(polyline, PRECISION_6).coordinates().stream()
                .map(point -> new LatLng(point.latitude(), point.longitude()))
                .collect(Collectors.toList());
        }
    }
}
