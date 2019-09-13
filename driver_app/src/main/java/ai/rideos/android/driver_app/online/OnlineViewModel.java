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
package ai.rideos.android.driver_app.online;

import ai.rideos.android.common.viewmodel.ViewModel;
import ai.rideos.android.driver_app.online.driving.DrivingListener;
import ai.rideos.android.driver_app.online.idle.GoOfflineListener;
import ai.rideos.android.driver_app.online.trip_details.TripDetailsListener;
import ai.rideos.android.driver_app.online.waiting_for_pickup.WaitingForPickupListener;
import ai.rideos.android.model.OnlineViewState;
import io.reactivex.Observable;

public interface OnlineViewModel extends ViewModel, GoOfflineListener, DrivingListener, WaitingForPickupListener,
    TripDetailsListener {
    Observable<OnlineViewState> getOnlineViewState();
}
