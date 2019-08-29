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
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class DefaultOfflineViewModel implements OfflineViewModel {
    private final User user;
    private final DriverVehicleInteractor vehicleInteractor;
    private final StateMachine<OfflineViewState> stateMachine;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public DefaultOfflineViewModel(final User user,
                                   final DriverVehicleInteractor vehicleInteractor) {
        this(user, vehicleInteractor, new DefaultSchedulerProvider());
    }

    public DefaultOfflineViewModel(final User user,
                                   final DriverVehicleInteractor vehicleInteractor,
                                   final SchedulerProvider schedulerProvider) {
        this.user = user;
        this.vehicleInteractor = vehicleInteractor;
        stateMachine = new StateMachine<>(OfflineViewState.OFFLINE, schedulerProvider);
        compositeDisposable.add(stateMachine.start());
    }

    @Override
    public Observable<OfflineViewState> getOfflineViewState() {
        return stateMachine.observeCurrentState();
    }

    @Override
    public void goOnline() {
        switch (stateMachine.getCurrentState()) {
            case OFFLINE:
            case FAILED_TO_GO_ONLINE:
                stateMachine.transition(state -> OfflineViewState.GOING_ONLINE);
                compositeDisposable.add(
                    vehicleInteractor.markVehicleReady(user.getId())
                        .subscribe(
                            () -> stateMachine.transition(state -> OfflineViewState.ONLINE),
                            throwable -> {
                                Timber.e(throwable, "Failed to bring vehicle online");
                                stateMachine.transition(state -> OfflineViewState.FAILED_TO_GO_ONLINE);
                            }
                        )
                );
                break;
        }
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        vehicleInteractor.shutDown();
    }
}
