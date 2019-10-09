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
package ai.rideos.android.driver_app.offline;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.settings.DriverStorageKeys;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

public class DefaultOfflineViewModel implements OfflineViewModel {
    private final User user;
    private final UserStorageReader userStorageReader;
    private final UserStorageWriter userStorageWriter;
    private final DriverVehicleInteractor vehicleInteractor;
    private final ProgressSubject progressSubject = new ProgressSubject();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public DefaultOfflineViewModel(final User user,
                                   final UserStorageReader userStorageReader,
                                   final UserStorageWriter userStorageWriter,
                                   final DriverVehicleInteractor vehicleInteractor) {
        this.user = user;
        this.userStorageReader = userStorageReader;
        this.userStorageWriter = userStorageWriter;
        this.vehicleInteractor = vehicleInteractor;
    }

    @Override
    public Observable<ProgressState> getGoingOnlineProgress() {
        return progressSubject.observeProgress();
    }

    @Override
    public Single<Boolean> shouldShowTutorial() {
        return userStorageReader.observeBooleanPreference(DriverStorageKeys.ONLINE_TOGGLE_TUTORIAL_SHOWN)
            // Only show if the tutorial hasn't been shown before
            .map(tutorialShown -> !tutorialShown)
            .firstOrError();
    }

    @Override
    public void goOnline() {
        compositeDisposable.add(progressSubject.followAsyncOperation(
            vehicleInteractor.markVehicleReady(user.getId())
                .doOnComplete(
                    // When the user has successfully gone online, don't show the tutorial anymore
                    () -> userStorageWriter.storeBooleanPreference(DriverStorageKeys.ONLINE_TOGGLE_TUTORIAL_SHOWN, true)
                )
        ));
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        vehicleInteractor.shutDown();
    }
}
