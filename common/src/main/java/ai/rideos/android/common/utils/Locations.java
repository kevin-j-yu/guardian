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
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.api.geo.v1.GeoProto.Position;
import android.location.Location;
import com.google.maps.android.SphericalUtil;

/**
 * Locations contains various useful conversions between rideOS models and Google/Android models.
 */
public class Locations {
    private static final int SOUTH_WEST_HEADING_DEGREES = 225;
    private static final int NORTH_EAST_HEADING_DEGREES = 45;

    public static com.google.android.gms.maps.model.LatLng toGoogleLatLng(final LatLng commonLatLng) {
        return new com.google.android.gms.maps.model.LatLng(commonLatLng.getLatitude(), commonLatLng.getLongitude());
    }

    public static LatLng fromGoogleLatLng(final com.google.android.gms.maps.model.LatLng googleLatLng) {
        return new LatLng(googleLatLng.latitude, googleLatLng.longitude);
    }

    public static Position toRideOsPosition(final LatLng latLng) {
        return Position.newBuilder()
            .setLatitude(latLng.getLatitude())
            .setLongitude(latLng.getLongitude())
            .build();
    }

    public static LatLng fromRideOsPosition(final Position position) {
        return new LatLng(position.getLatitude(), position.getLongitude());
    }

    public static LatLngBounds getBoundsFromCenterAndRadius(final LatLng center, final int radiusMeters) {
        return new LatLngBounds(
            fromGoogleLatLng(SphericalUtil.computeOffset(toGoogleLatLng(center), radiusMeters, SOUTH_WEST_HEADING_DEGREES)),
            fromGoogleLatLng(SphericalUtil.computeOffset(toGoogleLatLng(center), radiusMeters, NORTH_EAST_HEADING_DEGREES))
        );
    }

    public static LatLng getLatLngFromAndroidLocation(final Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    /**
     * An Android Location does not always have a heading. This can occur, for example, when the device is motionless.
     * In this event, the default heading is used.
     */
    public static float getHeadingFromAndroidLocationOrDefault(final Location location, final float defaultHeading) {
        return location.hasBearing() ? location.getBearing() : defaultHeading;
    }
}
