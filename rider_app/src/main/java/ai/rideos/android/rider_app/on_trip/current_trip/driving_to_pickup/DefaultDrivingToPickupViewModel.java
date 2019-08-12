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
package ai.rideos.android.rider_app.on_trip.current_trip.driving_to_pickup;

import ai.rideos.android.common.model.LatLng;
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
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.rider_app.R;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultDrivingToPickupViewModel implements DrivingToPickupViewModel {
    private final BehaviorSubject<TripStateModel> passengerStateSubject = BehaviorSubject.create();
    private final ResourceProvider resourceProvider;
    private final SchedulerProvider schedulerProvider;

    public DefaultDrivingToPickupViewModel(final ResourceProvider resourceProvider) {
        this(resourceProvider, new DefaultSchedulerProvider());
    }

    public DefaultDrivingToPickupViewModel(final ResourceProvider resourceProvider,
                                           final SchedulerProvider schedulerProvider) {
        this.resourceProvider = resourceProvider;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public void updatePassengerState(final TripStateModel passengerState) {
        passengerStateSubject.onNext(passengerState);
    }

    @Override
    public Observable<String> getRouteDetailText() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .filter(state -> state.getVehicleRouteInfo().isPresent())
            .map(passengerState -> {

                final Optional<String> queuingText = getQueuingText(passengerState.getWaypoints());
                final String travelTimeText = getTravelTimeText(
                    passengerState.getVehicleRouteInfo().get().getTravelTimeMillis()
                );
                if (queuingText.isPresent()) {
                    return resourceProvider.getString(
                        R.string.on_trip_queueing_and_arrival_time,
                        queuingText.get(),
                        travelTimeText
                    );
                } else {
                    return travelTimeText;
                }
            });
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(true, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .filter(state -> state.getVehicleRouteInfo().isPresent())
            .map(passengerState -> CameraUpdate.fitToBounds(
                Paths.getBoundsForPath(
                    passengerState.getVehicleRouteInfo().get().getRoute(),
                    passengerState.getPassengerPickupLocation()
                )
            ));
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .map(passengerState -> {
                final Map<String, DrawableMarker> markers = new HashMap<>();
                if (passengerState.getVehiclePosition().isPresent()) {
                    markers.put(Markers.VEHICLE_KEY, Markers.getVehicleMarker(
                        passengerState.getVehiclePosition().get().getLatLng(),
                        passengerState.getVehiclePosition().get().getHeading(),
                        resourceProvider
                    ));
                }
                markers.put(Markers.PICKUP_MARKER_KEY, Markers.getPickupMarker(
                    passengerState.getPassengerPickupLocation(),
                    resourceProvider
                ));
                markers.putAll(Markers.getWaypointMarkers(passengerState.getWaypoints(), resourceProvider));
                return markers;
            });
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .filter(state -> state.getVehicleRouteInfo().isPresent())
            .map(passengerState -> Collections.singletonList(DrawablePaths.getActivePath(
                passengerState.getVehicleRouteInfo().get().getRoute(),
                resourceProvider
            )));
    }

    private String getTravelTimeText(final long vehicleTravelTimeMillis) {
        final int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(vehicleTravelTimeMillis);
        if (minutes == 0) {
            return resourceProvider.getString(R.string.on_trip_arriving_less_than_minute_text);
        }
        return resourceProvider.getString(R.string.on_trip_arrival_time_text, minutes);
    }

    private Optional<String> getQueuingText(final List<LatLng> waypoints) {
        if (waypoints.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resourceProvider.getQuantityString(
            R.plurals.on_trip_stops_before_rider,
            waypoints.size(),
            waypoints.size()
        ));
    }
}
