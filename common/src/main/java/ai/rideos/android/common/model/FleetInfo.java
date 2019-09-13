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
import java.util.Objects;
import java.util.Optional;

public class FleetInfo {
    private final String id;
    private final String displayName;
    @Nullable
    private final LatLng center;
    private final boolean isPhantom;

    public FleetInfo(final String id) {
        this(id, id, null, false);
    }

    public FleetInfo(final String id, final String displayName, @Nullable final LatLng center, final boolean isPhantom) {
        this.id = id;
        this.displayName = displayName;
        this.center = center;
        this.isPhantom = isPhantom;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<LatLng> getCenter() {
        return Optional.ofNullable(center);
    }

    public boolean isPhantom() {
        return isPhantom;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof FleetInfo)) {
            return false;
        }
        final FleetInfo otherModel = (FleetInfo) other;
        return id.equals(otherModel.id)
            && displayName.equals(otherModel.displayName)
            && Objects.equals(center, otherModel.center)
            && isPhantom == otherModel.isPhantom;
    }
}
