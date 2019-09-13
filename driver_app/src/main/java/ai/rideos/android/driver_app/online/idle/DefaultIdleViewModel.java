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
package ai.rideos.android.driver_app.online.idle;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class DefaultIdleViewModel implements IdleViewModel {
    private final User user;
    private final DriverVehicleInteractor vehicleInteractor;
    private final ProgressSubject progressSubject = new ProgressSubject();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public DefaultIdleViewModel(final User user,
                                final DriverVehicleInteractor vehicleInteractor) {
        this.user = user;
        this.vehicleInteractor = vehicleInteractor;
    }

    @Override
    public Observable<ProgressState> getGoingOfflineProgress() {
        return progressSubject.observeProgress();
    }

    @Override
    public void goOffline() {
        compositeDisposable.add(
            progressSubject.followAsyncOperation(vehicleInteractor.markVehicleNotReady(user.getId()))
        );
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        vehicleInteractor.shutDown();
    }
}
