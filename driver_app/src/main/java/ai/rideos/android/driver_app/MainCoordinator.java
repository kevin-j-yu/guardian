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

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.offline.OfflineFragment;
import ai.rideos.android.driver_app.online.OnlineCoordinator;
import ai.rideos.android.driver_app.vehicle_unregistered.VehicleUnregisteredCoordinator;
import android.content.Context;
import io.reactivex.disposables.CompositeDisposable;

public class MainCoordinator implements Coordinator<EmptyArg> {
    private CompositeDisposable compositeDisposable;

    private final MainViewModel mainViewModel;
    private final NavigationController navController;
    private final Coordinator<EmptyArg> onlineCoordinator;
    private final Coordinator<EmptyArg> unregisteredCoordinator;
    private Coordinator activeChild = null;

    public MainCoordinator(final Context context,
                           final NavigationController navController) {
        this.navController = navController;
        mainViewModel = new DefaultMainViewModel(
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(context),
            User.get(context)
        );
        onlineCoordinator = new OnlineCoordinator(context, navController, mainViewModel);
        unregisteredCoordinator = new VehicleUnregisteredCoordinator(navController, mainViewModel);
    }

    @Override
    public void start(final EmptyArg emptyArg) {
        mainViewModel.initialize();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(
            mainViewModel.getMainViewState()
                .subscribe(state -> {
                    stopChild();
                    switch (state) {
                        case OFFLINE:
                            navController.navigateTo(
                                new OfflineFragment(),
                                EmptyArg.create(),
                                mainViewModel
                            );
                            break;
                        case ONLINE:
                            onlineCoordinator.start(EmptyArg.create());
                            activeChild = onlineCoordinator;
                            break;
                        case UNREGISTERED:
                            unregisteredCoordinator.start(EmptyArg.create());
                            activeChild = unregisteredCoordinator;
                            break;
                    }
                })
        );
    }

    private void stopChild() {
        if (activeChild != null) {
            activeChild.stop();
        }
        activeChild = null;
    }

    @Override
    public void stop() {
        stopChild();
        compositeDisposable.dispose();
    }

    @Override
    public void destroy() {
        onlineCoordinator.destroy();
        unregisteredCoordinator.destroy();
        mainViewModel.destroy();
    }
}
