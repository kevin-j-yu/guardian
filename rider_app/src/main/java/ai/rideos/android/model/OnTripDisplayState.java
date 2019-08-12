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
import java.util.Objects;
import java.util.Optional;

public class OnTripDisplayState {
    public enum Display {
        CURRENT_TRIP,
        CONFIRMING_CANCEL,
        EDITING_PICKUP,
        CONFIRMING_EDIT_PICKUP
    }

    private final Display display;
    private final NamedTaskLocation pickupLocation;
    private final String tripId;

    public OnTripDisplayState(final Display display, final String tripId) {
        this(display, tripId, null);
    }

    public OnTripDisplayState(final Display display, final String tripId, final NamedTaskLocation pickupLocation) {
        this.display = display;
        this.tripId = tripId;
        this.pickupLocation = pickupLocation;
    }

    public String getTripId() {
        return tripId;
    }

    public Display getDisplay() {
        return display;
    }

    public Optional<NamedTaskLocation> getPickupLocation() {
        return Optional.ofNullable(pickupLocation);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof OnTripDisplayState)) {
            return false;
        }
        final OnTripDisplayState otherModel = (OnTripDisplayState) other;
        return display == otherModel.display
            && tripId.equals(otherModel.tripId)
            && Objects.equals(pickupLocation, otherModel.pickupLocation);
    }
}
