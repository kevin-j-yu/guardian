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
package ai.rideos.android.rider_app.on_trip.current_trip;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.architecture.ViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.model.FollowTripDisplayState;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.rider_app.on_trip.current_trip.cancelled.CancelledFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.cancelled.CancelledFragment.CancelledArgs;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_drop_off.DrivingToDropOffFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_drop_off.DrivingToDropOffFragment.DrivingToDropOffArgs;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_pickup.DrivingToPickupFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_pickup.DrivingToPickupFragment.DrivingToPickupArgs;
import ai.rideos.android.rider_app.on_trip.current_trip.trip_completed.TripCompletedFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.trip_completed.TripCompletedFragment.TripCompletedArgs;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_assignment.WaitingForAssignmentFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_assignment.WaitingForAssignmentFragment.WaitingForAssignmentArgs;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_pickup.WaitingForPickupFragment;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_pickup.WaitingForPickupFragment.WaitingForPickupArgs;
import android.content.Context;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * CurrentTripViewController is the parent view controller for all current-trip passenger state. The controller listens
 * for display updates and attaches the appropriate view controller.
 */
public class CurrentTripCoordinator implements Coordinator<String> {
    private CompositeDisposable compositeDisposable;

    private final NavigationController navController;
    private final CurrentTripViewModel viewModel;

    public CurrentTripCoordinator(final Context context,
                                  final NavigationController navController,
                                  final CurrentTripListener currentTripListener) {
        this.navController = navController;
        final User user = User.get(context);
        this.viewModel = new DefaultCurrentTripViewModel(
            currentTripListener,
            RiderDependencyRegistry.riderDependencyFactory().getTripStateInteractor(context),
            RiderDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(context),
            user,
            SharedPreferencesUserStorageWriter.forContext(context),
            ResolvedFleet.get().observeFleetInfo()
        );
    }

    @Override
    public void start(final String tripId) {
        viewModel.initialize(tripId);
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            viewModel.getDisplay().observeOn(AndroidSchedulers.mainThread()).subscribe(displayState -> {
                if (displayState.hasStageChanged()) {
                    // If the stage has changed, attach a new view controller for the given stage
                    updateViewController(displayState);
                } else {
                    // If the stage hasn't changed, pass along the new passenger state so the view controller can update
                    // its display
                    propagateUpdate(displayState.getPassengerState());
                }
            })
        );
    }

    private void updateViewController(final FollowTripDisplayState displayState) {
        switch (displayState.getPassengerState().getStage()) {
            case WAITING_FOR_ASSIGNMENT:
                navController.navigateTo(
                    new WaitingForAssignmentFragment(),
                    new WaitingForAssignmentArgs(
                        displayState.getNamedPickupDropOff().getPickup(),
                        displayState.getNamedPickupDropOff().getDropOff()
                    ),
                    viewModel
                );
                break;
            case DRIVING_TO_PICKUP:
                navController.navigateTo(
                    new DrivingToPickupFragment(),
                    new DrivingToPickupArgs(
                        displayState.getNamedPickupDropOff().getPickup(),
                        displayState.getPassengerState().getVehicleInfo().orElse(null)
                    ),
                    viewModel
                );
                break;
            case WAITING_FOR_PICKUP:
                navController.navigateTo(
                    new WaitingForPickupFragment(),
                    new WaitingForPickupArgs(
                        displayState.getNamedPickupDropOff().getPickup(),
                        displayState.getPassengerState().getVehicleInfo().orElse(null)
                    ),
                    viewModel
                );
                break;
            case DRIVING_TO_DROP_OFF:
                navController.navigateTo(
                    new DrivingToDropOffFragment(),
                    new DrivingToDropOffArgs(
                        displayState.getNamedPickupDropOff().getDropOff(),
                        displayState.getPassengerState().getVehicleInfo().orElse(null)
                    ),
                    viewModel
                );
                break;
            case COMPLETED:
                navController.navigateTo(
                    new TripCompletedFragment(),
                    new TripCompletedArgs(displayState.getNamedPickupDropOff().getDropOff()),
                    viewModel
                );
                break;
            case CANCELLED:
                navController.navigateTo(
                    new CancelledFragment(),
                    new CancelledArgs(displayState.getPassengerState().getCancellationReason().orElse(null)),
                    viewModel
                );
                break;
            case UNKNOWN:
                break;
        }
    }

    private void propagateUpdate(final TripStateModel updatedState) {
        final ViewController currentVC = navController.getActiveViewController();
        // TODO make fragments implement PassengerStateObserver
        if (currentVC instanceof PassengerStateObserver) {
            ((PassengerStateObserver) currentVC).updatePassengerState(updatedState);
        }
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
