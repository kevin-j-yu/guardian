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
package ai.rideos.android.common.utils;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.api.route.v1.RouteProto.RouteResponse;
import java.util.List;
import java.util.stream.Collectors;

public class Paths {

    public static LatLngBounds getBoundsForPath(final List<LatLng> path, LatLng... additionalPoints) {
        final com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder =
            com.google.android.gms.maps.model.LatLngBounds.builder();

        for (final LatLng latLng : path) {
            boundsBuilder.include(Locations.toGoogleLatLng(latLng));
        }
        for (final LatLng latLng : additionalPoints) {
            boundsBuilder.include(Locations.toGoogleLatLng(latLng));
        }
        final com.google.android.gms.maps.model.LatLngBounds googleBounds = boundsBuilder.build();
        return new LatLngBounds(
            Locations.fromGoogleLatLng(googleBounds.southwest),
            Locations.fromGoogleLatLng(googleBounds.northeast)
        );
    }

    public static RouteInfoModel getRouteInfoFromRideOsRoute(final RouteResponse routeResponse) {
        return new RouteInfoModel(
            routeResponse.getPath().getGeometryList().stream()
                .map(position -> new LatLng(position.getLatitude(), position.getLongitude()))
                .collect(Collectors.toList()),
            routeResponse.getPath().getTravelTime().getMilliseconds(),
            routeResponse.getPath().getDistanceMeters()
        );
    }
}
