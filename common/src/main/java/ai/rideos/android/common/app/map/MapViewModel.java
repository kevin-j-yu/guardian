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
package ai.rideos.android.common.app.map;

import ai.rideos.android.common.app.map.MapStateReceiver.MapCenterListener;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.view.ViewMargins;
import io.reactivex.Observable;
import java.util.List;
import java.util.Map;

public interface MapViewModel {
    /**
     * Notify view model that the user started dragging the map to a different location.
     */
    void mapWasDragged();

    /**
     * Center the map back on the last requested camera update location.
     */
    void reCenterMap();

    /**
     * Get the camera updates that should actually be performed. This is a filtered set of the camera updates requested.
     */
    Observable<CameraUpdate> getCameraUpdatesToPerform();

    /**
     * Observe whether the re-center button should be overlayed on the map.
     */
    Observable<Boolean> shouldAllowReCentering();

    Observable<MapSettings> getMapSettings();

    Observable<Map<String, DrawableMarker>> getMarkers();

    Observable<List<DrawablePath>> getPaths();

    Observable<ViewMargins> getMapMargins();

    Observable<MapCenterListener> getMapCenterListener();
}
