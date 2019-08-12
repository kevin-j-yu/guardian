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

import ai.rideos.android.common.R;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.view.resources.ResourceProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Markers {
    public static final String PICKUP_MARKER_KEY = "pickup";
    public static final String DROP_OFF_MARKER_KEY = "drop-off";
    public static final String VEHICLE_KEY = "vehicle";
    public static final String WAYPOINT_KEY_PREFIX = "waypoint-";

    /**
     * Helper function to get drawable markers for the pickup and drop-off locations of a route.
     */
    public static Map<String, DrawableMarker> getMarkersForRoute(final RouteInfoModel routeInfoModel,
                                                                 final ResourceProvider resourceProvider) {
        final List<LatLng> path = routeInfoModel.getRoute();
        final Map<String, DrawableMarker> markers = new HashMap<>();
        if (path.size() > 0) {
            markers.put(PICKUP_MARKER_KEY, getPickupMarker(path.get(0), resourceProvider));
            markers.put(DROP_OFF_MARKER_KEY, getDropOffMarker(path.get(path.size() - 1), resourceProvider));
        }
        return markers;
    }

    public static DrawableMarker getPickupMarker(final LatLng pickup, final ResourceProvider resourceProvider) {
        return getPinMarkerForPosition(
            pickup,
            resourceProvider.getDrawableId(R.attr.rideos_pickup_pin)
        );
    }

    public static DrawableMarker getDropOffMarker(final LatLng dropOff, final ResourceProvider resourceProvider) {
        return getPinMarkerForPosition(
            dropOff,
            resourceProvider.getDrawableId(R.attr.rideos_drop_off_pin)
        );
    }

    public static Map<String, DrawableMarker> getWaypointMarkers(final List<LatLng> waypoints,
                                                                 final ResourceProvider resourceProvider) {
        return IntStream.range(0, waypoints.size())
            .boxed()
            .collect(Collectors.toMap(
                index -> WAYPOINT_KEY_PREFIX + Integer.toString(index),
                index -> new DrawableMarker(
                    waypoints.get(index),
                    0,
                    resourceProvider.getDrawableId(R.attr.rideos_waypoint_pin),
                    Anchor.CENTER
                )
            ));
    }

    public static DrawableMarker getVehicleMarker(final LatLng vehicleLocation,
                                                  final float vehicleHeading,
                                                  final ResourceProvider resourceProvider) {
        return new DrawableMarker(
            vehicleLocation,
            vehicleHeading,
            resourceProvider.getDrawableId(R.attr.rideos_car_icon),
            Anchor.CENTER
        );
    }

    private static DrawableMarker getPinMarkerForPosition(final LatLng position, final int drawableIcon) {
        return new DrawableMarker(position, 0, drawableIcon, Anchor.BOTTOM);
    }
}
