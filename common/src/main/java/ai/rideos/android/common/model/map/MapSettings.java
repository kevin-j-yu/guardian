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

public class MapSettings {

    private final boolean shouldShowUserLocation;
    private final CenterPin centerPin;

    public MapSettings(final boolean shouldShowUserLocation,
                       final CenterPin centerPin) {
        this.shouldShowUserLocation = shouldShowUserLocation;
        this.centerPin = centerPin;
    }

    public boolean shouldShowUserLocation() {
        return shouldShowUserLocation;
    }

    public CenterPin getCenterPin() {
        return centerPin;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MapSettings)) {
            return false;
        }
        final MapSettings otherModel = (MapSettings) other;
        return shouldShowUserLocation == otherModel.shouldShowUserLocation()
            && centerPin.equals(otherModel.getCenterPin());
    }
}

