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

import java.util.List;

public class RouteInfoModel {
    private final List<LatLng> route;
    private final long travelTimeMillis;
    private final double travelDistanceMeters;

    public RouteInfoModel(final List<LatLng> route, final long travelTimeMillis, final double travelDistanceMeters) {
        this.route = route;
        this.travelTimeMillis = travelTimeMillis;
        this.travelDistanceMeters = travelDistanceMeters;
    }

    public List<LatLng> getRoute() {
        return route;
    }

    public long getTravelTimeMillis() {
        return travelTimeMillis;
    }

    public double getTravelDistanceMeters() {
        return travelDistanceMeters;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof RouteInfoModel)) {
            return false;
        }
        final RouteInfoModel otherModel = (RouteInfoModel) other;
        return route.equals(otherModel.getRoute())
            && travelTimeMillis == otherModel.travelTimeMillis
            && travelDistanceMeters == otherModel.travelDistanceMeters;
    }
}
