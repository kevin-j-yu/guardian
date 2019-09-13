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
package ai.rideos.android.driver_app.online.trip_details;

import ai.rideos.android.model.VehiclePlan.Waypoint;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class TripDetail {
    public enum ActionToPerform {
        REJECT_TRIP,
        END_TRIP
    }

    private final Waypoint nextWaypoint;
    private final ActionToPerform actionToPerform;
    private final String passengerName;
    @Nullable
    private final String passengerPhone;

    public TripDetail(final Waypoint nextWaypoint,
                      final ActionToPerform actionToPerform,
                      final String passengerName,
                      @Nullable final String passengerPhone) {
        this.nextWaypoint = nextWaypoint;
        this.actionToPerform = actionToPerform;
        this.passengerName = passengerName;
        this.passengerPhone = passengerPhone;
    }

    public Waypoint getNextWaypoint() {
        return nextWaypoint;
    }

    public ActionToPerform getActionToPerform() {
        return actionToPerform;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public Optional<String> getPassengerPhone() {
        return Optional.ofNullable(passengerPhone);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TripDetail that = (TripDetail) o;
        return nextWaypoint.equals(that.nextWaypoint) &&
            actionToPerform == that.actionToPerform &&
            Objects.equals(passengerName, that.passengerName) &&
            Objects.equals(passengerPhone, that.passengerPhone);
    }
}
