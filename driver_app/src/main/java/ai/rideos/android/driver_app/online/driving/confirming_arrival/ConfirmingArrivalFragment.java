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
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.driving.confirming_arrival.ConfirmingArrivalFragment.ConfirmingArrivalArgs;
import ai.rideos.android.model.VehiclePlan.Waypoint;
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
        private final int titleTextResourceId;
        private final int drawableDestinationPinAttr;
        private final Waypoint waypoint;

        public ConfirmingArrivalArgs(@StringRes final int titleTextResourceId,
                                     @AttrRes final int drawableDestinationPinAttr,
                                     final Waypoint waypoint) {
            this.titleTextResourceId = titleTextResourceId;
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
            getArgs().waypoint,
            resourceProvider.getDrawableId(getArgs().drawableDestinationPinAttr),
            new PotentiallySimulatedDeviceLocator(getContext()),
            resourceProvider
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.confirming_arrival);

        final TextView titleView = view.findViewById(R.id.confirming_arrival_title);
        titleView.setText(getArgs().titleTextResourceId);
        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Button confirmArrivalButton = view.findViewById(R.id.confirm_arrival_button);
        confirmArrivalButton.setOnClickListener(click -> arrivalViewModel.confirmArrival());

        final View expandDetailsButton = view.findViewById(R.id.expand_trip_details_button);
        expandDetailsButton.setOnClickListener(click -> getListener().openTripDetails());

        final LoadableDividerView loadableDividerView = view.findViewById(R.id.loadable_divider);

        final TextView detailText = view.findViewById(R.id.confirming_arrival_detail);

        final Disposable mapSubscription = MapRelay.get().connectToProvider(arrivalViewModel);

        final Disposable detailSubscription = arrivalViewModel.getArrivalDetailText()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(detailText::setText);

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(mapSubscription, detailSubscription);

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
