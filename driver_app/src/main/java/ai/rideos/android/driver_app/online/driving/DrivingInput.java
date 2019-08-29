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
package ai.rideos.android.driver_app.online.driving;

import ai.rideos.android.driver_app.R;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;

public class DrivingInput {
    private final int drivePendingTitleResourceId;
    private final int arrivalTitleResourceId;
    private final int drawableDestinationPinAttr;
    private final Waypoint waypointToComplete;

    public static DrivingInput forPickup(final Waypoint waypointToComplete) {
        return new DrivingInput(
            R.string.drive_pending_pickup_title_text,
            R.string.confirming_arrival_pickup_title_text,
            R.attr.rideos_pickup_pin,
            waypointToComplete
        );
    }

    public static DrivingInput forDropOff(final Waypoint waypointToComplete) {
        return new DrivingInput(
            R.string.drive_pending_drop_off_title_text,
            R.string.confirming_arrival_drop_off_title_text,
            R.attr.rideos_drop_off_pin,
            waypointToComplete
        );
    }

    private DrivingInput(@StringRes final int drivePendingTitleResourceId,
                         @StringRes final int arrivalTitleResourceId,
                         @AttrRes final int drawableDestinationPinAttr,
                         final Waypoint waypointToComplete) {
        this.drivePendingTitleResourceId = drivePendingTitleResourceId;
        this.arrivalTitleResourceId = arrivalTitleResourceId;
        this.drawableDestinationPinAttr = drawableDestinationPinAttr;
        this.waypointToComplete = waypointToComplete;
    }

    public int getDrivePendingTitleResourceId() {
        return drivePendingTitleResourceId;
    }

    public int getArrivalTitleResourceId() {
        return arrivalTitleResourceId;
    }

    public Waypoint getWaypointToComplete() {
        return waypointToComplete;
    }

    public int getDrawableDestinationPinAttr() {
        return drawableDestinationPinAttr;
    }
}
