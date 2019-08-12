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
package ai.rideos.android.common.model;

public class VehiclePosition {
    private final String vehicleId;
    private final LatLng position;
    private final float heading;

    public VehiclePosition(final String vehicleId, final LatLng position, final float heading) {
        this.vehicleId = vehicleId;
        this.position = position;
        this.heading = heading;
    }

    public LatLng getPosition() {
        return position;
    }

    public float getHeading() {
        return heading;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof VehiclePosition)) {
            return false;
        }
        final VehiclePosition otherModel = (VehiclePosition) other;
        return vehicleId.equals(otherModel.getVehicleId())
            && position.equals(otherModel.getPosition())
            && heading == otherModel.getHeading();
    }
}
