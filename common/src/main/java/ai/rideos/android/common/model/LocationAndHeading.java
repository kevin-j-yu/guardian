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
package ai.rideos.android.common.model;

public class LocationAndHeading {
    private final LatLng latLng;
    // Defined as degrees clock-wise from true north
    private final float heading;

    public LocationAndHeading(final LatLng latLng, final float heading) {
        this.latLng = latLng;
        this.heading = heading;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public float getHeading() {
        return heading;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LocationAndHeading)) {
            return false;
        }
        final LocationAndHeading otherModel = (LocationAndHeading) other;
        return latLng.equals(otherModel.latLng) && heading == otherModel.heading;
    }
}
