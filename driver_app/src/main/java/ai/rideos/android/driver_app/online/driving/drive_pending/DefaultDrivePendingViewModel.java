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

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.online.DefaultOnTripViewModel;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class DefaultDrivePendingViewModel extends DefaultOnTripViewModel implements DrivePendingViewModel {
    private static final int RETRY_COUNT = 2;
    private static final float PATH_WIDTH = 10.0f;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final SingleSubject<Result<RouteInfoModel>> routeInfoResult = SingleSubject.create();
    private final BehaviorSubject<LocationAndHeading> currentLocation = BehaviorSubject.create();

    private final RouteInteractor routeInteractor;
    private final SchedulerProvider schedulerProvider;
    private final ResourceProvider resourceProvider;
    private final LatLng destination;
    private final int drawableDestinationPin;

    public DefaultDrivePendingViewModel(final DeviceLocator deviceLocator,
                                        final RouteInteractor routeInteractor,
                                        final GeocodeInteractor geocodeInteractor,
                                        final ResourceProvider resourceProvider,
                                        final Waypoint nextWaypoint,
                                        @DrawableRes final int drawableDestinationPin,
                                        @StringRes final int passengerDetailTemplate) {
        this(
            deviceLocator,
            routeInteractor,
            geocodeInteractor,
            resourceProvider,
            nextWaypoint,
            drawableDestinationPin,
            passengerDetailTemplate,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultDrivePendingViewModel(final DeviceLocator deviceLocator,
                                        final RouteInteractor routeInteractor,
                                        final GeocodeInteractor geocodeInteractor,
                                        final ResourceProvider resourceProvider,
                                        final Waypoint nextWaypoint,
                                        @DrawableRes final int drawableDestinationPin,
                                        @StringRes final int passengerDetailTemplate,
                                        final SchedulerProvider schedulerProvider) {
        super(geocodeInteractor, nextWaypoint, resourceProvider, passengerDetailTemplate, schedulerProvider);
        this.routeInteractor = routeInteractor;
        this.schedulerProvider = schedulerProvider;
        this.destination = nextWaypoint.getAction().getDestination();
        this.resourceProvider = resourceProvider;
        this.drawableDestinationPin = drawableDestinationPin;
        compositeDisposable.addAll(
            deviceLocator.getLastKnownLocation().subscribe(currentLocation::onNext),
            fetchRouteInfo().subscribe(routeInfoResult::onSuccess)
        );
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        routeInteractor.shutDown();
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(false, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return routeInfoResult.observeOn(schedulerProvider.computation())
            .toObservable()
            .filter(Result::isSuccess)
            .map(routeResponse -> {
                final LatLngBounds bounds = Paths.getBoundsForPath(routeResponse.get().getRoute(), destination);
                return CameraUpdate.fitToBounds(bounds);
            });
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return Observable.combineLatest(
            routeInfoResult.toObservable().filter(Result::isSuccess),
            currentLocation,
            Pair::create
        )
            .observeOn(schedulerProvider.computation())
            .map(routeAndLocation -> {
                final List<LatLng> path = routeAndLocation.first.get().getRoute();
                final LocationAndHeading vehicleLocation = routeAndLocation.second;
                final Map<String, DrawableMarker> markers = new HashMap<>();
                if (path.size() > 0) {
                    markers.put(
                        "destination",
                        new DrawableMarker(destination, 0, drawableDestinationPin, Anchor.BOTTOM)
                    );
                    markers.put(
                        Markers.VEHICLE_KEY,
                        Markers.getVehicleMarker(
                            vehicleLocation.getLatLng(),
                            vehicleLocation.getHeading(),
                            resourceProvider
                        )
                    );
                }
                return markers;
            });
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return routeInfoResult.observeOn(schedulerProvider.computation())
            .toObservable()
            .filter(Result::isSuccess)
            .map(routeResponse -> Collections.singletonList(
                new DrawablePath(
                    routeResponse.get().getRoute(),
                    PATH_WIDTH,
                    resourceProvider.getColor(R.attr.rideos_route_color)
                )
            ));
    }

    private Observable<Result<RouteInfoModel>> fetchRouteInfo() {
        return currentLocation
            .observeOn(schedulerProvider.computation())
            .flatMap(origin -> routeInteractor.getRoute(origin.getLatLng(), destination))
            .retry(RETRY_COUNT)
            .map(Result::success)
            .doOnError(error -> Timber.e(error, "Failed to get route to destination"))
            .onErrorReturn(Result::failure);
    }
}
