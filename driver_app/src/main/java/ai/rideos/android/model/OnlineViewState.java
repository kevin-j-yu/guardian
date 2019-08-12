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

import ai.rideos.android.model.VehiclePlan.Waypoint;
import java.util.Objects;

public class OnlineViewState {
    public enum DisplayType {
        IDLE,
        DRIVING_TO_PICKUP,
        WAITING_FOR_PASSENGER,
        DRIVING_TO_DROP_OFF
    }

    private final DisplayType displayType;
    private final Waypoint currentWaypoint;

    public OnlineViewState(final DisplayType displayType, final Waypoint currentWaypoint) {
        this.displayType = displayType;
        this.currentWaypoint = currentWaypoint;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public Waypoint getCurrentWaypoint() {
        return currentWaypoint;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof OnlineViewState)) {
            return false;
        }
        final OnlineViewState otherModel = (OnlineViewState) other;
        return displayType == otherModel.displayType
            && Objects.equals(currentWaypoint, otherModel.currentWaypoint);
    }
}
