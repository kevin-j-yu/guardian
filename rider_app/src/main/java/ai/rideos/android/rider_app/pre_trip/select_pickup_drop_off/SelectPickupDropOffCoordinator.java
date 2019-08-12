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
package ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.view.BackPropagator;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.LocationSearchInitialState;
import ai.rideos.android.model.PreTripLocation;
import ai.rideos.android.model.SelectPickupDropOffDisplayState;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationArgs;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationType;
import ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.SelectPickupDropOffCoordinator.SelectPickupDropOffInput;
import ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.location_search.LocationSearchFragment;
import androidx.annotation.Nullable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SelectPickupDropOffCoordinator implements Coordinator<SelectPickupDropOffInput>, BackPropagator {
    public static class SelectPickupDropOffInput {
        @Nullable
        private final PreTripLocation initialPickup;
        @Nullable
        private final PreTripLocation initialDropOff;
        private final LocationSearchFocusType initialFocus;

        public SelectPickupDropOffInput(@Nullable final PreTripLocation initialPickup,
                                        @Nullable final PreTripLocation initialDropOff,
                                        final LocationSearchFocusType initialFocus) {
            this.initialPickup = initialPickup;
            this.initialDropOff = initialDropOff;
            this.initialFocus = initialFocus;
        }
    }

    private Disposable disposable;
    private final SelectPickupDropOffViewModel viewModel;
    private final NavigationController navController;

    public SelectPickupDropOffCoordinator(final NavigationController navController,
                                          final SetPickupDropOffListener setPickupDropOffListener) {
        this.navController = navController;
        viewModel = new DefaultSelectPickupDropOffViewModel(setPickupDropOffListener);
    }

    @Override
    public void start(final SelectPickupDropOffInput selectPickupDropOffInput) {
        viewModel.initialize(
            selectPickupDropOffInput.initialPickup,
            selectPickupDropOffInput.initialDropOff,
            selectPickupDropOffInput.initialFocus
        );
        disposable = viewModel.getDisplayState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(displayState -> {
                switch (displayState.getStep()) {
                    case SEARCHING_FOR_PICKUP_DROP_OFF:
                        showLocationSearch(displayState);
                        break;
                    case SETTING_PICKUP_ON_MAP:
                        showSetPickupOnMap();
                        break;
                    case SETTING_DROP_OFF_ON_MAP:
                        showSetDropOffOnMap();
                        break;
                }
            });
    }

    @Override
    public void stop() {
        disposable.dispose();
    }

    @Override
    public void destroy() {
        viewModel.destroy();
    }

    private void showLocationSearch(final SelectPickupDropOffDisplayState displayState) {
        navController.navigateTo(
            new LocationSearchFragment(),
            new LocationSearchInitialState(
                displayState.getFocus(),
                displayState.getPickup(),
                displayState.getDropOff()
            ),
            viewModel
        );
    }

    private void showSetPickupOnMap() {
        navController.navigateTo(
            new ConfirmLocationFragment(),
            new ConfirmLocationArgs(
                false,
                ConfirmLocationType.PICKUP,
                null,
                R.string.set_pickup_on_map_title,
                R.string.set_pickup_on_map_button
            ),
            viewModel
        );
    }

    private void showSetDropOffOnMap() {
        navController.navigateTo(
            new ConfirmLocationFragment(),
            new ConfirmLocationArgs(
                false,
                ConfirmLocationType.DROP_OFF,
                null,
                R.string.set_drop_off_on_map_title,
                R.string.set_drop_off_on_map_button
            ),
            viewModel
        );
    }

    @Override
    public void propagateBackSignal() {
        viewModel.back();
    }
}
