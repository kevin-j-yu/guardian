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

import androidx.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class PreTripState {
    public enum Step {
        SELECTING_PICKUP_DROP_OFF,
        CONFIRMING_DROP_OFF,
        CONFIRMING_PICKUP,
        CONFIRMING_TRIP,
        CONFIRMING_VEHICLE,
        CONFIRMED
    }

    private final PreTripLocation pickup;
    private final PreTripLocation dropOff;
    // Only valid for SELECTING_PICKUP_DROP_OFF state, but unfortunately Java enums can't have associated values.
    private final LocationSearchFocusType initialSearchFocus;
    private final int numPassengers;
    @Nullable
    private final VehicleSelectionOption vehicleSelection;
    private final Step step;

    public PreTripState(final PreTripLocation pickup,
                        final PreTripLocation dropOff,
                        final int numPassengers,
                        final VehicleSelectionOption vehicleSelection,
                        final Step step) {
        this(pickup, dropOff, LocationSearchFocusType.DROP_OFF, numPassengers, vehicleSelection, step);
    }

    public PreTripState(final PreTripLocation pickup,
                        final PreTripLocation dropOff,
                        final LocationSearchFocusType initialSearchFocus,
                        final int numPassengers,
                        @Nullable final VehicleSelectionOption vehicleSelection,
                        final Step step) {
        this.pickup = pickup;
        this.dropOff = dropOff;
        this.initialSearchFocus = initialSearchFocus;
        this.numPassengers = numPassengers;
        this.vehicleSelection = vehicleSelection;
        this.step = step;
    }

    public PreTripLocation getPickup() {
        return pickup;
    }

    public PreTripLocation getDropOff() {
        return dropOff;
    }

    public int getNumPassengers() {
        return numPassengers;
    }

    public Step getStep() {
        return step;
    }

    public LocationSearchFocusType getInitialSearchFocus() {
        return initialSearchFocus;
    }

    public Optional<VehicleSelectionOption> getVehicleSelection() {
        return Optional.ofNullable(vehicleSelection);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof PreTripState)) {
            return false;
        }
        final PreTripState otherModel = (PreTripState) other;
        return Objects.equals(pickup, otherModel.pickup)
            && Objects.equals(dropOff, otherModel.dropOff)
            && numPassengers == otherModel.numPassengers
            && Objects.equals(vehicleSelection, otherModel.vehicleSelection)
            && step == otherModel.step
            && initialSearchFocus == otherModel.initialSearchFocus;
    }
}
