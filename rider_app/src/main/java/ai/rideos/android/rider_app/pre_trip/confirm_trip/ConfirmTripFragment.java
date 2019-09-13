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
package ai.rideos.android.rider_app.pre_trip.confirm_trip;

import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.progress.ProgressConnector;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.rider_app.pre_trip.confirm_trip.ConfirmTripFragment.ConfirmTripArgs;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class ConfirmTripFragment extends FragmentViewController<ConfirmTripArgs, ConfirmTripListener> {
    public static class ConfirmTripArgs implements Serializable {
        private final NamedTaskLocation pickup;
        private final NamedTaskLocation dropOff;

        public ConfirmTripArgs(final NamedTaskLocation pickup,
                               final NamedTaskLocation dropOff) {
            this.pickup = pickup;
            this.dropOff = dropOff;
        }
    }

    private CompositeDisposable compositeDisposable;
    private ConfirmTripViewModel viewModel;
    private ConfirmSeatsDialogPresenter confirmSeatsVC;

    @Override
    public ControllerTypes<ConfirmTripArgs, ConfirmTripListener> getTypes() {
        return new ControllerTypes<>(ConfirmTripArgs.class, ConfirmTripListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultConfirmTripViewModel(
            getListener(),
            RiderDependencyRegistry.riderDependencyFactory().getRouteInteractor(getContext()),
            AndroidResourceProvider.forContext(getContext()),
            new MetadataReader(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final ConfirmTripListener listener = getListener();
        final View view = BottomDetailAndButtonView.inflateWithUpButton(inflater, container, listener::navigateUp, R.layout.request_ride);

        final TextView pickUpText = view.findViewById(R.id.pickup_address_text);
        pickUpText.setText(getArgs().pickup.getDisplayName());

        final TextView dropOffText = view.findViewById(R.id.drop_off_address_text);
        dropOffText.setText(getArgs().dropOff.getDisplayName());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        confirmSeatsVC = new ConfirmSeatsDialogPresenter(getContext(), viewModel::confirmTrip);

        final ConfirmSeatsDialogView dialogView = (ConfirmSeatsDialogView) getLayoutInflater()
            .inflate(R.layout.confirm_seats_dialog, null);
        confirmSeatsVC.attach(dialogView);

        final Button confirmButton = view.findViewById(R.id.request_ride_button);
        confirmButton.setOnClickListener(click -> {
            if (viewModel.isSeatSelectionDisabled()) {
                viewModel.confirmTripWithoutSeats();
                return;
            }
            compositeDisposable.add(
                viewModel.getPassengerCountBounds()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bounds -> {
                        confirmSeatsVC.setSeatSelectionBounds(bounds.first, bounds.second);
                        confirmSeatsVC.show();
                    })
            );
        });

        final TextView loadingText = view.findViewById(R.id.loading_text);
        final LoadableDividerView loadableDivider = view.findViewById(R.id.loadable_divider);

        viewModel.setOriginAndDestination(
            getArgs().pickup.getLocation().getLatLng(),
            getArgs().dropOff.getLocation().getLatLng()
        );

        final ProgressConnector progressConnector = ProgressConnector.newBuilder()
            .showLoadableDividerWhenLoading(loadableDivider)
            .disableButtonWhenLoading(confirmButton)
            .showTextWhenLoading(loadingText, R.string.optimizing_route_text)
            .build();

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(
            MapRelay.get().connectToProvider(viewModel),
            viewModel.getRouteInformation()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(distanceAndTime -> {
                    final TextView etaText = view.findViewById(R.id.pickup_detail_text);
                    final TextView distanceText = view.findViewById(R.id.distance_text);
                    loadingText.setVisibility(View.GONE); // In the event that the route is processed before the status
                    etaText.setText(distanceAndTime.getTime());
                    distanceText.setText(distanceAndTime.getDistance());
                }),
            progressConnector.connect(
                viewModel.getFetchingRouteProgress().observeOn(AndroidSchedulers.mainThread())
            )
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        confirmSeatsVC.detach();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }
}
