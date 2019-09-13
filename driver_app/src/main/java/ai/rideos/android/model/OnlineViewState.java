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
        DRIVING_TO_DROP_OFF,
        TRIP_DETAILS
    }

    private final DisplayType displayType;
    private final Waypoint currentWaypoint;
    private final VehiclePlan vehiclePlan;

    public static OnlineViewState idle() {
        return new OnlineViewState(DisplayType.IDLE, null, null);
    }

    public static OnlineViewState drivingToPickup(final Waypoint currentWaypoint) {
        return new OnlineViewState(DisplayType.DRIVING_TO_PICKUP, currentWaypoint, null);
    }

    public static OnlineViewState waitingForPassenger(final Waypoint currentWaypoint) {
        return new OnlineViewState(DisplayType.WAITING_FOR_PASSENGER, currentWaypoint, null);
    }

    public static OnlineViewState drivingToDropOff(final Waypoint currentWaypoint) {
        return new OnlineViewState(DisplayType.DRIVING_TO_DROP_OFF, currentWaypoint, null);
    }

    public static OnlineViewState tripDetails(final VehiclePlan vehiclePlan) {
        return new OnlineViewState(DisplayType.TRIP_DETAILS, null, vehiclePlan);
    }

    private OnlineViewState(final DisplayType displayType,
                            final Waypoint currentWaypoint,
                            final VehiclePlan vehiclePlan) {
        this.displayType = displayType;
        this.currentWaypoint = currentWaypoint;
        this.vehiclePlan = vehiclePlan;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public Waypoint getCurrentWaypoint() {
        return currentWaypoint;
    }

    public VehiclePlan getVehiclePlan() {
        return vehiclePlan;
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
            && Objects.equals(currentWaypoint, otherModel.currentWaypoint)
            && Objects.equals(vehiclePlan, otherModel.vehiclePlan);
    }
}
