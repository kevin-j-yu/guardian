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
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.view.strings.RouteFormatter;
import ai.rideos.android.driver_app.R;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.SingleSubject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class DefaultDrivePendingViewModel implements DrivePendingViewModel {
    private static final int RETRY_COUNT = 2;
    private static final float PATH_WIDTH = 10.0f;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final SingleSubject<Result<RouteInfoModel>> routeInfoResult = SingleSubject.create();

    private final RouteInteractor routeInteractor;
    private final SchedulerProvider schedulerProvider;
    private final ResourceProvider resourceProvider;
    private final LatLng destination;
    private final RouteFormatter routeFormatter;

    public DefaultDrivePendingViewModel(final DeviceLocator deviceLocator,
                                        final RouteInteractor routeInteractor,
                                        final ResourceProvider resourceProvider,
                                        final LatLng destination) {
        this(
            deviceLocator,
            routeInteractor,
            resourceProvider,
            destination,
            new DefaultSchedulerProvider(),
            new RouteFormatter(resourceProvider)
        );
    }

    public DefaultDrivePendingViewModel(final DeviceLocator deviceLocator,
                                        final RouteInteractor routeInteractor,
                                        final ResourceProvider resourceProvider,
                                        final LatLng destination,
                                        final SchedulerProvider schedulerProvider,
                                        final RouteFormatter routeFormatter) {
        this.routeInteractor = routeInteractor;
        this.schedulerProvider = schedulerProvider;
        this.destination = destination;
        this.resourceProvider = resourceProvider;
        this.routeFormatter = routeFormatter;
        compositeDisposable.addAll(
            fetchRouteInfo(deviceLocator)
                .subscribe(routeInfoResult::onSuccess)
        );
    }

    @Override
    public Observable<String> getRouteDetailText() {
        return routeInfoResult.observeOn(schedulerProvider.computation())
            .toObservable()
            .map(routeFormatter::getDisplayStringForRouteResult);
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        routeInteractor.shutDown();
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(true, CenterPin.hidden()));
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
        return routeInfoResult.observeOn(schedulerProvider.computation())
            .toObservable()
            .filter(Result::isSuccess)
            .map(routeResponse -> Markers.getMarkersForRoute(routeResponse.get(), resourceProvider));
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

    private Observable<Result<RouteInfoModel>> fetchRouteInfo(final DeviceLocator deviceLocator) {
        return deviceLocator.getLastKnownLocation()
            .observeOn(schedulerProvider.computation())
            .flatMapObservable(origin -> routeInteractor.getRoute(origin.getLatLng(), destination))
            .retry(RETRY_COUNT)
            .map(Result::success)
            .doOnError(error -> Timber.e(error, "Failed to get route to destination"))
            .onErrorReturn(Result::failure);
    }
}
