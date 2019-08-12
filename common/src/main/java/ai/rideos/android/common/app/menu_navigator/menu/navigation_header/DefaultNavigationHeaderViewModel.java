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
package ai.rideos.android.common.app.menu_navigator.menu.navigation_header;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import com.auth0.android.result.UserProfile;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.SingleSubject;
import timber.log.Timber;

public class DefaultNavigationHeaderViewModel implements NavigationHeaderViewModel {
    private static final int RETRY_COUNT = 3;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final SingleSubject<UserProfile> userProfileSubject = SingleSubject.create();

    private final SchedulerProvider schedulerProvider;
    private final UserStorageReader userStorageReader;

    public DefaultNavigationHeaderViewModel(final User user, final UserStorageReader userStorageReader) {
        this(user, userStorageReader, new DefaultSchedulerProvider());
    }

    public DefaultNavigationHeaderViewModel(final User user,
                                            final UserStorageReader userStorageReader,
                                            final SchedulerProvider schedulerProvider) {
        this.userStorageReader = userStorageReader;
        this.schedulerProvider = schedulerProvider;
        compositeDisposable.add(
            user.fetchUserProfile()
                .observeOn(schedulerProvider.computation())
                .retry(RETRY_COUNT)
                .subscribe(userProfileSubject::onSuccess, e -> Timber.e(e, "Failed to fetch user profile"))
        );
    }

    @Override
    public Observable<String> getProfilePictureUrl() {
        return userProfileSubject.map(UserProfile::getPictureURL)
            .toObservable();
    }

    @Override
    public Observable<String> getFullName() {
        return userStorageReader.observeStringPreference(StorageKeys.PREFERRED_NAME);
    }

    @Override
    public Observable<String> getEmail() {
        return userProfileSubject.map(UserProfile::getEmail)
            .toObservable();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }
}
