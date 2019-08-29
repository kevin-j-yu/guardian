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
package ai.rideos.android.driver_app;

import static ai.rideos.android.common.viewmodel.state_machine.StateTransitions.transitionIf;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.MainViewState;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.function.Predicate;

public class DefaultMainViewModel implements MainViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final StateMachine<MainViewState> stateMachine;
    private final DriverVehicleInteractor vehicleInteractor;
    private final User user;
    private final SchedulerProvider schedulerProvider;

    public DefaultMainViewModel(final DriverVehicleInteractor vehicleInteractor,
                                final User user) {
        this(vehicleInteractor, user, new DefaultSchedulerProvider());
    }

    public DefaultMainViewModel(final DriverVehicleInteractor vehicleInteractor,
                                final User user,
                                final SchedulerProvider schedulerProvider) {
        this.vehicleInteractor = vehicleInteractor;
        this.user = user;
        this.schedulerProvider = schedulerProvider;
        stateMachine = new StateMachine<>(schedulerProvider);

        compositeDisposable.add(stateMachine.start());
    }

    @Override
    public void initialize() {
        stateMachine.initialize(MainViewState.UNKNOWN);
    }

    @Override
    public Observable<MainViewState> getMainViewState() {
        final Disposable vehicleStatusSubscription = vehicleInteractor.getVehicleStatus(user.getId())
            .observeOn(schedulerProvider.computation())
            .retry()
            .subscribe(status -> {
                switch (status) {
                    case READY:
                        stateMachine.transition(state -> MainViewState.ONLINE);
                        break;
                    case NOT_READY:
                        stateMachine.transition(state -> MainViewState.OFFLINE);
                        break;
                    case UNREGISTERED:
                        stateMachine.transition(state -> MainViewState.UNREGISTERED);
                        break;
                }
            });
        return stateMachine.observeCurrentState()
            .filter(state -> state != MainViewState.UNKNOWN)
            .doOnDispose(vehicleStatusSubscription::dispose);
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }

    @Override
    public void didGoOnline() {
        stateMachine.transition(transitionIf(
            Predicate.isEqual(MainViewState.OFFLINE),
            state -> MainViewState.ONLINE
        ));
    }

    @Override
    public void didGoOffline() {
        stateMachine.transition(transitionIf(
            Predicate.isEqual(MainViewState.ONLINE),
            state -> MainViewState.OFFLINE
        ));
    }

    @Override
    public void doneRegistering() {
        stateMachine.transition(transitionIf(
            state -> state == MainViewState.UNREGISTERED,
            state -> MainViewState.OFFLINE
        ));
    }
}
