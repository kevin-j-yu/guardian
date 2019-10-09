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
import ai.rideos.android.common.model.UserProfile;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class DefaultAccountSettingsViewModel implements AccountSettingsViewModel {
    private static final int RETRY_COUNT = 3;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final BehaviorSubject<String> editedPreferredNameSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> editedPhoneNumberSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> savedPreferredNameSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> savedPhoneNumberSubject = BehaviorSubject.create();

    private final User user;
    private final UserProfileInteractor userProfileInteractor;
    private final SchedulerProvider schedulerProvider;

    public DefaultAccountSettingsViewModel(final User user,
                                           final UserProfileInteractor userProfileInteractor) {
        this(user, userProfileInteractor, new DefaultSchedulerProvider());
    }

    public DefaultAccountSettingsViewModel(final User user,
                                           final UserProfileInteractor userProfileInteractor,
                                           final SchedulerProvider schedulerProvider) {
        this.user = user;
        this.userProfileInteractor = userProfileInteractor;
        this.schedulerProvider = schedulerProvider;

        compositeDisposable.add(
            userProfileInteractor.getUserProfile(user.getId())
                .retry(RetryBehaviors.DEFAULT_RETRY_COUNT)
                .subscribe(
                    profile -> {
                        savedPreferredNameSubject.onNext(profile.getPreferredName());
                        savedPhoneNumberSubject.onNext(profile.getPhoneNumber());
                    },
                    error -> Timber.e(error, "Couldn't save user profile")
                )
        );
    }

    @Override
    public void save() {
        final String preferredName = getCurrentFieldValue(editedPreferredNameSubject, savedPreferredNameSubject);
        final String phoneNumber = getCurrentFieldValue(editedPhoneNumberSubject, savedPhoneNumberSubject);

        // TODO consider exposing this completable to the view controller so it can display errors
        compositeDisposable.add(
            userProfileInteractor.storeUserProfile(
                user.getId(),
                new UserProfile(preferredName, phoneNumber)
            )
                .retry(RetryBehaviors.DEFAULT_RETRY_COUNT)
                .subscribe(
                    () -> {
                        savedPreferredNameSubject.onNext(preferredName);
                        savedPhoneNumberSubject.onNext(phoneNumber);
                    },
                    error -> Timber.e(error, "Couldn't save user profile")
                )
        );
    }

    private String getCurrentFieldValue(final BehaviorSubject<String> editedValueSubject,
                                        final BehaviorSubject<String> savedValueSubject) {
        final String editedValue = editedValueSubject.getValue();
        return editedValue == null ? savedValueSubject.getValue() : editedValue;
    }

    @Override
    public void editPreferredName(final String preferredName) {
        editedPreferredNameSubject.onNext(preferredName);
    }

    @Override
    public void editPhoneNumber(final String phoneNumber) {
        editedPhoneNumberSubject.onNext(phoneNumber);
    }

    @Override
    public Observable<String> getPreferredName() {
        return savedPreferredNameSubject;
    }

    @Override
    public Observable<String> getPhoneNumber() {
        return savedPhoneNumberSubject;
    }

    @Override
    public Observable<String> getEmail() {
        return user.fetchUserProfile()
            .observeOn(schedulerProvider.computation())
            .retry(RETRY_COUNT)
            .map(com.auth0.android.result.UserProfile::getEmail)
            .doOnError(e -> Timber.e(e, "Failed to fetch profile for user"))
            .onErrorReturnItem("")
            .toObservable();
    }

    @Override
    public Observable<Boolean> isSavingEnabled() {
        return Observable.combineLatest(
            observeFieldDifferences(editedPreferredNameSubject, savedPreferredNameSubject),
            observeFieldDifferences(editedPhoneNumberSubject, savedPhoneNumberSubject),
            Boolean::logicalOr
        )
            .observeOn(schedulerProvider.computation())
            .distinctUntilChanged();
    }

    private Observable<Boolean> observeFieldDifferences(final BehaviorSubject<String> editedValueSubject,
                                                        final BehaviorSubject<String> savedValueSubject) {
        return Observable.combineLatest(
            editedValueSubject,
            savedValueSubject,
            (edited, saved) -> !edited.equals(saved)
        )
            .startWith(false);
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        userProfileInteractor.shutDown();
    }
}
