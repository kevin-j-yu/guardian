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
package ai.rideos.android.driver_app.online.waiting_for_pickup;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.model.TripResourceInfo;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DefaultWaitingForPickupViewModel implements WaitingForPickupViewModel {
    private static final int POLL_INTERVAL_MILLIS = 1000;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final TripResourceInfo tripResourceInfo;
    private final LatLng pickupLocation;
    private final ResourceProvider resourceProvider;
    private final SchedulerProvider schedulerProvider;
    private final BehaviorSubject<LocationAndHeading> currentLocation = BehaviorSubject.create();

    public DefaultWaitingForPickupViewModel(final TripResourceInfo tripResourceInfo,
                                            final LatLng pickupLocation,
                                            final ResourceProvider resourceProvider,
                                            final DeviceLocator deviceLocator) {
        this(tripResourceInfo, pickupLocation, resourceProvider, deviceLocator, new DefaultSchedulerProvider());
    }

    public DefaultWaitingForPickupViewModel(final TripResourceInfo tripResourceInfo,
                                            final LatLng pickupLocation,
                                            final ResourceProvider resourceProvider,
                                            final DeviceLocator deviceLocator,
                                            final SchedulerProvider schedulerProvider) {
        this.tripResourceInfo = tripResourceInfo;
        this.pickupLocation = pickupLocation;
        this.resourceProvider = resourceProvider;
        this.schedulerProvider = schedulerProvider;
        compositeDisposable.add(
            deviceLocator.observeCurrentLocation(POLL_INTERVAL_MILLIS).subscribe(currentLocation::onNext)
        );
    }

    @Override
    public String getPassengersToPickupText() {
        final String passengersToPickupText;
        if (tripResourceInfo.getNumPassengers() > 1) {
            final int numberOfRidersExcludingRequester = tripResourceInfo.getNumPassengers() - 1;
            passengersToPickupText = String.format(
                "%s + %s",
                tripResourceInfo.getNameOfTripRequester(),
                numberOfRidersExcludingRequester
            );
        } else {
            passengersToPickupText = tripResourceInfo.getNameOfTripRequester();
        }

        return resourceProvider.getString(R.string.waiting_for_pickup_title_format, passengersToPickupText);
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
                    pickupLocation
                ));
                return CameraUpdate.fitToBounds(bounds);
            });
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return currentLocation.observeOn(schedulerProvider.computation())
            .map(location -> {
                final Map<String, DrawableMarker> markers = new HashMap<>();
                markers.put(Markers.PICKUP_MARKER_KEY, Markers.getPickupMarker(pickupLocation, resourceProvider));
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
