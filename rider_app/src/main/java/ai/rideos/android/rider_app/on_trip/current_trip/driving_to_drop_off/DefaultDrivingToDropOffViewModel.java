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
package ai.rideos.android.rider_app.on_trip.current_trip.driving_to_drop_off;

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
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultDrivingToDropOffViewModel implements DrivingToDropOffViewModel {
    private final BehaviorSubject<TripStateModel> passengerStateSubject = BehaviorSubject.create();
    private final ResourceProvider resourceProvider;
    private final DateFormat preferredDateFormat;
    private final SchedulerProvider schedulerProvider;

    public DefaultDrivingToDropOffViewModel(final ResourceProvider resourceProvider,
                                            final DateFormat preferredDateFormat) {
        this(resourceProvider, preferredDateFormat, new DefaultSchedulerProvider());
    }

    public DefaultDrivingToDropOffViewModel(final ResourceProvider resourceProvider,
                                            final DateFormat preferredDateFormat,
                                            final SchedulerProvider schedulerProvider) {
        this.resourceProvider = resourceProvider;
        this.preferredDateFormat = preferredDateFormat;
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
            .map(passengerState -> getEtaText(passengerState.getVehicleRouteInfo().get().getTravelTimeMillis()));
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(false, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .filter(state -> state.getVehicleRouteInfo().isPresent())
            .map(passengerState -> CameraUpdate.fitToBounds(
                Paths.getBoundsForPath(
                    passengerState.getVehicleRouteInfo().get().getRoute(),
                    passengerState.getPassengerDropOffLocation()
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

                markers.put(Markers.DROP_OFF_MARKER_KEY, Markers.getDropOffMarker(
                    passengerState.getPassengerDropOffLocation(),
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

    private String getEtaText(final long vehicleTravelTimeMillis) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, (int) vehicleTravelTimeMillis);
        final String arrivalTime = preferredDateFormat.format(calendar.getTime());
        return resourceProvider.getString(R.string.on_trip_eta_text, arrivalTime);
    }
}
