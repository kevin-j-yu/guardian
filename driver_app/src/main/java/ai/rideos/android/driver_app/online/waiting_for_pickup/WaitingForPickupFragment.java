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
package ai.rideos.android.driver_app.online.waiting_for_pickup;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.progress.ProgressConnector;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.waiting_for_pickup.WaitingForPickupFragment.WaitingForPickupArgs;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import ai.rideos.android.view.HeaderAndBottomDetailView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.Balloon.Builder;
import com.skydoves.balloon.BalloonAnimation;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class WaitingForPickupFragment extends FragmentViewController<WaitingForPickupArgs, WaitingForPickupListener> {
    public static class WaitingForPickupArgs implements Serializable {
        private final Waypoint waypointToComplete;

        public WaitingForPickupArgs(final Waypoint waypointToComplete) {
            this.waypointToComplete = waypointToComplete;
        }
    }

    private CompositeDisposable compositeDisposable;
    private WaitingForPickupViewModel viewModel;

    @Override
    public ControllerTypes<WaitingForPickupArgs, WaitingForPickupListener> getTypes() {
        return new ControllerTypes<>(WaitingForPickupArgs.class, WaitingForPickupListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultWaitingForPickupViewModel(
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext()),
            DriverDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(getContext()),
            User.get(getContext()),
            getArgs().waypointToComplete,
            AndroidResourceProvider.forContext(getContext()),
            new PotentiallySimulatedDeviceLocator(getContext()),
            SharedPreferencesUserStorageReader.forContext(getContext()),
            SharedPreferencesUserStorageWriter.forContext(getContext()),
            getListener()
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = HeaderAndBottomDetailView.inflate(
            inflater,
            container,
            getActivity(),
            R.layout.on_trip_header,
            R.layout.on_trip_bottom_detail
        );

        final Button confirmPickupButton = view.findViewById(R.id.on_trip_action_button);
        confirmPickupButton.setText(R.string.waiting_for_pickup_button);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();

        final View view = getView();

        final Button confirmPickupButton = view.findViewById(R.id.on_trip_action_button);
        confirmPickupButton.setOnClickListener(click -> viewModel.confirmPickup());

        final View expandDetailsButton = view.findViewById(R.id.passenger_detail_row);

        final LoadableDividerView loadableDividerView = view.findViewById(R.id.loadable_divider);

        final TextView passengerDetail = view.findViewById(R.id.on_trip_passenger_detail);
        passengerDetail.setText(viewModel.getPassengerDetailText());

        final TextView destinationAddress = view.findViewById(R.id.on_trip_address);
        compositeDisposable.add(
            viewModel.getDestinationAddress().observeOn(AndroidSchedulers.mainThread())
                .subscribe(destinationAddress::setText)
        );

        compositeDisposable.add(MapRelay.get().connectToProvider(viewModel));

        final ProgressConnector progressConnector = ProgressConnector.newBuilder()
            .disableButtonWhenLoadingOrSuccessful(confirmPickupButton)
            .showLoadableDividerWhenLoading(loadableDividerView)
            .alertOnFailure(getContext(), R.string.waiting_for_pickup_failure_message)
            .doOnSuccess(() -> getListener().pickedUpPassenger())
            .build();
        compositeDisposable.addAll(
            progressConnector.connect(
                viewModel.getConfirmingPickupProgress().observeOn(AndroidSchedulers.mainThread())
            ),
            viewModel.shouldShowTripDetailTutorial().observeOn(AndroidSchedulers.mainThread())
                .subscribe(shouldShow -> {
                    if (shouldShow) {
                        showTooltip(expandDetailsButton);
                    } else {
                        expandDetailsButton.setOnClickListener(click -> viewModel.openTripDetails());
                    }
                })
        );
    }

    private void showTooltip(final View expandDetailsView) {
        final ResourceProvider resourceProvider = AndroidResourceProvider.forContext(getContext());
        final Balloon balloon = new Builder(getContext())
            .setArrowSize(8)
            .setArrowOrientation(ArrowOrientation.BOTTOM)
            .setArrowVisible(true)
            .setWidth(309)
            .setHeight(78)
            .setTextSize(17f)
            .setCornerRadius(6f)
            .setText(getString(R.string.trip_detail_tooltip_text))
            .setTextColor(resourceProvider.getColor(R.attr.rideos_tool_tip_font_color))
            .setBackgroundColor(resourceProvider.getColor(R.attr.rideos_tool_tip_background_color))
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setLifecycleOwner(this)
            .setDismissWhenTouchOutside(true)
            .build();
        balloon.showAlignTop(expandDetailsView);
        balloon.setOnBalloonClickListener(v -> balloon.dismiss());
        expandDetailsView.setOnClickListener(click -> {
            balloon.dismiss();
            viewModel.openTripDetails();
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }
}
