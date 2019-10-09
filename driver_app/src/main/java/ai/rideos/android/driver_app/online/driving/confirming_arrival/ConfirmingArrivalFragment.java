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
package ai.rideos.android.driver_app.online.driving.confirming_arrival;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.progress.ProgressConnector;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.driving.confirming_arrival.ConfirmingArrivalFragment.ConfirmingArrivalArgs;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import ai.rideos.android.view.HeaderAndBottomDetailView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.io.Serializable;

public class ConfirmingArrivalFragment extends FragmentViewController<ConfirmingArrivalArgs, ConfirmArrivalListener> {
    public static class ConfirmingArrivalArgs implements Serializable {
        private final int passengerDetailTemplate;
        private final int drawableDestinationPinAttr;
        private final Waypoint waypoint;

        public ConfirmingArrivalArgs(@StringRes final int passengerDetailTemplate,
                                     @AttrRes final int drawableDestinationPinAttr,
                                     final Waypoint waypoint) {
            this.passengerDetailTemplate = passengerDetailTemplate;
            this.drawableDestinationPinAttr = drawableDestinationPinAttr;
            this.waypoint = waypoint;
        }
    }

    private CompositeDisposable compositeDisposable;
    private ConfirmingArrivalViewModel arrivalViewModel;

    @Override
    public ControllerTypes<ConfirmingArrivalArgs, ConfirmArrivalListener> getTypes() {
        return new ControllerTypes<>(ConfirmingArrivalArgs.class, ConfirmArrivalListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ResourceProvider resourceProvider = AndroidResourceProvider.forContext(getContext());
        arrivalViewModel = new DefaultConfirmingArrivalViewModel(
            DriverDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext()),
            User.get(getContext()),
            new PotentiallySimulatedDeviceLocator(getContext()),
            resourceProvider,
            getArgs().waypoint,
            resourceProvider.getDrawableId(getArgs().drawableDestinationPinAttr),
            getArgs().passengerDetailTemplate
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
            R.layout.confirming_arrival
        );

        final Button confirmArrivalButton = view.findViewById(R.id.on_trip_action_button);
        confirmArrivalButton.setText(R.string.confirm_arrival_button_text);
        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Button confirmArrivalButton = view.findViewById(R.id.on_trip_action_button);
        confirmArrivalButton.setOnClickListener(click -> arrivalViewModel.confirmArrival());

        final TextView passengerDetail = view.findViewById(R.id.on_trip_passenger_detail);
        passengerDetail.setText(arrivalViewModel.getPassengerDetailText());

        final View expandDetailsButton = view.findViewById(R.id.passenger_detail_row);
        expandDetailsButton.setOnClickListener(click -> getListener().openTripDetails());

        final Button backToNavButton = view.findViewById(R.id.back_to_nav_button);
        backToNavButton.setOnClickListener(click -> getListener().backToNavigation());

        final LoadableDividerView loadableDividerView = view.findViewById(R.id.loadable_divider);

        final TextView onTripAddress = view.findViewById(R.id.on_trip_address);

        final Disposable addressSubscription = arrivalViewModel.getDestinationAddress()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onTripAddress::setText);

        final Disposable mapSubscription = MapRelay.get().connectToProvider(arrivalViewModel);

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(mapSubscription, addressSubscription);

        final ProgressConnector progressConnector = ProgressConnector.newBuilder()
            .disableButtonWhenLoadingOrSuccessful(confirmArrivalButton)
            .showLoadableDividerWhenLoading(loadableDividerView)
            .alertOnFailure(getContext(), R.string.confirm_arrival_failure_message)
            .doOnSuccess(() -> getListener().didConfirmArrival())
            .build();
        compositeDisposable.add(
            progressConnector.connect(
                arrivalViewModel.getConfirmingArrivalProgress().observeOn(AndroidSchedulers.mainThread())
            )
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arrivalViewModel.destroy();
    }
}
