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
package ai.rideos.android.rider_app.pre_trip.confirm_trip;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.viewmodel.ViewModel;
import ai.rideos.android.common.viewmodel.map.MapStateProvider;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.model.RouteTimeDistanceDisplay;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * TODO we should probably not bloat this view model with vehicle selection features. When we move onto an improved
 * modular architecture, we should remove the vehicle selection methods and place them into another step in the pre-trip
 * flow
 */
public interface ConfirmTripViewModel extends MapStateProvider, ViewModel {
    enum FetchingRouteStatus {
        IDLE,
        IN_PROGRESS,
        ERROR
    }

    void confirmTrip(final int passengerCount);

    void confirmTripWithoutSeats();

    void setOriginAndDestination(final LatLng origin, final LatLng destination);

    Observable<RouteTimeDistanceDisplay> getRouteInformation();

    Observable<ProgressState> getFetchingRouteProgress();

    Single<Pair<Integer, Integer>> getPassengerCountBounds();

    boolean isSeatSelectionDisabled();
}
