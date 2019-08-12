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
package ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_pickup;

import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.model.TripStateModel;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultWaitingForPickupViewModel implements WaitingForPickupViewModel {
    private static final float ZOOM_LEVEL = 15;

    private final BehaviorSubject<TripStateModel> passengerStateSubject = BehaviorSubject.create();
    private final ResourceProvider resourceProvider;
    private final SchedulerProvider schedulerProvider;

    public DefaultWaitingForPickupViewModel(final ResourceProvider resourceProvider) {
        this(resourceProvider, new DefaultSchedulerProvider());
    }

    public DefaultWaitingForPickupViewModel(final ResourceProvider resourceProvider,
                                            final SchedulerProvider schedulerProvider) {
        this.resourceProvider = resourceProvider;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public void updatePassengerState(final TripStateModel passengerState) {
        passengerStateSubject.onNext(passengerState);
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(true, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .map(passengerState -> CameraUpdate.centerAndZoom(passengerState.getPassengerPickupLocation(), ZOOM_LEVEL));
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return passengerStateSubject.observeOn(schedulerProvider.computation())
            .filter(passengerState -> passengerState.getVehiclePosition().isPresent())
            .map(passengerState -> ImmutableMap.of(
                Markers.VEHICLE_KEY,
                Markers.getVehicleMarker(
                    passengerState.getVehiclePosition().get().getLatLng(),
                    passengerState.getVehiclePosition().get().getHeading(),
                    resourceProvider
                )
            ));
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return Observable.just(Collections.emptyList());
    }
}
