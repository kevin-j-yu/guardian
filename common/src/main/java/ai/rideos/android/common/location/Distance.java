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
package ai.rideos.android.common.location;

import ai.rideos.android.common.model.LatLng;
import android.location.Location;

public class Distance {
    private static double MILES_IN_METER = 0.000621371192;

    public static double metersToMiles(final double meters) {
        return meters * MILES_IN_METER;
    }

    public static double milesToMeters(final double miles) {
        return miles / MILES_IN_METER;
    }

    public static double haversineDistanceMeters(final LatLng latLng0, final LatLng latLng1) {
        // We can use Android Location to easily calculate distances
        Location location0 = new Location("");
        Location location1 = new Location("");

        location0.setLatitude(latLng0.getLatitude());
        location0.setLongitude(latLng0.getLongitude());

        location1.setLatitude(latLng1.getLatitude());
        location1.setLongitude(latLng1.getLongitude());

        return location0.distanceTo(location1);
    }
}
