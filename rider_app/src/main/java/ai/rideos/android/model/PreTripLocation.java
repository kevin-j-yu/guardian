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

public class PreTripLocation {
    private final DesiredAndAssignedLocation desiredAndAssignedLocation;
    private final boolean wasConfirmedOnMap;

    public PreTripLocation(final DesiredAndAssignedLocation desiredAndAssignedLocation,
                           final boolean wasConfirmedOnMap) {
        this.desiredAndAssignedLocation = desiredAndAssignedLocation;
        this.wasConfirmedOnMap = wasConfirmedOnMap;
    }

    public DesiredAndAssignedLocation getDesiredAndAssignedLocation() {
        return desiredAndAssignedLocation;
    }

    public boolean wasConfirmedOnMap() {
        return wasConfirmedOnMap;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof PreTripLocation)) {
            return false;
        }
        final PreTripLocation otherModel = (PreTripLocation) other;
        return desiredAndAssignedLocation.equals(otherModel.desiredAndAssignedLocation)
            && wasConfirmedOnMap == otherModel.wasConfirmedOnMap;
    }
}
