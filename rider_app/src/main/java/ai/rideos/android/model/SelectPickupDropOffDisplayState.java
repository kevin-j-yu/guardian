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

/**
 * Display state for the select pickup/drop-off flow.
 */
public class SelectPickupDropOffDisplayState {
    public enum SetPickupDropOffStep {
        SEARCHING_FOR_PICKUP_DROP_OFF,
        SETTING_PICKUP_ON_MAP,
        SETTING_DROP_OFF_ON_MAP
    }

    private final SetPickupDropOffStep step;
    private final NamedTaskLocation pickup;
    private final NamedTaskLocation dropOff;
    private final LocationSearchFocusType focus;

    public SelectPickupDropOffDisplayState(final SetPickupDropOffStep step,
                                           final NamedTaskLocation pickup,
                                           final NamedTaskLocation dropOff,
                                           final LocationSearchFocusType focus) {
        this.step = step;
        this.pickup = pickup;
        this.dropOff = dropOff;
        this.focus = focus;
    }

    public SetPickupDropOffStep getStep() {
        return step;
    }

    public NamedTaskLocation getPickup() {
        return pickup;
    }

    public NamedTaskLocation getDropOff() {
        return dropOff;
    }

    public LocationSearchFocusType getFocus() {
        return focus;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SelectPickupDropOffDisplayState)) {
            return false;
        }
        final SelectPickupDropOffDisplayState otherModel = (SelectPickupDropOffDisplayState) other;
        return step == otherModel.step
            && Objects.equals(pickup, otherModel.pickup)
            && Objects.equals(dropOff, otherModel.dropOff);
    }
}
