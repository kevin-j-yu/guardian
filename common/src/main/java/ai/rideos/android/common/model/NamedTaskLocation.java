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

import java.io.Serializable;

/**
 * NamedTaskLocation wraps TaskLocation with a displayable name that could come from reverse geocoding or another source.
 */
public class NamedTaskLocation implements Serializable {
    private final String displayName;
    private final TaskLocation location;

    // Helper constructor for the common case that the task location is just a lat/lng coordinate
    public NamedTaskLocation(final String displayName, final LatLng latLng) {
        this(displayName, new TaskLocation(latLng));
    }

    public NamedTaskLocation(final String displayName, final TaskLocation location) {
        this.displayName = displayName;
        this.location = location;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TaskLocation getLocation() {
        return location;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof NamedTaskLocation)) {
            return false;
        }
        final NamedTaskLocation otherModel = (NamedTaskLocation) other;
        return displayName.equals(otherModel.getDisplayName())
            && location.equals(otherModel.getLocation());
    }
}
