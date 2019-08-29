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
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.driving.confirming_arrival.ConfirmingArrivalFragment.ConfirmingArrivalArgs;
import ai.rideos.android.view.ActionDetailView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        private final LatLng destination;

        public ConfirmingArrivalArgs(@StringRes final int titleTextResourceId,
                                     @AttrRes final int drawableDestinationPinAttr,
                                     final LatLng destination) {
            this.titleTextResourceId = titleTextResourceId;
            this.drawableDestinationPinAttr = drawableDestinationPinAttr;
            this.destination = destination;
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
            getArgs().destination,
            resourceProvider.getDrawableId(getArgs().drawableDestinationPinAttr),
            new PotentiallySimulatedDeviceLocator(getContext()),
            resourceProvider
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.action_detail);
        final ActionDetailView drivingActionView = view.findViewById(R.id.action_detail_container);

        drivingActionView.getTitleView().setText(getArgs().titleTextResourceId);
        drivingActionView.getActionButton().setText(R.string.confirm_arrival_button_text);
        drivingActionView.getActionButton().setOnClickListener(click -> getListener().confirmArrival());
        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        final ActionDetailView drivingActionView = getView().findViewById(R.id.action_detail_container);
        final Disposable mapSubscription = MapRelay.get().connectToProvider(arrivalViewModel);

        final Disposable detailSubscription = arrivalViewModel.getArrivalDetailText()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(detail -> drivingActionView.getDetailView().setText(detail));

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(mapSubscription, detailSubscription);
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
