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
package ai.rideos.android.rider_app.pre_trip.confirm_location;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.DrawablePath.Style;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.StopInteractor;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import com.google.common.collect.ImmutableMap;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class FixedLocationConfirmLocationViewModel implements ConfirmLocationViewModel {
    private static final String UNKNOWN_LOCATION_DISPLAY = "Unknown Location";
    private static final float ZOOM_LEVEL = 16;
    private static final float DEFAULT_PATH_WIDTH = 15.0f;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final BehaviorSubject<LatLng> currentLocationSubject = BehaviorSubject.create();
    private final BehaviorSubject<DesiredAndAssignedLocation> resolvedLocationSubject = BehaviorSubject.create();

    private final SingleSubject<LatLng> firstLocationSubject = SingleSubject.create();
    private final ProgressSubject geocodeProgress;
    private final PublishSubject<Notification> startedMovingSubject = PublishSubject.create();

    private final StopInteractor stopInteractor;
    private final GeocodeInteractor geocodeInteractor;
    private final Consumer<DesiredAndAssignedLocation> onConfirmed;
    private final ResourceProvider resourceProvider;
    private final int centerPinDrawable;
    private final int fixedLocationDrawable;
    private final SchedulerProvider schedulerProvider;

    public FixedLocationConfirmLocationViewModel(final Consumer<DesiredAndAssignedLocation> onConfirmed,
                                                 final StopInteractor stopInteractor,
                                                 final GeocodeInteractor geocodeInteractor,
                                                 final DeviceLocator deviceLocator,
                                                 final Observable<FleetInfo> observableFleet,
                                                 final ResourceProvider resourceProvider,
                                                 final int centerPinDrawable,
                                                 final int fixedLocationDrawable,
                                                 final @Nullable LatLng initialLocation) {
        this(
            onConfirmed,
            stopInteractor,
            geocodeInteractor,
            deviceLocator,
            observableFleet,
            resourceProvider,
            centerPinDrawable,
            fixedLocationDrawable,
            initialLocation,
            new DefaultSchedulerProvider()
        );
    }

    public FixedLocationConfirmLocationViewModel(final Consumer<DesiredAndAssignedLocation> onConfirmed,
                                                 final StopInteractor stopInteractor,
                                                 final GeocodeInteractor geocodeInteractor,
                                                 final DeviceLocator deviceLocator,
                                                 final Observable<FleetInfo> observableFleet,
                                                 final ResourceProvider resourceProvider,
                                                 final int centerPinDrawable,
                                                 final int fixedLocationDrawable,
                                                 final @Nullable LatLng initialLocation,
                                                 final SchedulerProvider schedulerProvider) {
        this.onConfirmed = onConfirmed;
        this.stopInteractor = stopInteractor;
        this.geocodeInteractor = geocodeInteractor;
        this.resourceProvider = resourceProvider;
        this.centerPinDrawable = centerPinDrawable;
        this.fixedLocationDrawable = fixedLocationDrawable;
        this.schedulerProvider = schedulerProvider;

        geocodeProgress = new ProgressSubject(ProgressState.LOADING);

        compositeDisposable.add(
            observableFleet.observeOn(schedulerProvider.computation())
                .firstOrError()
                .subscribe(fleetInfo -> compositeDisposable.add(
                    currentLocationSubject
                        .observeOn(schedulerProvider.computation())
                        .toFlowable(BackpressureStrategy.LATEST)
                        .doOnNext(latLng -> geocodeProgress.started())
                        .flatMapSingle(location -> resolveDesiredLocation(location, fleetInfo), false, 1)
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                resolvedLocationSubject.onNext(result.get());
                                geocodeProgress.idle();
                            } else {
                                geocodeProgress.failed();
                            }
                        })
                ))
        );

        if (initialLocation != null) {
            setInitialCameraPosition(initialLocation);
        } else {
            compositeDisposable.add(
                deviceLocator.getLastKnownLocation()
                    .observeOn(schedulerProvider.computation())
                    .map(LocationAndHeading::getLatLng)
                    .subscribe(this::setInitialCameraPosition)
            );
        }
    }

    @Override
    public void confirmLocation() {
        compositeDisposable.add(
            resolvedLocationSubject
                .observeOn(schedulerProvider.computation())
                .firstOrError()
                .subscribe(onConfirmed::accept)
        );
    }

    @Override
    public void onCameraMoved(final LatLng location) {
        currentLocationSubject.onNext(location);
    }

    @Override
    public Observable<String> getReverseGeocodedLocation() {
        return resolvedLocationSubject
            .observeOn(schedulerProvider.computation())
            .map(DesiredAndAssignedLocation::getAssignedLocation)
            .map(NamedTaskLocation::getDisplayName);
    }

    @Override
    public Observable<ProgressState> getReverseGeocodingProgress() {
        return geocodeProgress.observeProgress();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        stopInteractor.shutDown();
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(
            true,
            CenterPin.ofDrawable(centerPinDrawable)
        ));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return firstLocationSubject
            .observeOn(schedulerProvider.computation())
            .map(currentLocation -> CameraUpdate.centerAndZoom(currentLocation, ZOOM_LEVEL))
            .toObservable();
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return resolvedLocationSubject
            .observeOn(schedulerProvider.computation())
            .map(desiredAndAssigned -> desiredAndAssigned.getAssignedLocation().getLocation().getLatLng())
            .distinctUntilChanged()
            .map(closestPudol -> (Map<String, DrawableMarker>) ImmutableMap.of(
                UUID.randomUUID().toString(), // By using a random string, the marker won't animate when moved
                new DrawableMarker(closestPudol, 0, fixedLocationDrawable, Anchor.BOTTOM)
            ));
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return resolvedLocationSubject
            .observeOn(schedulerProvider.computation())
            .map(desiredAndAssignedLocation -> Collections.singletonList(new DrawablePath(
                Arrays.asList(
                    desiredAndAssignedLocation.getDesiredLocation().getLocation().getLatLng(),
                    desiredAndAssignedLocation.getAssignedLocation().getLocation().getLatLng()
                ),
                DEFAULT_PATH_WIDTH,
                resourceProvider.getColor(ai.rideos.android.common.R.attr.rideos_inactive_route_color),
                Style.DOTTED
            )))
            // Whenever the map starts moving, don't show any paths
            .mergeWith(startedMovingSubject.map(notification -> Collections.emptyList()));
    }

    @Override
    public void onCameraStartedMoving() {
        startedMovingSubject.onNext(Notification.create());
    }

    private Single<Result<DesiredAndAssignedLocation>> resolveDesiredLocation(final LatLng currentLocation,
                                                                              final FleetInfo fleetInfo) {
        return Single.zip(
            reverseGeocode(new TaskLocation(currentLocation))
                .map(result -> resolveFailedGeocode(result, new TaskLocation(currentLocation)))
                .firstOrError(),
            getClosestPudol(currentLocation, fleetInfo.getId())
                .flatMap(closestPudol -> this.reverseGeocode(closestPudol)
                    .map(result -> this.resolveFailedGeocode(result, closestPudol))
                    .firstOrError()
                ),
            DesiredAndAssignedLocation::new
        )
            .map(Result::success)
            .onErrorReturn(Result::failure);
    }

    private Observable<Result<NamedTaskLocation>> reverseGeocode(final TaskLocation location) {
        return geocodeInteractor.getBestReverseGeocodeResult(location.getLatLng())
            .observeOn(schedulerProvider.computation())
            .map(result -> {
                if (result.isSuccess()) {
                    return Result.success(new NamedTaskLocation(result.get().getDisplayName(), location));
                }
                return result;
            })
            .retryWhen(RetryBehaviors.getDefault())
            .onErrorReturn(Result::failure);
    }

    private NamedTaskLocation resolveFailedGeocode(final Result<NamedTaskLocation> geocodedLocation,
                                                   final TaskLocation location) {
        if (geocodedLocation.isFailure()) {
            return new NamedTaskLocation(UNKNOWN_LOCATION_DISPLAY, location);
        }
        return geocodedLocation.get();
    }

    private void setInitialCameraPosition(final LatLng location) {
        firstLocationSubject.onSuccess(location);
        currentLocationSubject.onNext(location);
    }

    // May error
    private Single<TaskLocation> getClosestPudol(final LatLng location, final String fleetId) {
        return stopInteractor.getBestStop(fleetId, location)
            .observeOn(schedulerProvider.computation())
            .retryWhen(RetryBehaviors.getDefault())
            .map(pudol -> new TaskLocation(pudol.getLatLng(), pudol.getId()))
            .firstOrError();
    }
}
