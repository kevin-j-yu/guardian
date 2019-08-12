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

import java.io.Serializable;

public class VehicleInfo implements Serializable {
    public static class ContactInfo implements Serializable {
        private final String url;

        public ContactInfo(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof ContactInfo)) {
                return false;
            }
            final ContactInfo otherModel = (ContactInfo) other;
            return url.equals(otherModel.url);
        }
    }

    private final String licensePlate;
    private final ContactInfo contactInfo;

    public VehicleInfo(final String licensePlate, final ContactInfo contactInfo) {
        this.licensePlate = licensePlate;
        this.contactInfo = contactInfo;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof VehicleInfo)) {
            return false;
        }
        final VehicleInfo otherModel = (VehicleInfo) other;
        return licensePlate.equals(otherModel.licensePlate) && contactInfo.equals(otherModel.contactInfo);
    }
}
