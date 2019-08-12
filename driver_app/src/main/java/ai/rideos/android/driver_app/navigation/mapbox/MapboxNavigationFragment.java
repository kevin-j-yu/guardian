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
package ai.rideos.android.driver_app.navigation.mapbox;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.device.FusedLocationDeviceLocator;
import ai.rideos.android.common.interactors.mapbox.MapboxApiInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.device.SimulatedDeviceLocator;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.navigation.DefaultExternalRouteNavigationViewModel;
import ai.rideos.android.driver_app.navigation.ExternalRouteNavigationViewModel;
import ai.rideos.android.driver_app.navigation.NavigationDoneListener;
import ai.rideos.android.driver_app.navigation.mapbox.MapboxNavigationFragment.NavigationArgs;
import ai.rideos.android.settings.DriverStorageKeys;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class MapboxNavigationFragment extends FragmentViewController<NavigationArgs, NavigationDoneListener>
    implements RouteListener, NavigationListener, ProgressChangeListener {
    public static class NavigationArgs implements Serializable {
        private final LatLng destination;
        private final boolean useExternalRouting;

        public NavigationArgs(final LatLng destination, final boolean useExternalRouting) {
            this.destination = destination;
            this.useExternalRouting = useExternalRouting;
        }
    }

    @Override
    public ControllerTypes<NavigationArgs, NavigationDoneListener> getTypes() {
        return new ControllerTypes<>(NavigationArgs.class, NavigationDoneListener.class);
    }

    private CompositeDisposable compositeDisposable;
    @Nullable
    private LocationEngine mapboxLocationEngine;
    private MapboxNavigationViewModel mapboxViewModel;
    private ExternalRouteNavigationViewModel externalNavViewModel;
    private AlertDialog alertDialog;
    private NavigationView navigationView;
    private boolean shouldSimulateRoute;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        compositeDisposable = new CompositeDisposable();

        shouldSimulateRoute = SharedPreferencesUserStorageReader.forContext(getContext())
            .getBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION);

        final DeviceLocator deviceLocator;
        if (shouldSimulateRoute) {
            mapboxLocationEngine = null;
            deviceLocator = SimulatedDeviceLocator.get(getContext());
        } else {
            mapboxLocationEngine = LocationEngineProvider.getBestLocationEngine(getContext());
            deviceLocator = new MapboxDeviceLocator(getContext(), mapboxLocationEngine);
        }
        mapboxViewModel = new MapboxNavigationViewModel(
            deviceLocator,
            new MapboxApiInteractor(getContext())
        );

        if (getArgs().useExternalRouting) {
            externalNavViewModel = new DefaultExternalRouteNavigationViewModel(
                DriverDependencyRegistry.driverDependencyFactory().getRouteInteractor(getContext()),
                new FusedLocationDeviceLocator(getContext())
            );
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        alertDialog = new AlertDialog.Builder(getContext())
            .setTitle(R.string.nav_failure_alert_title)
            .setMessage(R.string.nav_failure_alert_message)
            .setPositiveButton(
                R.string.nav_failure_alert_retry_button,
                (dialog, i) -> initiateRouteRequest()
            )
            .create();
        return inflater.inflate(R.layout.mapbox_nav_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationView = view.findViewById(R.id.nav_view);
        navigationView.onCreate(savedInstanceState);
        // the view model is subscribed to when the view is created because the navigation always runs as long as it's
        // been created, ignoring if the app is closed or in the background
        compositeDisposable.add(
            mapboxViewModel.getInitialCameraPosition().observeOn(AndroidSchedulers.mainThread())
                .subscribe(cameraPosition -> navigationView.initialize(running -> onNavigationReady(), cameraPosition))
        );
    }

    private void onNavigationReady() {
        compositeDisposable.add(
            mapboxViewModel.getDirections().observeOn(AndroidSchedulers.mainThread())
                .subscribe(directionsResult -> {
                    if (directionsResult.isFailure()) {
                        showAlertOnFailure(directionsResult.getError());
                    } else {
                        final NavigationViewOptions options = NavigationViewOptions.builder()
                            .shouldSimulateRoute(shouldSimulateRoute)
                            .directionsRoute(directionsResult.get())
                            .progressChangeListener(this)
                            .routeListener(this)
                            .navigationListener(this)
                            .locationEngine(mapboxLocationEngine)
                            .build();
                        navigationView.startNavigation(options);
                    }
                })
        );
        if (externalNavViewModel != null) {
            compositeDisposable.add(
                externalNavViewModel.getRoute().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(routeResult -> {
                        if (routeResult.isSuccess()) {
                            mapboxViewModel.matchDirectionsToRoute(routeResult.get());
                        } else {
                            showAlertOnFailure(routeResult.getError());
                        }
                    })
            );
        }
        initiateRouteRequest();
    }

    private void initiateRouteRequest() {
        if (externalNavViewModel != null) {
            externalNavViewModel.setDestination(getArgs().destination);
        } else {
            mapboxViewModel.routeTo(getArgs().destination);
        }
    }

    // Display an alert that allows the user to either exit navigation or retry navigation
    private void showAlertOnFailure(final Throwable e) {
        if (alertDialog.isShowing()) {
            return;
        }
        alertDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            getString(R.string.nav_failure_alert_exit_button),
            (dialog, i) -> getListener().finishedNavigationWithError(e)
        );
        alertDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            navigationView.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.dispose();
        navigationView.onDestroy();
    }

    //////////////
    // Mapbox Listeners
    //////////////
    @Override
    public boolean allowRerouteFrom(final Point offRoutePoint, final float heading) {
        if (externalNavViewModel != null) {
            externalNavViewModel.didGoOffRoute(new LocationAndHeading(
                new LatLng(offRoutePoint.latitude(), offRoutePoint.longitude()),
                heading
            ));
            return false;
        }
        // Otherwise, let mapbox figure out re-route
        return true;
    }

    @Override
    public void onOffRoute(final Point offRoutePoint) {
        // Do nothing, we don't care about this
    }

    @Override
    public void onRerouteAlong(final DirectionsRoute directionsRoute) {
        // Do nothing, we don't care about this
    }

    @Override
    public void onFailedReroute(final String errorMessage) {
        // Do nothing, we don't care about this
    }

    @Override
    public void onArrival() {
        navigationView.stopNavigation();
        getListener().finishedNavigation();
    }

    @Override
    public void onCancelNavigation() {
        navigationView.stopNavigation();
        getListener().finishedNavigation();
    }

    @Override
    public void onNavigationFinished() {
        navigationView.stopNavigation();
        getListener().finishedNavigation();
    }

    @Override
    public void onNavigationRunning() {

    }

    @Override
    public void onProgressChange(final Location location, final RouteProgress routeProgress) {
        if (shouldSimulateRoute) {
            SimulatedDeviceLocator.get(getContext()).updateSimulatedLocation(new LocationAndHeading(
                Locations.getLatLngFromAndroidLocation(location),
                Locations.getHeadingFromAndroidLocationOrDefault(location, 0f)
            ));
        }
    }
}
