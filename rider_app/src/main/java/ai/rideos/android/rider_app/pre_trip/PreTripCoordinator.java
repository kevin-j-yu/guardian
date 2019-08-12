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
package ai.rideos.android.rider_app.pre_trip;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.view.BackPropagator;
import ai.rideos.android.common.view.errors.ErrorDialog;
import ai.rideos.android.model.PreTripState;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationArgs;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationType;
import ai.rideos.android.rider_app.pre_trip.confirm_trip.ConfirmTripFragment;
import ai.rideos.android.rider_app.pre_trip.confirm_trip.ConfirmTripFragment.ConfirmTripArgs;
import ai.rideos.android.rider_app.pre_trip.confirm_vehicle.ConfirmVehicleFragment;
import ai.rideos.android.rider_app.pre_trip.requesting_trip.RequestingTripFragment;
import ai.rideos.android.rider_app.pre_trip.requesting_trip.RequestingTripFragment.RequestingTripArgs;
import ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.SelectPickupDropOffCoordinator;
import ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.SelectPickupDropOffCoordinator.SelectPickupDropOffInput;
import android.content.Context;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class PreTripCoordinator implements Coordinator<EmptyArg>, BackPropagator {
    private final Context context;
    private final NavigationController navController;
    private final PreTripViewModel preTripViewModel;
    private final Coordinator<SelectPickupDropOffInput> selectPickupDropOffCoordinator;
    private final ErrorDialog errorDialog;

    private Coordinator activeChild = null;
    private CompositeDisposable compositeDisposable;

    public PreTripCoordinator(final Context context,
                              final NavigationController navController,
                              final PreTripListener listener) {
        this.context = context;
        this.navController = navController;
        preTripViewModel = new DefaultPreTripViewModel(
            listener,
            RiderDependencyRegistry.riderDependencyFactory().getTripInteractor(context),
            User.get(context),
            ResolvedFleet.get().observeFleetInfo(),
            SharedPreferencesUserStorageReader.forContext(context)
        );
        selectPickupDropOffCoordinator = new SelectPickupDropOffCoordinator(
            navController,
            preTripViewModel
        );
        errorDialog = new ErrorDialog(context);
    }

    @Override
    public void start(final EmptyArg emptyArg) {
        preTripViewModel.initialize();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(
            preTripViewModel.getPreTripState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    stopChild();
                    switch (state.getStep()) {
                        case SELECTING_PICKUP_DROP_OFF:
                            showSelectPickupDropOff(state);
                            break;
                        case CONFIRMING_PICKUP:
                            showConfirmPickup(state.getPickup().getDesiredAndAssignedLocation().getDesiredLocation());
                            break;
                        case CONFIRMING_DROP_OFF:
                            showConfirmDropOff(state.getDropOff().getDesiredAndAssignedLocation().getDesiredLocation());
                            break;
                        case CONFIRMING_TRIP:
                            // TODO update confirming trip screen to factor in desired/assigned location
                            showConfirmingTrip(
                                state.getPickup().getDesiredAndAssignedLocation().getAssignedLocation(),
                                state.getDropOff().getDesiredAndAssignedLocation().getAssignedLocation()
                            );
                            break;
                        case CONFIRMED:
                            showConfirmed(
                                state.getPickup().getDesiredAndAssignedLocation().getAssignedLocation(),
                                state.getDropOff().getDesiredAndAssignedLocation().getAssignedLocation()
                            );
                            break;
                        case CONFIRMING_VEHICLE:
                            showConfirmVehicle();
                            break;
                    }
                }),
            preTripViewModel.getTripCreationFailures()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(notification -> showTripFailureAlert())
        );
    }

    @Override
    public void stop() {
        stopChild();
        compositeDisposable.dispose();
    }

    @Override
    public void destroy() {
        selectPickupDropOffCoordinator.destroy();
        preTripViewModel.destroy();
    }

    private void showSelectPickupDropOff(final PreTripState state) {
        selectPickupDropOffCoordinator.start(new SelectPickupDropOffInput(
            state.getPickup(),
            state.getDropOff(),
            state.getInitialSearchFocus()
        ));
        activeChild = selectPickupDropOffCoordinator;
    }

    private void showConfirmPickup(final NamedTaskLocation initialValue) {
        navController.navigateTo(
            new ConfirmLocationFragment(),
            new ConfirmLocationArgs(
                true,
                ConfirmLocationType.PICKUP,
                initialValue,
                R.string.confirm_pickup_title_text,
                R.string.confirm_pickup_button_text
            ),
            preTripViewModel
        );
    }

    private void showConfirmDropOff(final NamedTaskLocation initialValue) {
        navController.navigateTo(
            new ConfirmLocationFragment(),
            new ConfirmLocationArgs(
                true,
                ConfirmLocationType.DROP_OFF,
                initialValue,
                R.string.confirm_drop_off_title_text,
                R.string.confirm_drop_off_button_text
            ),
            preTripViewModel
        );
    }

    private void showConfirmingTrip(final NamedTaskLocation pickup,
                                    final NamedTaskLocation dropOff) {
        navController.navigateTo(new ConfirmTripFragment(), new ConfirmTripArgs(pickup, dropOff), preTripViewModel);
    }

    private void showConfirmVehicle() {
        navController.navigateTo(new ConfirmVehicleFragment(), EmptyArg.create(), preTripViewModel);
    }

    private void showConfirmed(final NamedTaskLocation pickup,
                               final NamedTaskLocation dropOff) {
        navController.navigateTo(
            new RequestingTripFragment(),
            new RequestingTripArgs(pickup, dropOff),
            preTripViewModel
        );
    }

    private void stopChild() {
        if (activeChild != null) {
            activeChild.stop();
        }
        activeChild = null;
    }

    @Override
    public void propagateBackSignal() {
        if (activeChild instanceof BackPropagator) {
            ((BackPropagator) activeChild).propagateBackSignal();
        } else {
            preTripViewModel.back();
        }
    }

    private void showTripFailureAlert() {
        errorDialog.show(context.getString(R.string.request_failure_message));
    }
}
