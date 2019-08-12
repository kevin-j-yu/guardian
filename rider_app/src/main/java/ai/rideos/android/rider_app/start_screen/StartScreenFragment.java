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
package ai.rideos.android.rider_app.start_screen;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.device.FusedLocationDeviceLocator;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import android.app.Activity;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import io.reactivex.disposables.CompositeDisposable;

public class StartScreenFragment extends FragmentViewController<EmptyArg, StartScreenListener> {
    private CompositeDisposable compositeDisposable;
    private StartScreenViewModel viewModel;

    @Override
    public ControllerTypes<EmptyArg, StartScreenListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, StartScreenListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultStartScreenViewModel(
            getListener(),
            new FusedLocationDeviceLocator(getContext()),
            RiderDependencyRegistry.riderDependencyFactory().getPreviewVehicleInteractor(getContext()),
            ResolvedFleet.get().observeFleetInfo()
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final TransitionSet transitionSet = new TransitionSet()
            .addTransition(new ChangeBounds()
                .addTarget("search_card")
            )
            .addTransition(new Slide(Gravity.BOTTOM)
                .addTarget("search_recycler")
            );
        setSharedElementEnterTransition(transitionSet);
        setExitTransition(transitionSet);
        final View view = inflater.inflate(R.layout.start_screen, container, false);

        final ImageButton menuButton = view.findViewById(R.id.menu_button);
        final Activity activity = getActivity();
        if (activity instanceof OpenMenuListener) {
            final OpenMenuListener openMenuListener = (OpenMenuListener) activity;
            menuButton.setOnClickListener(click -> openMenuListener.openMenu());
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        final View enterDestinationCard = getView().findViewById(R.id.enter_destination_card);
        enterDestinationCard.setOnClickListener(click -> viewModel.startDestinationSearch());

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            MapRelay.get().connectToProvider(viewModel, viewModel::setCurrentMapCenter)
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
        viewModel.destroy();
    }
}
