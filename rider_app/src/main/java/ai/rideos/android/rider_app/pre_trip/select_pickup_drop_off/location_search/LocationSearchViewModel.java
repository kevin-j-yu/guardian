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
package ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.location_search;

import ai.rideos.android.common.viewmodel.ViewModel;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.LocationSearchInitialState;
import ai.rideos.android.model.LocationSearchOptionModel;
import io.reactivex.Observable;
import java.util.List;

/**
 * LocationSearchViewModel represents the view model for selecting pickup and drop-off locations for a trip using a
 * shared list of geocoded results.
 */
public interface LocationSearchViewModel extends ViewModel {

    // Set input text in pickup field
    void setPickupInput(final String input);

    // Set input text in drop-off field
    void setDropOffInput(final String input);

    // Set the currently focused text field
    void setFocus(final LocationSearchFocusType inputType);

    // Call when an option in a list view is selected
    void makeSelection(final LocationSearchOptionModel selectedLocation);

    void done();

    Observable<LocationSearchInitialState> getInitialState();

    Observable<Boolean> isDoneActionEnabled();

    // Get current list of geocoded options based on current text input
    Observable<List<LocationSearchOptionModel>> getLocationOptions();

    Observable<String> getSelectedPickup();

    Observable<String> getSelectedDropOff();

    Observable<Boolean> canClearPickup();

    Observable<Boolean> canClearDropOff();
}
