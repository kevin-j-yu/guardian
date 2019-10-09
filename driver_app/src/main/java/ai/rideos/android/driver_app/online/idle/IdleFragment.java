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
package ai.rideos.android.driver_app.online.idle;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.app.progress.ProgressConnector;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.view.layout.TopDetailView;
import ai.rideos.android.common.viewmodel.map.FollowCurrentLocationMapStateProvider;
import ai.rideos.android.common.viewmodel.map.MapStateProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class IdleFragment extends FragmentViewController<EmptyArg, GoOfflineListener> {
    private CompositeDisposable compositeDisposable;
    private MapStateProvider mapStateProvider;
    private IdleViewModel idleViewModel;

    @Override
    public ControllerTypes<EmptyArg, GoOfflineListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, GoOfflineListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapStateProvider = new FollowCurrentLocationMapStateProvider(
            new PotentiallySimulatedDeviceLocator(getContext()),
            R.mipmap.car
        );
        idleViewModel = new DefaultIdleViewModel(
            User.get(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = TopDetailView.inflateViewWithDetail(inflater, container, R.layout.online_idle);
        final View topButton = view.findViewById(R.id.top_button);
        final Activity activity = getActivity();
        if (activity instanceof OpenMenuListener) {
            topButton.setOnClickListener(click -> ((OpenMenuListener) activity).openMenu());
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Switch onlineOfflineToggle = view.findViewById(R.id.online_toggle_switch);
        onlineOfflineToggle.setOnCheckedChangeListener((v, checked) -> {
            if (!checked) {
                idleViewModel.goOffline();
            }
        });

        final GoOfflineListener listener = getListener();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(MapRelay.get().connectToProvider(mapStateProvider));

        final ProgressConnector progressConnector = ProgressConnector.newBuilder()
            .toggleSwitchWhenLoading(onlineOfflineToggle)
            .alertOnFailure(getContext(), R.string.go_offline_failure_message)
            .doOnSuccess(listener::didGoOffline)
            .build();
        compositeDisposable.add(
            progressConnector.connect(
                idleViewModel.getGoingOfflineProgress().observeOn(AndroidSchedulers.mainThread())
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
        idleViewModel.destroy();
    }
}
