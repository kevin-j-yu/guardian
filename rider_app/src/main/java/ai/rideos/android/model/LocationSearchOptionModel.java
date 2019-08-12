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
package ai.rideos.android.model;

import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.rider_app.R;
import java.util.Objects;
import java.util.Optional;

public class LocationSearchOptionModel {
    public enum OptionType {
        AUTOCOMPLETE_LOCATION,
        CURRENT_LOCATION,
        SELECT_ON_MAP
    }

    private final String primaryName;
    private final String secondaryName;
    private final Integer drawableIcon;
    private final LocationAutocompleteResult autocompleteResult;
    private final OptionType optionType;

    public static LocationSearchOptionModel autocompleteLocation(final LocationAutocompleteResult autocompleteResult) {
        return new LocationSearchOptionModel(
            autocompleteResult.getPrimaryName(),
            autocompleteResult.getSecondaryName(),
            autocompleteResult,
            OptionType.AUTOCOMPLETE_LOCATION,
            null
        );
    }

    public static LocationSearchOptionModel currentLocation(final String primaryName) {
        return new LocationSearchOptionModel(
            primaryName,
            "",
            null,
            OptionType.CURRENT_LOCATION,
            R.drawable.ic_my_location_24dp
        );
    }

    public static LocationSearchOptionModel selectOnMap(final String primaryName) {
        return new LocationSearchOptionModel(
            primaryName,
            "",
            null,
            OptionType.SELECT_ON_MAP,
            R.drawable.ic_set_on_map_24dp
        );
    }

    public static LocationSearchOptionModel historicalSearch(final LocationAutocompleteResult autocompleteResult) {
        return new LocationSearchOptionModel(
            autocompleteResult.getPrimaryName(),
            autocompleteResult.getSecondaryName(),
            autocompleteResult,
            OptionType.AUTOCOMPLETE_LOCATION,
            R.drawable.ic_history_24dp
        );
    }

    private LocationSearchOptionModel(final String primaryName,
                                      final String secondaryName,
                                      final LocationAutocompleteResult autocompleteResult,
                                      final OptionType optionType,
                                      final Integer drawableIcon) {
        this.primaryName = primaryName;
        this.secondaryName = secondaryName;
        this.autocompleteResult = autocompleteResult;
        this.optionType = optionType;
        this.drawableIcon = drawableIcon;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public String getSecondaryName() {
        return secondaryName;
    }

    public LocationAutocompleteResult getAutocompleteResult() {
        return autocompleteResult;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public Optional<Integer> getDrawableIcon() {
        return Optional.ofNullable(drawableIcon);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LocationSearchOptionModel)) {
            return false;
        }
        final LocationSearchOptionModel otherModel = (LocationSearchOptionModel) other;
        return primaryName.equals(otherModel.primaryName)
            && secondaryName.equals(otherModel.secondaryName)
            && Objects.equals(autocompleteResult, otherModel.autocompleteResult)
            && optionType == otherModel.optionType;
    }
}
