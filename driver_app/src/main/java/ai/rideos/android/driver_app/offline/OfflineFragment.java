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
package ai.rideos.android.driver_app.offline;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.view.errors.ErrorDialog;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.common.viewmodel.map.FollowCurrentLocationMapStateProvider;
import ai.rideos.android.common.viewmodel.map.MapStateProvider;
import ai.rideos.android.device.PotentiallySimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class OfflineFragment extends FragmentViewController<EmptyArg, GoOnlineListener> {
    private CompositeDisposable compositeDisposable;
    private MapStateProvider mapStateProvider;
    private OfflineViewModel offlineViewModel;

    @Override
    public ControllerTypes<EmptyArg, GoOnlineListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, GoOnlineListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapStateProvider = new FollowCurrentLocationMapStateProvider(
            new PotentiallySimulatedDeviceLocator(getContext()),
            R.mipmap.car
        );
        offlineViewModel = new DefaultOfflineViewModel(
            User.get(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.offline);
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Button goOnlineButton = view.findViewById(R.id.go_online_button);
        goOnlineButton.setOnClickListener(click -> offlineViewModel.goOnline());

        final LoadableDividerView loadableDividerView = view.findViewById(R.id.loadable_divider);
        final GoOnlineListener listener = getListener();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(MapRelay.get().connectToProvider(mapStateProvider));
        compositeDisposable.add(
            offlineViewModel.getOfflineViewState().observeOn(AndroidSchedulers.mainThread())
                .subscribe(viewState -> {
                    switch (viewState) {
                        case OFFLINE:
                            goOnlineButton.setEnabled(true);
                            loadableDividerView.stopLoading();
                            break;
                        case GOING_ONLINE:
                            goOnlineButton.setEnabled(false);
                            loadableDividerView.startLoading();
                            break;
                        case ONLINE:
                            listener.didGoOnline();
                            break;
                        case FAILED_TO_GO_ONLINE:
                            goOnlineButton.setEnabled(true);
                            loadableDividerView.stopLoading();
                            showAlertDialog();
                            break;
                    }
                })
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
        offlineViewModel.destroy();
    }

    private void showAlertDialog() {
        new ErrorDialog(getContext())
            .show(getContext().getString(R.string.go_online_failure_message));
    }
}
