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
package ai.rideos.android.model;

public class RouteTimeDistanceDisplay {
    private final String time;
    private final String distance;

    public RouteTimeDistanceDisplay(final String time, final String distance) {
        this.time = time;
        this.distance = distance;
    }

    public String getTime() {
        return time;
    }

    public String getDistance() {
        return distance;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof RouteTimeDistanceDisplay)) {
            return false;
        }
        final RouteTimeDistanceDisplay otherModel = (RouteTimeDistanceDisplay) other;
        return distance.equals(otherModel.distance) && time.equals(otherModel.time);
    }
}
