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
import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.app.progress.ProgressConnector;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.common.view.layout.TopDetailView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
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
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.Balloon.Builder;
import com.skydoves.balloon.BalloonAnimation;
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
            SharedPreferencesUserStorageReader.forContext(getContext()),
            SharedPreferencesUserStorageWriter.forContext(getContext()),
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = TopDetailView.inflateViewWithDetail(inflater, container, R.layout.offline);
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
            if (checked) {

                offlineViewModel.goOnline();
            }
        });

        final GoOnlineListener listener = getListener();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(MapRelay.get().connectToProvider(mapStateProvider));

        compositeDisposable.add(
            offlineViewModel.shouldShowTutorial().observeOn(AndroidSchedulers.mainThread())
                .subscribe(shouldShow -> {
                    if (shouldShow) {
                        showTooltip(onlineOfflineToggle);
                    }
                })
        );

        final ProgressConnector progressConnector = ProgressConnector.newBuilder()
            .toggleSwitchWhenLoading(onlineOfflineToggle)
            .alertOnFailure(getContext(), R.string.go_online_failure_message)
            .doOnSuccess(listener::didGoOnline)
            .build();
        compositeDisposable.add(
            progressConnector.connect(
                offlineViewModel.getGoingOnlineProgress().observeOn(AndroidSchedulers.mainThread())
            )
        );
    }

    private void showTooltip(final Switch onlineOfflineToggle) {
        final ResourceProvider resourceProvider = AndroidResourceProvider.forContext(getContext());
        final Balloon balloon = new Builder(getContext())
            .setArrowSize(8)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setArrowVisible(true)
            .setWidth(285)
            .setHeight(86)
            .setTextSize(17f)
            .setCornerRadius(6f)
            .setText(getString(R.string.go_online_tutorial_text))
            .setTextColor(resourceProvider.getColor(R.attr.rideos_tool_tip_font_color))
            .setBackgroundColor(resourceProvider.getColor(R.attr.rideos_tool_tip_background_color))
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setDismissWhenTouchOutside(true)
            .setLifecycleOwner(this)
            .build();
        balloon.showAlignBottom(onlineOfflineToggle, 0, 16);
        balloon.setOnBalloonClickListener(v -> balloon.dismiss());
        onlineOfflineToggle.setOnClickListener(click -> balloon.dismiss());
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        offlineViewModel.destroy();
    }
}
