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
package ai.rideos.android.common.interactors;

import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.map.LatLngBounds;
import io.reactivex.Observable;
import java.util.List;

public interface LocationAutocompleteInteractor {
    /**
     * Given some incomplete text, e.g. "221 m" return suggestions based on the text and bounded proximity.
     * @param searchText - query to search for a location
     * @param bounds - bounds to bias the search towards
     * @return list of autocomplete results, or empty if there are no results
     */
    Observable<List<LocationAutocompleteResult>> getAutocompleteResults(final String searchText,
                                                                        final LatLngBounds bounds);

    /**
     * Given an autocomplete result, return the geocoded location that includes a display name and coordinates.
     * @param result - autocomplete result returned from getAutocompleteResults
     * @return geocoded location model
     */
    Observable<NamedTaskLocation> getLocationFromAutocompleteResult(final LocationAutocompleteResult result);
}
