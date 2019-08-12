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

import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * TaskLocation is a location that can be represented by a lat/lng coordinate and/or a backend location ID.
 * The location ID can specify a PUDOL or special stop like an airport terminal.
 */
public class TaskLocation implements Serializable {
    private final LatLng latLng;
    @Nullable
    private final String locationId;

    public TaskLocation(final LatLng latLng) {
        this(latLng, null);
    }

    public TaskLocation(final LatLng latLng, @Nullable final String locationId) {
        this.latLng = latLng;
        this.locationId = locationId;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public Optional<String> getLocationId() {
        return Optional.ofNullable(locationId);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TaskLocation)) {
            return false;
        }
        final TaskLocation otherModel = (TaskLocation) other;
        return Objects.equals(locationId, otherModel.locationId)
            && latLng.equals(otherModel.latLng);
    }
}
