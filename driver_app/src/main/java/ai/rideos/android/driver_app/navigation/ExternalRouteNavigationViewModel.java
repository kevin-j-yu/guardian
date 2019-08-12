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
package ai.rideos.android.driver_app.navigation;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.viewmodel.ViewModel;
import io.reactivex.Observable;
import java.util.List;

/**
 * ExternalRouteNavigationViewModel provides a way of routing to a destination external from a navigation view.
 * It provides an observable route that can change if a new destination is set or the navigation went off route.
 */
public interface ExternalRouteNavigationViewModel extends ViewModel {
    /**
     * Set the destination of the navigation. This value can be reused if `didGoOffRoute` is called. This method can
     * be called again to update the destination.
     * @param destination - coordinates to route to.
     */
    void setDestination(final LatLng destination);

    /**
     * Notify the view model that the navigation went off the route, prompting a re-route.
     */
    void didGoOffRoute(final LocationAndHeading fromLocation);

    /**
     * Get the route to display in navigation.
     * @return list of coordinates representing the route.
     */
    Observable<Result<List<LatLng>>> getRoute();
}
