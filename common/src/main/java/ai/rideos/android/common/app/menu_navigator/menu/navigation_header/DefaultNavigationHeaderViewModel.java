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

import ai.rideos.android.common.app.menu_navigator.account_settings.UserProfileInteractor;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import com.auth0.android.result.UserProfile;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.SingleSubject;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class DefaultNavigationHeaderViewModel implements NavigationHeaderViewModel {
    private static final int DEFAULT_POLLING_INTERVAL_MILLIS = 3000;
    private static final int RETRY_COUNT = 3;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final SingleSubject<UserProfile> userProfileSubject = SingleSubject.create();

    private final User user;
    private final UserProfileInteractor userProfileInteractor;
    private final SchedulerProvider schedulerProvider;

    public DefaultNavigationHeaderViewModel(final User user,
                                            final UserProfileInteractor userProfileInteractor) {
        this(user, userProfileInteractor, new DefaultSchedulerProvider());
    }

    public DefaultNavigationHeaderViewModel(final User user,
                                            final UserProfileInteractor userProfileInteractor,
                                            final SchedulerProvider schedulerProvider) {
        this.user = user;
        this.userProfileInteractor = userProfileInteractor;
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
        // Poll for the full name since it may change periodically
        return Observable.interval(0, DEFAULT_POLLING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, schedulerProvider.io())
            .flatMapSingle(tick ->
                userProfileInteractor.getUserProfile(user.getId())
                    .map(ai.rideos.android.common.model.UserProfile::getPreferredName)
                    .map(Result::success)
                    .onErrorReturn(Result::failure)
            )
            .filter(Result::isSuccess)
            .map(Result::get);
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
