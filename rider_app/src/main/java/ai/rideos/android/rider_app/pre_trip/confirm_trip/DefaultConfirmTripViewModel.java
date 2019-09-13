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
package ai.rideos.android.rider_app.pre_trip.confirm_trip;

import ai.rideos.android.common.app.MetadataReader;
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
import ai.rideos.android.common.reactive.RetryBehavior;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders;
import ai.rideos.android.common.utils.DrawablePaths;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.view.strings.RouteFormatter;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.model.RouteTimeDistanceDisplay;
import ai.rideos.android.settings.RiderMetadataKeys;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class DefaultConfirmTripViewModel implements ConfirmTripViewModel {
    private static final int NO_SEAT_PASSENGER_COUNT = 1;
    private static final Pair<Integer, Integer> DEFAULT_PASSENGER_COUNT_BOUNDS = Pair.create(1, 4);

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final BehaviorSubject<PickupDropOff> pickupDropOffSubject = BehaviorSubject.create();
    private final BehaviorSubject<Result<RouteInfoModel>> routeResponseSubject = BehaviorSubject.create();
    private final ProgressSubject progressSubject;

    private final RouteInteractor routeInteractor;
    private final SchedulerProvider schedulerProvider;
    private final ConfirmTripListener listener;
    private final ResourceProvider resourceProvider;
    private final RouteFormatter routeFormatter;
    private final RetryBehavior retryBehavior;
    private final MetadataReader metadataReader;

    public DefaultConfirmTripViewModel(final ConfirmTripListener listener,
                                       final RouteInteractor routeInteractor,
                                       final ResourceProvider resourceProvider,
                                       final MetadataReader metadataReader) {
        this(
            listener,
            routeInteractor,
            resourceProvider,
            metadataReader,
            new SchedulerProviders.DefaultSchedulerProvider(),
            new RouteFormatter(resourceProvider),
            RetryBehaviors.getDefault()
        );
    }

    public DefaultConfirmTripViewModel(final ConfirmTripListener listener,
                                       final RouteInteractor routeInteractor,
                                       final ResourceProvider resourceProvider,
                                       final MetadataReader metadataReader,
                                       final SchedulerProvider schedulerProvider,
                                       final RouteFormatter routeFormatter,
                                       final RetryBehavior retryBehavior) {
        this.listener = listener;
        this.schedulerProvider = schedulerProvider;
        this.routeInteractor = routeInteractor;
        this.resourceProvider = resourceProvider;
        this.routeFormatter = routeFormatter;
        this.retryBehavior = retryBehavior;
        this.metadataReader = metadataReader;

        progressSubject = new ProgressSubject(ProgressState.LOADING);

        compositeDisposable.add(
            pickupDropOffSubject
                .observeOn(schedulerProvider.computation())
                .flatMap(this::getRouteForPickupDropOff)
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        progressSubject.succeeded();
                    } else {
                        progressSubject.failed();
                    }
                    routeResponseSubject.onNext(response);
                })
        );
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        routeInteractor.shutDown();
    }

    @Override
    public void confirmTrip(final int seatCount) {
        listener.confirmTrip(seatCount);
    }

    @Override
    public void confirmTripWithoutSeats() {
        listener.confirmTrip(NO_SEAT_PASSENGER_COUNT);
    }

    @Override
    public void setOriginAndDestination(final LatLng origin, final LatLng destination) {
        pickupDropOffSubject.onNext(new PickupDropOff(origin, destination));
    }

    @Override
    public Observable<RouteTimeDistanceDisplay> getRouteInformation() {
        return routeResponseSubject.observeOn(schedulerProvider.computation())
            .filter(Result::isSuccess)
            .map(routeResponse -> new RouteTimeDistanceDisplay(
                routeFormatter.getTravelTimeDisplayString(routeResponse.get()),
                routeFormatter.getTravelDistanceDisplayString(routeResponse.get())
            ));
    }

    @Override
    public Observable<ProgressState> getFetchingRouteProgress() {
        return progressSubject.observeProgress();
    }

    @Override
    public Single<Pair<Integer, Integer>> getPassengerCountBounds() {
        return Single.just(DEFAULT_PASSENGER_COUNT_BOUNDS);
    }

    @Override
    public boolean isSeatSelectionDisabled() {
        return metadataReader.getBooleanMetadata(RiderMetadataKeys.DISABLE_SEAT_SELECTION_KEY)
            .getOrDefault(false);
    }

    private Observable<Result<RouteInfoModel>> getRouteForPickupDropOff(final PickupDropOff pickupDropOff) {
        return routeInteractor.getRoute(pickupDropOff.pickup, pickupDropOff.dropOff)
            .map(Result::success)
            // log errors
            .doOnError(throwable -> Timber.e(throwable, "Failed to find route"))
            // retry a few times
            .retryWhen(retryBehavior)
            // return an error response
            .onErrorReturn(Result::failure);
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(
            false,
            CenterPin.hidden()
        ));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return routeResponseSubject
            .observeOn(schedulerProvider.computation())
            .filter(Result::isSuccess)
            .map(routeResponse -> {
                final PickupDropOff pudo = pickupDropOffSubject.getValue();
                final LatLngBounds bounds = Paths.getBoundsForPath(
                    routeResponse.get().getRoute(),
                    pudo.pickup,
                    pudo.dropOff
                );
                return CameraUpdate.fitToBounds(bounds);
            });
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return routeResponseSubject
            .observeOn(schedulerProvider.computation())
            .filter(Result::isSuccess)
            .map(routeResponse -> Markers.getMarkersForRoute(routeResponse.get(), resourceProvider));
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return routeResponseSubject
            .observeOn(schedulerProvider.computation())
            .filter(Result::isSuccess)
            .map(routeResponse -> Collections.singletonList(DrawablePaths.getInactivePath(
                routeResponse.get().getRoute(),
                resourceProvider
            )));
    }

    private static class PickupDropOff {
        LatLng pickup;
        LatLng dropOff;

        PickupDropOff(final LatLng pickup, final LatLng dropOff) {
            this.pickup = pickup;
            this.dropOff = dropOff;
        }
    }
}
