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
package ai.rideos.android.driver_app.online.driving;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.driver_app.navigation.mapbox.MapboxNavigationFragment;
import ai.rideos.android.driver_app.navigation.mapbox.MapboxNavigationFragment.NavigationArgs;
import ai.rideos.android.driver_app.online.driving.confirming_arrival.ConfirmingArrivalFragment;
import ai.rideos.android.driver_app.online.driving.confirming_arrival.ConfirmingArrivalFragment.ConfirmingArrivalArgs;
import ai.rideos.android.driver_app.online.driving.drive_pending.DrivePendingFragment;
import ai.rideos.android.driver_app.online.driving.drive_pending.DrivePendingFragment.DrivePendingArgs;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class DrivingCoordinator implements Coordinator<DrivingInput> {
    private CompositeDisposable compositeDisposable;

    private final NavigationController navController;
    private final DrivingViewModel drivingViewModel;

    public DrivingCoordinator(final NavigationController navController,
                              final DrivingListener drivingListener) {
        this.navController = navController;
        this.drivingViewModel = new DefaultDrivingViewModel(drivingListener);
    }

    @Override
    public void start(final DrivingInput input) {
        drivingViewModel.initialize(input.getWaypointToComplete());
        compositeDisposable = new CompositeDisposable();
        final Disposable stateSubscription = drivingViewModel.getDrivingViewState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(state -> {
                final LatLng destination = state.getWaypointToComplete().getAction().getDestination();
                switch (state.getDrivingStep()) {
                    case DRIVE_PENDING:
                        navController.navigateTo(
                            new DrivePendingFragment(),
                            new DrivePendingArgs(
                                input.getPassengerDetailTemplate(),
                                input.getDrawableDestinationPinAttr(),
                                state.getWaypointToComplete()
                            ),
                            drivingViewModel
                        );
                        break;
                    case NAVIGATING:
                        navController.navigateTo(
                            new MapboxNavigationFragment(),
                            new NavigationArgs(destination),
                            drivingViewModel
                        );
                        break;
                    case CONFIRMING_ARRIVAL:
                        navController.navigateTo(
                            new ConfirmingArrivalFragment(),
                            new ConfirmingArrivalArgs(
                                input.getPassengerDetailTemplate(),
                                input.getDrawableDestinationPinAttr(),
                                state.getWaypointToComplete()
                            ),
                            drivingViewModel
                        );
                        break;
                }
            });
        compositeDisposable.add(stateSubscription);
    }

    @Override
    public void stop() {
        compositeDisposable.dispose();
    }

    @Override
    public void destroy() {
        drivingViewModel.destroy();
    }
}
