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
package ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_assignment;

import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.DrawablePaths;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.model.NamedPickupDropOff;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.SingleSubject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class DefaultWaitingForAssignmentViewModel implements WaitingForAssignmentViewModel {
    private static final int RETRY_INTERVAL_MILLIS = 1000;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final SchedulerProvider schedulerProvider;
    private final ResourceProvider resourceProvider;
    private final RouteInteractor routeInteractor;
    private final SingleSubject<RouteInfoModel> routeInfoSubject = SingleSubject.create();

    public DefaultWaitingForAssignmentViewModel(final NamedPickupDropOff pickupDropOff,
                                                final RouteInteractor routeInteractor,
                                                final ResourceProvider resourceProvider) {
        this(pickupDropOff, routeInteractor, resourceProvider, new DefaultSchedulerProvider());
    }

    public DefaultWaitingForAssignmentViewModel(final NamedPickupDropOff pickupDropOff,
                                                final RouteInteractor routeInteractor,
                                                final ResourceProvider resourceProvider,
                                                final SchedulerProvider schedulerProvider) {
        this.routeInteractor = routeInteractor;
        this.resourceProvider = resourceProvider;
        this.schedulerProvider = schedulerProvider;
        compositeDisposable.add(
            routeInteractor.getRoute(
                pickupDropOff.getPickup().getLocation().getLatLng(),
                pickupDropOff.getDropOff().getLocation().getLatLng()
            )
                // continually retry on a route failure
                .doOnError(e -> Timber.e(e, "Failed to find route for trip"))
                .retryWhen(errors -> errors.flatMap(error ->
                    Observable.timer(RETRY_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, schedulerProvider.computation())
                ))
                .observeOn(schedulerProvider.computation())
                .subscribe(routeInfoSubject::onSuccess)
        );
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(true, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return routeInfoSubject.observeOn(schedulerProvider.computation())
            .map(routeInfo -> CameraUpdate.fitToBounds(Paths.getBoundsForPath(routeInfo.getRoute())))
            .toObservable();
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return routeInfoSubject.observeOn(schedulerProvider.computation())
            .map(routeInfo -> Markers.getMarkersForRoute(routeInfo, resourceProvider))
            .toObservable();
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return routeInfoSubject.observeOn(schedulerProvider.computation())
            .map(routeInfo -> Collections.singletonList(DrawablePaths.getInactivePath(
                routeInfo.getRoute(),
                resourceProvider
            )))
            .toObservable();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        routeInteractor.shutDown();
    }
}
