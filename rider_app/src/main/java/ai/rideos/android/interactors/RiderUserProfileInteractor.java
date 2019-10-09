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
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class RiderUserProfileInteractor implements UserProfileInteractor {
    private final UserStorageReader userStorageReader;
    private final UserStorageWriter userStorageWriter;

    public RiderUserProfileInteractor(final UserStorageReader userStorageReader,
                                      final UserStorageWriter userStorageWriter) {
        this.userStorageReader = userStorageReader;
        this.userStorageWriter = userStorageWriter;
    }

    @Override
    public Completable storeUserProfile(final String userId, final UserProfile userProfile) {
        return Completable.fromAction(() -> {
            userStorageWriter.storeStringPreference(StorageKeys.PREFERRED_NAME, userProfile.getPreferredName());
            userStorageWriter.storeStringPreference(StorageKeys.PHONE_NUMBER, userProfile.getPhoneNumber());
        });
    }

    @Override
    public Single<UserProfile> getUserProfile(final String userId) {
        return Observable.combineLatest(
            userStorageReader.observeStringPreference(StorageKeys.PREFERRED_NAME),
            userStorageReader.observeStringPreference(StorageKeys.PHONE_NUMBER),
            UserProfile::new
        )
            .firstOrError();
    }

    @Override
    public void shutDown() {
        // Nothing to do
    }
}
