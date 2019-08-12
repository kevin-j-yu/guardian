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

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.driver_app.vehicle_unregistered.pre_registration.PreRegistrationFragment;
import ai.rideos.android.driver_app.vehicle_unregistered.register_vehicle.RegisterVehicleFragment;
import io.reactivex.disposables.CompositeDisposable;

public class VehicleUnregisteredCoordinator implements Coordinator<EmptyArg> {
    private CompositeDisposable compositeDisposable;
    private final NavigationController navController;
    private final VehicleUnregisteredViewModel viewModel;

    public VehicleUnregisteredCoordinator(final NavigationController navController,
                                          final DoneRegisteringVehicleListener doneRegisteringListener) {
        this.navController = navController;
        viewModel = new DefaultVehicleUnregisteredViewModel(doneRegisteringListener);
    }

    @Override
    public void start(final EmptyArg emptyArg) {
        viewModel.initialize();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            viewModel.getViewState()
                .subscribe(state -> {
                    switch (state) {
                        case PRE_REGISTRATION:
                            navController.navigateTo(new PreRegistrationFragment(), EmptyArg.create(), viewModel);
                            break;
                        case REGISTER_VEHICLE:
                            navController.navigateTo(new RegisterVehicleFragment(), EmptyArg.create(), viewModel);
                            break;
                    }
                })
        );
    }

    @Override
    public void stop() {
        compositeDisposable.dispose();
    }

    @Override
    public void destroy() {
        viewModel.destroy();
    }
}
