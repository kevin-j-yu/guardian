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
package ai.rideos.android.driver_app.online;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.alerts.NewRideRequestAlertFragment;
import ai.rideos.android.driver_app.alerts.NewRideRequestAlertFragment.NewRideRequestAlertArgs;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.driving.DrivingCoordinator;
import ai.rideos.android.driver_app.online.driving.DrivingInput;
import ai.rideos.android.driver_app.online.idle.GoOfflineListener;
import ai.rideos.android.driver_app.online.idle.IdleFragment;
import ai.rideos.android.driver_app.online.trip_details.TripDetailsFragment;
import ai.rideos.android.driver_app.online.trip_details.TripDetailsFragment.TripDetailsArgs;
import ai.rideos.android.driver_app.online.waiting_for_pickup.WaitingForPickupFragment;
import ai.rideos.android.driver_app.online.waiting_for_pickup.WaitingForPickupFragment.WaitingForPickupArgs;
import android.content.Context;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * OnlineViewController is a parent view controller for controlling the view when the driver's vehicle goes online.
 */
public class OnlineCoordinator implements Coordinator<EmptyArg> {
    private CompositeDisposable compositeDisposable;

    private final NavigationController navController;
    private final OnlineViewModel onlineViewModel;

    private final Coordinator<DrivingInput> drivingCoordinator;
    private Coordinator activeChild = null;

    public OnlineCoordinator(final Context context,
                             final NavigationController navController,
                             final GoOfflineListener goOfflineListener) {
        this.navController = navController;
        final User user = User.get(context);
        onlineViewModel = new DefaultOnlineViewModel(
            goOfflineListener,
            DriverDependencyRegistry.driverDependencyFactory().getDriverPlanInteractor(context),
            new DefaultExternalVehicleRouteSynchronizer(
                DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(context),
                DriverDependencyRegistry.driverDependencyFactory().getRouteInteractor(context),
                user
            ),
            new PotentiallySimulatedDeviceLocator(context),
            user
        );
        drivingCoordinator = new DrivingCoordinator(navController, onlineViewModel);
    }

    @Override
    public void start(final EmptyArg emptyArg) {
        final Disposable viewStateSubscription = onlineViewModel.getOnlineViewState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(state -> {
                stopChild();
                switch (state.getDisplayType()) {
                    case IDLE:
                        navController.navigateTo(new IdleFragment(), EmptyArg.create(), onlineViewModel);
                        return;
                    case DRIVING_TO_PICKUP:
                        drivingCoordinator.start(DrivingInput.forPickup(state.getCurrentWaypoint()));
                        activeChild = drivingCoordinator;
                        return;
                    case DRIVING_TO_DROP_OFF:
                        drivingCoordinator.start(DrivingInput.forDropOff(state.getCurrentWaypoint()));
                        activeChild = drivingCoordinator;
                        return;
                    case WAITING_FOR_PASSENGER:
                        navController.navigateTo(
                            new WaitingForPickupFragment(),
                            new WaitingForPickupArgs(state.getCurrentWaypoint()),
                            onlineViewModel
                        );
                        return;
                    case TRIP_DETAILS:
                        navController.navigateTo(
                            new TripDetailsFragment(),
                            new TripDetailsArgs(state.getVehiclePlan()),
                            onlineViewModel
                        );
                }
            });
        final Disposable alertSubscription = onlineViewModel.getDriverAlerts()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(alert -> {
                switch (alert.getAlertType()) {
                    case NEW_REQUEST:
                        navController.showModal(
                            new NewRideRequestAlertFragment(),
                            new NewRideRequestAlertArgs(alert.getResourceInfo().getNumPassengers()),
                            () -> {} // ignore for now
                        );
                        return;
                }
            });
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(viewStateSubscription, alertSubscription);
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
        drivingCoordinator.destroy();
        onlineViewModel.destroy();
    }
}
