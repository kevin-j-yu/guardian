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
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class DefaultIdleViewModel implements IdleViewModel {
    private final User user;
    private final DriverVehicleInteractor vehicleInteractor;
    private final StateMachine<IdleViewState> stateMachine;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public DefaultIdleViewModel(final User user,
                                final DriverVehicleInteractor vehicleInteractor) {
        this(user, vehicleInteractor, new DefaultSchedulerProvider());
    }

    public DefaultIdleViewModel(final User user,
                                final DriverVehicleInteractor vehicleInteractor,
                                final SchedulerProvider schedulerProvider) {
        this.user = user;
        this.vehicleInteractor = vehicleInteractor;
        stateMachine = new StateMachine<>(IdleViewState.ONLINE, schedulerProvider);
        compositeDisposable.add(stateMachine.start());
    }

    @Override
    public Observable<IdleViewState> getIdleViewState() {
        return stateMachine.observeCurrentState().distinctUntilChanged();
    }

    @Override
    public void goOffline() {
        switch (stateMachine.getCurrentState()) {
            case ONLINE:
            case FAILED_TO_GO_OFFLINE:
                stateMachine.transition(state -> IdleViewState.GOING_OFFLINE);
                compositeDisposable.add(
                    vehicleInteractor.markVehicleNotReady(user.getId())
                        .subscribe(
                            () -> stateMachine.transition(state -> IdleViewState.OFFLINE),
                            throwable -> {
                                Timber.e(throwable, "Failed to bring vehicle online");
                                stateMachine.transition(state -> IdleViewState.FAILED_TO_GO_OFFLINE);
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
