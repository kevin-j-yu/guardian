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
package ai.rideos.android.common.viewmodel.map;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FollowCurrentLocationMapStateProvider implements MapStateProvider {
    private static final int POLL_INTERVAL_MILLIS = 2000;
    private static final float ZOOM_LEVEL = 15;

    private final SchedulerProvider schedulerProvider;
    private final int drawableMarkerId;
    private final Observable<LocationAndHeading> observableLocation;

    public FollowCurrentLocationMapStateProvider(final DeviceLocator deviceLocator,
                                                 final int drawableMarkerId) {
        this(deviceLocator, drawableMarkerId, new DefaultSchedulerProvider());
    }

    public FollowCurrentLocationMapStateProvider(final DeviceLocator deviceLocator,
                                                 final int drawableMarkerId,
                                                 final SchedulerProvider schedulerProvider) {
        this.drawableMarkerId = drawableMarkerId;
        this.schedulerProvider = schedulerProvider;
        observableLocation = deviceLocator.observeCurrentLocation(POLL_INTERVAL_MILLIS)
            .observeOn(schedulerProvider.computation());
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(false, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return observableLocation
            .map(locationAndHeading -> CameraUpdate.centerAndZoom(locationAndHeading.getLatLng(), ZOOM_LEVEL));
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return observableLocation
            .map(locationAndHeading -> Collections.singletonMap(
                Markers.VEHICLE_KEY,
                new DrawableMarker(
                    locationAndHeading.getLatLng(),
                    locationAndHeading.getHeading(),
                    drawableMarkerId,
                    Anchor.CENTER
                )
            ));
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return Observable.just(Collections.emptyList());
    }
}
