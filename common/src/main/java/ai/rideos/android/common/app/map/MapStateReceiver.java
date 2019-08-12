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

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.view.ViewMargins;
import java.util.List;
import java.util.Map;

public interface MapStateReceiver {
    interface MapCenterListener {
        MapCenterListener NOOP = move -> {};

        default void mapCenterStartedMoving() {
        }

        void mapCenterDidMove(final LatLng move);
    }

    /**
     * Set common map view settings like if current location is shown and if a center pin is shown
     * @param mapSettings
     */
    void setMapSettings(final MapSettings mapSettings);

    /**
     * Move the camera to a new location.
     * @param cameraUpdate - how to update the camera (e.g. zoom to point, fit to bounds)
     * @param force - if true, forces the camera to move. If false, update is ignored when user is dragging
     */
    void moveCamera(final CameraUpdate cameraUpdate, final boolean force);

    /**
     * Show markers on the map at given locations. To update markers, use the same id and call this method. To delete
     * markers, call this method and exclude the ids.
     * @param markers - markers to set on the map. New markers will be added, existing markers will be updated, and
     *                missing markers will be deleted
     */
    void showMarkers(final Map<String, DrawableMarker> markers);

    /**
     * Show paths on the map. When this is called, any shown path will be removed.
     * @param paths - paths to show
     */
    void showPaths(final List<DrawablePath> paths);

    /**
     * Listen to the movement of the map after such events like a user dragging the screen
     * @param listener - called when movement finishes occurring
     */
    void setMapCenterListener(final MapCenterListener listener);

    void setMapMargins(final ViewMargins mapMargins);

}
