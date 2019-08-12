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
package ai.rideos.android.rider_app.on_trip.current_trip;

import ai.rideos.android.common.viewmodel.ViewModel;
import ai.rideos.android.model.FollowTripDisplayState;
import ai.rideos.android.rider_app.on_trip.current_trip.cancelled.CancelledListener;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_drop_off.DrivingToDropOffListener;
import ai.rideos.android.rider_app.on_trip.current_trip.driving_to_pickup.DrivingToPickupListener;
import ai.rideos.android.rider_app.on_trip.current_trip.trip_completed.TripCompletedListener;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_assignment.WaitingForAssignmentListener;
import ai.rideos.android.rider_app.on_trip.current_trip.waiting_for_pickup.WaitingForPickupListener;
import io.reactivex.Observable;

public interface CurrentTripViewModel extends ViewModel, WaitingForAssignmentListener, WaitingForPickupListener,
    DrivingToPickupListener, DrivingToDropOffListener, TripCompletedListener, CancelledListener {
    void initialize(final String tripId);

    Observable<FollowTripDisplayState> getDisplay();
}
