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
package ai.rideos.android.common.model;

public class LocationAutocompleteResult {
    private final String primaryName;
    private final String secondaryName;
    private final String id;

    public LocationAutocompleteResult(final String primaryName, final String id) {
        this(primaryName, "", id);
    }

    public LocationAutocompleteResult(final String primaryName, final String secondaryName, final String id) {
        this.primaryName = primaryName;
        this.secondaryName = secondaryName;
        this.id = id;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public String getSecondaryName() {
        return secondaryName;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LocationAutocompleteResult)) {
            return false;
        }
        final LocationAutocompleteResult otherModel = (LocationAutocompleteResult) other;
        return primaryName.equals(otherModel.primaryName)
            && secondaryName.equals(otherModel.secondaryName)
            && id.equals(otherModel.id);
    }
}
