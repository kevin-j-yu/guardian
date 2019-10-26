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

import ai.rideos.android.common.model.LatLng;
import com.google.gson.Gson;
import java.io.Serializable;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class VehiclePlan {
    public static class Waypoint implements Serializable {
        private final String taskId;
        private final List<String> stepIds;
        private final Action action;

        public Waypoint(final String taskId, final List<String> stepIds, final Action action) {
            this.taskId = taskId;
            this.stepIds = stepIds;
            this.action = action;
        }

        public String getTaskId() {
            return taskId;
        }

        public List<String> getStepIds() {
            return stepIds;
        }

        public Action getAction() {
            return action;
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Waypoint)) {
                return false;
            }
            final Waypoint otherModel = (Waypoint) other;
            return taskId.equals(otherModel.taskId)
                && stepIds.equals(otherModel.stepIds)
                && action.equals(otherModel.action);
        }
    }

    public static class Action implements Serializable {
        public enum ActionType {
            DRIVE_TO_PICKUP,
            LOAD_RESOURCE,
            DRIVE_TO_DROP_OFF
        }

        private final LatLng destination;
        private final ActionType actionType;
        private final TripResourceInfo tripResourceInfo;

        public Action(final LatLng destination,
                      final ActionType actionType,
                      final TripResourceInfo tripResourceInfo) {
            this.destination = destination;
            this.actionType = actionType;
            this.tripResourceInfo = tripResourceInfo;
        }

        public LatLng getDestination() {
            return destination;
        }

        public ActionType getActionType() {
            return actionType;
        }

        public TripResourceInfo getTripResourceInfo() {
            return tripResourceInfo;
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Action)) {
                return false;
            }
            final Action otherModel = (Action) other;
            return destination.equals(otherModel.destination)
                && actionType == otherModel.actionType
                && tripResourceInfo.equals(otherModel.tripResourceInfo);
        }
    }

    private final List<Waypoint> waypoints;

    public VehiclePlan(final List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof VehiclePlan)) {
            return false;
        }
        return waypoints.equals(((VehiclePlan) other).waypoints);
    }

    @NotNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
