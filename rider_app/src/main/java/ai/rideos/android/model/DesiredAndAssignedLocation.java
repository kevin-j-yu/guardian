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

import ai.rideos.android.common.model.NamedTaskLocation;

/**
 * DesiredAndAssignedLocation wraps the user's desired pickup/drop-off spot and the one that is actually assigned. In
 * most cases only the desired location is used, but in the event that the app needs to assign a nearby PUDOL or stop,
 * the assigned location is used.
 */
public class DesiredAndAssignedLocation {
    private final NamedTaskLocation desiredLocation;
    private final NamedTaskLocation assignedLocation;

    /**
     * In the case where the desired and assigned location are one in the same, the desired location only need be
     * specified.
     */
    public DesiredAndAssignedLocation(final NamedTaskLocation desiredLocation) {
        this(desiredLocation, desiredLocation);
    }

    public DesiredAndAssignedLocation(final NamedTaskLocation desiredLocation,
                                      final NamedTaskLocation assignedLocation) {
        this.desiredLocation = desiredLocation;
        this.assignedLocation = assignedLocation;
    }

    public NamedTaskLocation getDesiredLocation() {
        return desiredLocation;
    }

    public NamedTaskLocation getAssignedLocation() {
        return assignedLocation;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DesiredAndAssignedLocation)) {
            return false;
        }
        final DesiredAndAssignedLocation otherModel = (DesiredAndAssignedLocation) other;
        return desiredLocation.equals(otherModel.desiredLocation)
            && assignedLocation.equals(otherModel.assignedLocation);
    }
}
