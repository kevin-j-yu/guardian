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
package ai.rideos.android.common.app.menu_navigator.account_settings;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import com.auth0.android.result.UserProfile;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class DefaultAccountSettingsViewModel implements AccountSettingsViewModel {
    private static final int RETRY_COUNT = 3;

    private final BehaviorSubject<String> editedPreferredNameSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> savedPreferredNameSubject;

    private final User user;
    private final UserStorageWriter userStorageWriter;
    private final SchedulerProvider schedulerProvider;

    public DefaultAccountSettingsViewModel(final User user,
                                           final UserStorageReader userStorageReader,
                                           final UserStorageWriter userStorageWriter) {
        this(user, userStorageReader, userStorageWriter, new DefaultSchedulerProvider());
    }

    public DefaultAccountSettingsViewModel(final User user,
                                           final UserStorageReader userStorageReader,
                                           final UserStorageWriter userStorageWriter,
                                           final SchedulerProvider schedulerProvider) {
        this.user = user;
        this.userStorageWriter = userStorageWriter;
        this.schedulerProvider = schedulerProvider;

        savedPreferredNameSubject = BehaviorSubject.createDefault(
            userStorageReader.getStringPreference(StorageKeys.PREFERRED_NAME)
        );
    }

    @Override
    public void save() {
        final String preferredName = editedPreferredNameSubject.getValue();

        if (preferredName != null) {
            userStorageWriter.storeStringPreference(StorageKeys.PREFERRED_NAME, preferredName);
            savedPreferredNameSubject.onNext(preferredName);
        }
    }

    @Override
    public void editPreferredName(final String preferredName) {
        editedPreferredNameSubject.onNext(preferredName);
    }

    @Override
    public Observable<String> getPreferredName() {
        return Observable.just(savedPreferredNameSubject.getValue());
    }

    @Override
    public Observable<String> getEmail() {
        return user.fetchUserProfile()
            .observeOn(schedulerProvider.computation())
            .retry(RETRY_COUNT)
            .map(UserProfile::getEmail)
            .doOnError(e -> Timber.e(e, "Failed to fetch profile for user"))
            .onErrorReturnItem("")
            .toObservable();
    }

    @Override
    public Observable<Boolean> isSavingEnabled() {
        return Observable.combineLatest(
            editedPreferredNameSubject,
            savedPreferredNameSubject,
            (edited, saved) -> !edited.equals(saved)
        )
            .observeOn(schedulerProvider.computation())
            .startWith(false)
            .distinctUntilChanged();
    }
}
