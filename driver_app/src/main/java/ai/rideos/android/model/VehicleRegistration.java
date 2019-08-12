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

public class VehicleRegistration {
    private final String preferredName;
    private final String phoneNumber;
    private final String licensePlate;
    private final int riderCapacity;

    public VehicleRegistration(final String preferredName,
                               final String phoneNumber,
                               final String licensePlate,
                               final int riderCapacity) {
        this.preferredName = preferredName;
        this.phoneNumber = phoneNumber;
        this.licensePlate = licensePlate;
        this.riderCapacity = riderCapacity;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public int getRiderCapacity() {
        return riderCapacity;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof VehicleRegistration)) {
            return false;
        }
        final VehicleRegistration otherModel = (VehicleRegistration) other;
        return preferredName.equals(otherModel.preferredName)
            && phoneNumber.equals(otherModel.phoneNumber)
            && licensePlate.equals(otherModel.licensePlate)
            && riderCapacity == otherModel.riderCapacity;
    }
}
