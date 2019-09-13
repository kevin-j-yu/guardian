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
import ai.rideos.android.common.location.DistanceCalculator;
import ai.rideos.android.common.location.HaversineDistanceCalculator;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.RetryBehavior;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.rider_app.R;
import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DefaultConfirmLocationViewModel implements ConfirmLocationViewModel {
    private static final String UNKNOWN_LOCATION_DISPLAY = "Unknown Location";
    private static final float ZOOM_LEVEL = 15;
    private static final double LOCATION_TOLERANCE_METERS = 1;

    private final Consumer<DesiredAndAssignedLocation> onConfirmed;
    private final int drawablePin;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final BehaviorSubject<LatLng> currentLocationSubject = BehaviorSubject.create();
    private final ProgressSubject geocodeProgress;
    // Used for creating initial state
    private final SingleSubject<LatLng> firstLocationSubject = SingleSubject.create();
    private final BehaviorSubject<NamedTaskLocation> reverseGeocodeSubject = BehaviorSubject.create();
    private final SchedulerProvider schedulerProvider;
    private final GeocodeInteractor geocodeInteractor;
    private final RetryBehavior retryBehavior;
    private final String currentLocationDisplayString;

    /**
     * Construct a DefaultConfirmLocationViewModel with no initial location (uses current location as default)
     */
    public DefaultConfirmLocationViewModel(final GeocodeInteractor geocodeInteractor,
                                           final DeviceLocator deviceLocator,
                                           final Consumer<DesiredAndAssignedLocation> onConfirmed,
                                           final ResourceProvider resourceProvider,
                                           final int drawablePin) {
        this(
            geocodeInteractor,
            deviceLocator,
            onConfirmed,
            resourceProvider,
            null,
            drawablePin
        );
    }

    /**
     * Construct a DefaultConfirmLocationViewModel with an initial location
     */
    public DefaultConfirmLocationViewModel(final GeocodeInteractor geocodeInteractor,
                                           final DeviceLocator deviceLocator,
                                           final Consumer<DesiredAndAssignedLocation> onConfirmed,
                                           final ResourceProvider resourceProvider,
                                           @Nullable final NamedTaskLocation initialLocation,
                                           final int drawablePin) {
        this(
            onConfirmed,
            deviceLocator,
            resourceProvider,
            initialLocation,
            drawablePin,
            geocodeInteractor,
            new DefaultSchedulerProvider(),
            RetryBehaviors.getDefault(),
            new HaversineDistanceCalculator()
        );
    }

    public DefaultConfirmLocationViewModel(final Consumer<DesiredAndAssignedLocation> onConfirmed,
                                           final DeviceLocator deviceLocator,
                                           final ResourceProvider resourceProvider,
                                           @Nullable final NamedTaskLocation initialLocation,
                                           final int drawablePin,
                                           final GeocodeInteractor geocodeInteractor,
                                           final SchedulerProvider schedulerProvider,
                                           final RetryBehavior retryBehavior,
                                           final DistanceCalculator distanceCalculator) {
        this.onConfirmed = onConfirmed;
        this.drawablePin = drawablePin;
        this.schedulerProvider = schedulerProvider;
        this.geocodeInteractor = geocodeInteractor;
        this.retryBehavior = retryBehavior;

        geocodeProgress = new ProgressSubject(ProgressState.LOADING);

        currentLocationDisplayString = resourceProvider.getString(R.string.current_location_search_option);

        compositeDisposable.add(
            currentLocationSubject
                .observeOn(schedulerProvider.computation())
                .distinctUntilChanged((oldLocation, newLocation) ->
                    distanceCalculator.getDistanceInMeters(oldLocation, newLocation) < LOCATION_TOLERANCE_METERS
                )
                .toFlowable(BackpressureStrategy.LATEST)
                .doOnNext(next -> geocodeProgress.started())
                .flatMap(
                    latLng -> reverseGeocode(latLng, initialLocation, distanceCalculator)
                        .doOnNext(result -> {
                            if (result.isFailure()) {
                                geocodeProgress.failed();
                            } else {
                                geocodeProgress.idle();
                            }
                        })
                        .map(result -> resolveFailedGeocode(result, latLng))
                        .toFlowable(BackpressureStrategy.LATEST),
                    false,
                    1
                )
                .subscribe(reverseGeocodeSubject::onNext)
        );

        if (initialLocation != null) {
            setInitialCameraPosition(initialLocation.getLocation().getLatLng());
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
            reverseGeocodeSubject
                .observeOn(schedulerProvider.computation())
                .firstOrError()
                .map(DesiredAndAssignedLocation::new)
                .subscribe(onConfirmed::accept)
        );
    }

    private void setInitialCameraPosition(final LatLng location) {
        firstLocationSubject.onSuccess(location);
        currentLocationSubject.onNext(location);
    }

    @Override
    public void onCameraMoved(final LatLng location) {
        currentLocationSubject.onNext(location);
    }

    @Override
    public Observable<String> getReverseGeocodedLocation() {
        return reverseGeocodeSubject.map(NamedTaskLocation::getDisplayName);
    }

    @Override
    public Observable<ProgressState> getReverseGeocodingProgress() {
        return geocodeProgress.observeProgress();
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(
            true,
            CenterPin.ofDrawable(drawablePin)
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
        return Observable.just(Collections.emptyMap());
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return Observable.just(Collections.emptyList());
    }

    private Observable<Result<NamedTaskLocation>> reverseGeocode(final LatLng latLng,
                                                                 @Nullable final NamedTaskLocation initialLocation,
                                                                 final DistanceCalculator distanceCalculator) {
        if (shouldReturnInitialLocation(latLng, initialLocation, distanceCalculator)) {
            return Observable.just(Result.success(initialLocation));
        }
        return geocodeInteractor
            .getBestReverseGeocodeResult(latLng)
            .retryWhen(retryBehavior)
            .onErrorReturn(Result::failure);
    }

    private boolean shouldReturnInitialLocation(final LatLng latLng,
                                                @Nullable final NamedTaskLocation initialLocation,
                                                final DistanceCalculator distanceCalculator) {
        if (initialLocation == null) {
            return false;
        }
        if (initialLocation.getDisplayName().equals(currentLocationDisplayString)) {
            return false;
        }
        return distanceCalculator.getDistanceInMeters(initialLocation.getLocation().getLatLng(), latLng) < LOCATION_TOLERANCE_METERS;
    }

    private NamedTaskLocation resolveFailedGeocode(final Result<NamedTaskLocation> geocodedLocation,
                                                   final LatLng latLng) {
        if (geocodedLocation.isFailure()) {
            return new NamedTaskLocation(UNKNOWN_LOCATION_DISPLAY, new TaskLocation(latLng));
        }
        return geocodedLocation.get();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }
}
