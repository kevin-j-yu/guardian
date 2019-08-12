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
package ai.rideos.android.driver_app.online.driving.drive_pending;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.driving.drive_pending.DrivePendingFragment.DrivePendingArgs;
import ai.rideos.android.view.ActionDetailView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.io.Serializable;

public class DrivePendingFragment extends FragmentViewController<DrivePendingArgs, StartNavigationListener> {
    public static class DrivePendingArgs implements Serializable {
        private final int titleTextResourceId;
        private final LatLng destination;

        public DrivePendingArgs(@StringRes final int titleTextResourceId, final LatLng destination) {
            this.titleTextResourceId = titleTextResourceId;
            this.destination = destination;
        }
    }

    private CompositeDisposable compositeDisposable;
    private DrivePendingViewModel drivingViewModel;

    @Override
    public ControllerTypes<DrivePendingArgs, StartNavigationListener> getTypes() {
        return new ControllerTypes<>(DrivePendingArgs.class, StartNavigationListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drivingViewModel = new DefaultDrivePendingViewModel(
            new PotentiallySimulatedDeviceLocator(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getRouteInteractor(getContext()),
            AndroidResourceProvider.forContext(getContext()),
            getArgs().destination
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.action_detail);
        final ActionDetailView drivingActionView = view.findViewById(R.id.action_detail_container);

        drivingActionView.getTitleView().setText(getArgs().titleTextResourceId);
        drivingActionView.getActionButton().setText(R.string.start_navigation_button_text);
        drivingActionView.getActionButton().setOnClickListener(click -> getListener().startNavigation());
        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        final ActionDetailView drivingActionView = getView().findViewById(R.id.action_detail_container);
        final Disposable mapSubscription = MapRelay.get().connectToProvider(drivingViewModel);

        final Disposable detailSubscription = drivingViewModel.getRouteDetailText()
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
        drivingViewModel.destroy();
    }
}
