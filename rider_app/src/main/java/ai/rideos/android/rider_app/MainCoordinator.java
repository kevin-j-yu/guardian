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
package ai.rideos.android.rider_app;

import ai.rideos.android.common.architecture.Coordinator;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.NavigationController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.view.BackPropagator;
import ai.rideos.android.common.viewmodel.BackListener;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.rider_app.on_trip.OnTripCoordinator;
import ai.rideos.android.rider_app.pre_trip.PreTripCoordinator;
import ai.rideos.android.rider_app.start_screen.StartScreenFragment;
import android.content.Context;
import io.reactivex.disposables.CompositeDisposable;

public class MainCoordinator implements Coordinator<EmptyArg>, BackPropagator {
    private CompositeDisposable compositeDisposable;

    private final MainViewModel mainViewModel;
    private final NavigationController navController;
    private final Coordinator<EmptyArg> preTripCoordinator;
    private final Coordinator<String> onTripCoordinator;
    private Coordinator activeChild = null;

    public MainCoordinator(final Context context,
                           final NavigationController navController,
                           final BackListener backListener) {
        this.navController = navController;
        mainViewModel = new DefaultMainViewModel(
            backListener,
            RiderDependencyRegistry.riderDependencyFactory().getTripInteractor(context),
            User.get(context)
        );
        preTripCoordinator = new PreTripCoordinator(context, navController, mainViewModel);
        onTripCoordinator = new OnTripCoordinator(context, navController, mainViewModel);
    }

    @Override
    public void start(final EmptyArg emptyArg) {
        mainViewModel.initialize();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(
            mainViewModel.getMainViewState()
                .subscribe(state -> {
                    stopChild();
                    switch (state.getStep()) {
                        case START_SCREEN:
                            navController.navigateTo(new StartScreenFragment(), EmptyArg.create(), mainViewModel);
                            break;
                        case PRE_TRIP:
                            preTripCoordinator.start(EmptyArg.create());
                            activeChild = preTripCoordinator;
                            break;
                        case ON_TRIP:
                            onTripCoordinator.start(state.getTaskId());
                            activeChild = onTripCoordinator;
                            break;
                    }
                })
        );

    }

    private void stopChild() {
        if (activeChild != null) {
            activeChild.stop();
        }
        activeChild = null;
    }

    @Override
    public void stop() {
        stopChild();
        compositeDisposable.dispose();
    }

    @Override
    public void destroy() {
        preTripCoordinator.destroy();
        onTripCoordinator.destroy();
        mainViewModel.destroy();
    }

    @Override
    public void propagateBackSignal() {
        if (activeChild instanceof BackPropagator) {
            ((BackPropagator) activeChild).propagateBackSignal();
        } else {
            mainViewModel.back();
        }
    }
}
