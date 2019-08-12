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
package ai.rideos.android.google.interactors;

import ai.rideos.android.common.interactors.LocationAutocompleteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import android.content.Context;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Place.Field;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GooglePlacesAutocompleteInteractor uses the Places SDK to return autocomplete results when searching for a location.
 * Note: in order for this to be used, the Places SDK needs to be initialized somewhere in the code, like in a top
 * level activity, with `Places.initialize(context, apiKey)`.
 */
public class GooglePlacesAutocompleteInteractor implements LocationAutocompleteInteractor {
    private final PlacesClient placesClient;
    private final AutocompleteSessionToken sessionToken = AutocompleteSessionToken.newInstance();
    private final SchedulerProvider schedulerProvider;

    public GooglePlacesAutocompleteInteractor(final Context context) {
        this(Places.createClient(context), new DefaultSchedulerProvider());
    }

    public GooglePlacesAutocompleteInteractor(final PlacesClient placesClient,
                                              final SchedulerProvider schedulerProvider) {
        this.placesClient = placesClient;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<List<LocationAutocompleteResult>> getAutocompleteResults(final String searchText,
                                                                               final LatLngBounds bounds) {
        final RectangularBounds rectangularBounds = RectangularBounds.newInstance(
            Locations.toGoogleLatLng(bounds.getSouthwestCorner()),
            Locations.toGoogleLatLng(bounds.getNortheastCorner())
        );
        final FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
            .setLocationBias(rectangularBounds)
            .setQuery(searchText)
            .setSessionToken(sessionToken)
            .build();
        return Observable.<List<AutocompletePrediction>>create(emitter ->
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(result -> {
                    emitter.onNext(result.getAutocompletePredictions());
                    emitter.onComplete();
                })
                .addOnFailureListener(emitter::onError)
        )
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.computation())
            .map(GooglePlacesAutocompleteInteractor::convertAutocompletePredictionsToResults);
    }

    @Override
    public Observable<NamedTaskLocation> getLocationFromAutocompleteResult(final LocationAutocompleteResult result) {
        final FetchPlaceRequest request = FetchPlaceRequest.newInstance(
            result.getId(),
            // Need to return coordinates, address, and the name of the place
            Arrays.asList(Field.LAT_LNG, Field.ADDRESS, Field.NAME)
        );
        return Observable.<Place>create(emitter ->
            placesClient.fetchPlace(request)
                .addOnSuccessListener(placeResult -> {
                    emitter.onNext(placeResult.getPlace());
                    emitter.onComplete();
                })
                .addOnFailureListener(emitter::onError)
        )
            .subscribeOn(schedulerProvider.io())
            .map(GooglePlacesAutocompleteInteractor::convertPlaceToNamedTaskLocation);
    }

    private static List<LocationAutocompleteResult> convertAutocompletePredictionsToResults(
        final List<AutocompletePrediction> predictions) {

        return predictions.stream()
            .map(prediction -> new LocationAutocompleteResult(
                prediction.getPrimaryText(null).toString(),
                prediction.getSecondaryText(null).toString(),
                prediction.getPlaceId()
            ))
            .collect(Collectors.toList());
    }

    private static NamedTaskLocation convertPlaceToNamedTaskLocation(final Place place) {
        // Prefer to use the name of the place (like the business name) and default to the address if not available
        final LatLng latLng = Locations.fromGoogleLatLng(place.getLatLng());
        if (place.getName() != null) {
            return new NamedTaskLocation(place.getName(), latLng);
        } else {
            return new NamedTaskLocation(place.getAddress(), latLng);
        }
    }
}
