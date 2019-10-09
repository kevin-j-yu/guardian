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
package ai.rideos.android.interactors;

import ai.rideos.android.common.app.menu_navigator.account_settings.UserProfileInteractor;
import ai.rideos.android.common.model.UserProfile;
import ai.rideos.android.common.model.VehicleInfo;
import io.reactivex.Completable;
import io.reactivex.Single;

public class DriverUserProfileInteractor implements UserProfileInteractor {
    private final DriverVehicleInteractor vehicleInteractor;

    public DriverUserProfileInteractor(final DriverVehicleInteractor vehicleInteractor) {
        this.vehicleInteractor = vehicleInteractor;
    }

    @Override
    public Completable storeUserProfile(final String userId, final UserProfile userProfile) {
        return vehicleInteractor.updateContactInfo(
            userId,
            new VehicleInfo.ContactInfo(
                userProfile.getPreferredName(),
                userProfile.getPhoneNumber(),
                // The driver app doesn't set the url, but currently all fields in contact info must be updated
                ""
            )
        );
    }

    @Override
    public Single<UserProfile> getUserProfile(final String userId) {
        return vehicleInteractor.getVehicleInfo(userId)
            .map(vehicleInfo -> new UserProfile(
                vehicleInfo.getContactInfo().getName(),
                vehicleInfo.getContactInfo().getPhoneNumber()
            ));
    }

    @Override
    public void shutDown() {
        vehicleInteractor.shutDown();
    }
}
