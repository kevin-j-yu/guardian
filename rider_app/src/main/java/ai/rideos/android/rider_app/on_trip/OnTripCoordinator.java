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
package ai.rideos.android.rider_app.on_trip;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.view.BackPropagator;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.rider_app.on_trip.confirming_cancel.ConfirmingCancelFragment;
import ai.rideos.android.rider_app.on_trip.confirming_edit_pickup.ConfirmingEditPickupFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.CurrentTripCoordinator;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationArgs;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationType;
import android.content.Context;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class OnTripCoordinator implements Coordinator<String>, BackPropagator {
    private CompositeDisposable compositeDisposable;

    private final NavigationController navController;
    private final OnTripViewModel onTripViewModel;
    private final Coordinator<String> currentTripCoordinator;
    private Coordinator activeChild;

    public OnTripCoordinator(final Context context,
                             final NavigationController navController,
                             final OnTripListener listener) {
        this.navController = navController;
        final User user = User.get(context);
        onTripViewModel = new DefaultOnTripViewModel(
            user,
            RiderDependencyRegistry.riderDependencyFactory().getTripInteractor(context),
            listener
        );
        currentTripCoordinator = new CurrentTripCoordinator(
            context,
            navController,
            onTripViewModel
        );
    }

    @Override
    public void start(final String tripId) {
        compositeDisposable = new CompositeDisposable();
        onTripViewModel.initialize(tripId);
        compositeDisposable.add(
            onTripViewModel.getDisplayState().observeOn(AndroidSchedulers.mainThread()).subscribe(state -> {
                stopChild();
                switch (state.getDisplay()) {
                    case CURRENT_TRIP:
                        currentTripCoordinator.start(tripId);
                        activeChild = currentTripCoordinator;
                        break;
                    case CONFIRMING_CANCEL:
                        navController.navigateTo(
                            new ConfirmingCancelFragment(),
                            EmptyArg.create(),
                            onTripViewModel
                        );
                        break;
                    case EDITING_PICKUP:
                        navController.navigateTo(
                            new ConfirmLocationFragment(),
                            new ConfirmLocationArgs(
                                false,
                                ConfirmLocationType.PICKUP,
                                null,
                                R.string.set_pickup_on_map_title,
                                R.string.set_pickup_on_map_button
                            ),
                            onTripViewModel
                        );
                        break;
                    case CONFIRMING_EDIT_PICKUP:
                        navController.navigateTo(
                            new ConfirmingEditPickupFragment(),
                            EmptyArg.create(),
                            onTripViewModel
                        );
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
        currentTripCoordinator.destroy();
        onTripViewModel.destroy();
    }

    @Override
    public void propagateBackSignal() {
        if (activeChild instanceof BackPropagator) {
            ((BackPropagator) activeChild).propagateBackSignal();
        } else {
            onTripViewModel.back();
        }
    }
}
