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

public class CameraUpdate {

    public enum UpdateType {
        NO_UPDATE, // NOOP. Use if you cannot filter out camera updates of an observable value
        FIT_LAT_LNG_BOUNDS,
        CENTER_AND_ZOOM
    }

    private final LatLng newPosition;
    private final float newZoom;
    private final LatLngBounds newBounds;
    private final UpdateType updateType;

    private CameraUpdate(final UpdateType updateType,
                         final LatLng newPosition,
                         final float newZoom,
                         final LatLngBounds newBounds) {
        this.updateType = updateType;
        this.newBounds = newBounds;
        this.newPosition = newPosition;
        this.newZoom = newZoom;
    }

    /**
     * noUpdate provides a NOOP camera update
     */
    public static CameraUpdate noUpdate() {
        return new CameraUpdate(UpdateType.NO_UPDATE, null, 0, null);
    }

    public static CameraUpdate fitToBounds(final LatLngBounds latLngBounds) {
        return new CameraUpdate(UpdateType.FIT_LAT_LNG_BOUNDS, null, 0, latLngBounds);
    }

    public static CameraUpdate centerAndZoom(final LatLng newCenter, final float newZoom) {
        return new CameraUpdate(UpdateType.CENTER_AND_ZOOM, newCenter, newZoom, null);
    }

    public LatLng getNewCenter() {
        return newPosition;
    }

    public float getNewZoom() {
        return newZoom;
    }

    public LatLngBounds getNewBounds() {
        return newBounds;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof CameraUpdate)) {
            return false;
        }
        final CameraUpdate otherModel = (CameraUpdate) other;
        if (updateType != otherModel.getUpdateType()) {
            return false;
        }
        switch (updateType) {
            case FIT_LAT_LNG_BOUNDS:
                return newBounds.equals(otherModel.getNewBounds());
            case CENTER_AND_ZOOM:
                return newPosition.equals(otherModel.getNewCenter()) && newZoom == otherModel.getNewZoom();
            case NO_UPDATE:
            default:
                return true;
        }
    }
}
