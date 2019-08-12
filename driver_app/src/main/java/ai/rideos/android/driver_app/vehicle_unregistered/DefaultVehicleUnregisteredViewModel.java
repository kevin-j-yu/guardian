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
package ai.rideos.android.driver_app.vehicle_unregistered;

import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.model.VehicleUnregisteredViewState;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class DefaultVehicleUnregisteredViewModel implements VehicleUnregisteredViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final DoneRegisteringVehicleListener doneRegisteringListener;
    private final StateMachine<VehicleUnregisteredViewState> stateMachine;

    public DefaultVehicleUnregisteredViewModel(final DoneRegisteringVehicleListener doneRegisteringListener) {
        this(doneRegisteringListener, new DefaultSchedulerProvider());
    }

    public DefaultVehicleUnregisteredViewModel(final DoneRegisteringVehicleListener doneRegisteringListener,
                                               final SchedulerProvider schedulerProvider) {
        this.doneRegisteringListener = doneRegisteringListener;
        stateMachine = new StateMachine<>(schedulerProvider);

        compositeDisposable.add(stateMachine.start());
    }

    @Override
    public void initialize() {
        stateMachine.initialize(VehicleUnregisteredViewState.PRE_REGISTRATION);
    }

    @Override
    public Observable<VehicleUnregisteredViewState> getViewState() {
        return stateMachine.observeCurrentState().distinctUntilChanged();
    }

    @Override
    public void startRegistration() {
        stateMachine.transition(state -> VehicleUnregisteredViewState.REGISTER_VEHICLE);
    }

    @Override
    public void cancelRegistration() {
        stateMachine.transition(state -> VehicleUnregisteredViewState.PRE_REGISTRATION);
    }

    @Override
    public void doneRegistering() {
        doneRegisteringListener.doneRegistering();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }
}
