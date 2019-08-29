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
package ai.rideos.android.driver_app.online.driving.confirming_arrival;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
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
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class DefaultConfirmingArrivalViewModel implements ConfirmingArrivalViewModel {
    private static final int RETRY_COUNT = 2;
    private static final int POLL_INTERVAL_MILLIS = 1000;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final GeocodeInteractor geocodeInteractor;
    private final SchedulerProvider schedulerProvider;
    private final LatLng destination;
    private final int drawableDestinationPin;
    private final ResourceProvider resourceProvider;
    private final BehaviorSubject<LocationAndHeading> currentLocation = BehaviorSubject.create();

    public DefaultConfirmingArrivalViewModel(final GeocodeInteractor geocodeInteractor,
                                             final LatLng destination,
                                             final int drawableDestinationPin,
                                             final DeviceLocator deviceLocator,
                                             final ResourceProvider resourceProvider) {
        this(
            geocodeInteractor,
            destination,
            drawableDestinationPin,
            deviceLocator,
            resourceProvider,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultConfirmingArrivalViewModel(final GeocodeInteractor geocodeInteractor,
                                             final LatLng destination,
                                             final int drawableDestinationPin,
                                             final DeviceLocator deviceLocator,
                                             final ResourceProvider resourceProvider,
                                             final SchedulerProvider schedulerProvider) {
        this.geocodeInteractor = geocodeInteractor;
        this.schedulerProvider = schedulerProvider;
        this.destination = destination;
        this.drawableDestinationPin = drawableDestinationPin;
        this.resourceProvider = resourceProvider;

        compositeDisposable.add(
            deviceLocator.observeCurrentLocation(POLL_INTERVAL_MILLIS).subscribe(currentLocation::onNext)
        );
    }

    @Override
    public Observable<String> getArrivalDetailText() {
        return geocodeInteractor.getBestReverseGeocodeResult(destination)
            .observeOn(schedulerProvider.computation())
            .retry(RETRY_COUNT)
            .doOnError(error -> Timber.e(error, "Failed to geocode location"))
            .onErrorReturn(Result::failure)
            .map(result -> {
                if (result.isFailure()) {
                    return "";
                }
                return result.get().getDisplayName();
            });
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(false, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return currentLocation.observeOn(schedulerProvider.computation())
            .map(location -> {
                final LatLngBounds bounds = Paths.getBoundsForPath(Arrays.asList(
                    location.getLatLng(),
                    destination
                ));
                return CameraUpdate.fitToBounds(bounds);
            });
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return currentLocation.observeOn(schedulerProvider.computation())
            .map(location -> {
                final Map<String, DrawableMarker> markers = new HashMap<>();
                markers.put(
                    "destination",
                    new DrawableMarker(destination, 0, drawableDestinationPin, Anchor.BOTTOM)
                );
                markers.put(
                    Markers.VEHICLE_KEY,
                    Markers.getVehicleMarker(location.getLatLng(), location.getHeading(), resourceProvider)
                );
                return markers;
            });
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return Observable.just(Collections.emptyList());
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }
}
