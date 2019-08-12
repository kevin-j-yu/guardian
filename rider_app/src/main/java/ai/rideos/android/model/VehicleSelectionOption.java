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

public class VehicleSelectionOption {
    public enum SelectionType {
        AUTOMATIC,
        MANUAL
    }

    private final SelectionType selectionType;
    @Nullable
    private final String vehicleId;

    private VehicleSelectionOption(final SelectionType selectionType,
                                   final @Nullable String vehicleId) {
        this.selectionType = selectionType;
        this.vehicleId = vehicleId;
    }

    public static VehicleSelectionOption automatic() {
        return new VehicleSelectionOption(SelectionType.AUTOMATIC, null);
    }

    public static VehicleSelectionOption manual(final String vehicleId) {
        return new VehicleSelectionOption(SelectionType.MANUAL, vehicleId);
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public Optional<String> getVehicleId() {
        return Optional.ofNullable(vehicleId);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof VehicleSelectionOption)) {
            return false;
        }
        final VehicleSelectionOption otherModel = (VehicleSelectionOption) other;
        return selectionType == otherModel.selectionType && Objects.equals(vehicleId, otherModel.vehicleId);
    }
}
