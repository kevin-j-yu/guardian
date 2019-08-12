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
package ai.rideos.android.common.model.map;

import ai.rideos.android.common.model.LatLng;

public class LatLngBounds {
    private final LatLng southwest;
    private final LatLng northeast;

    public LatLngBounds(final LatLng southwest, final LatLng northeast) {
        this.southwest = southwest;
        this.northeast = northeast;
    }

    public LatLng getSouthwestCorner() {
        return southwest;
    }

    public LatLng getNortheastCorner() {
        return northeast;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LatLngBounds)) {
            return false;
        }
        final LatLngBounds otherModel = (LatLngBounds) other;
        return southwest.equals(otherModel.getSouthwestCorner()) && northeast.equals(otherModel.getNortheastCorner());
    }
}
