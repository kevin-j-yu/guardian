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
package ai.rideos.android.interactors;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.google.interactors.GooglePlacesAutocompleteInteractor;
import android.text.SpannableString;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class GooglePlacesAutocompleteInteractorTest {
    private static final LatLng LAT_LNG = new LatLng(0, 1);
    private PlacesClient client;
    private GooglePlacesAutocompleteInteractor interactorUnderTest;

    @Before
    public void setUp() {
        client = Mockito.mock(PlacesClient.class);
        interactorUnderTest = new GooglePlacesAutocompleteInteractor(client, new TrampolineSchedulerProvider());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAutocompleteResultsOnClientSuccess() {
        final ArgumentCaptor<OnSuccessListener> onSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        final Task<FindAutocompletePredictionsResponse> mockTask =
            (Task<FindAutocompletePredictionsResponse>) Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnSuccessListener(onSuccessCaptor.capture())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(Mockito.any())).thenReturn(mockTask);

        Mockito.when(client.findAutocompletePredictions(Mockito.any())).thenReturn(mockTask);

        final FindAutocompletePredictionsResponse response = Mockito.mock(FindAutocompletePredictionsResponse.class);
        final AutocompletePrediction prediction = Mockito.mock(AutocompletePrediction.class);

        final SpannableString primaryName = Mockito.mock(SpannableString.class);
        Mockito.when(primaryName.toString()).thenReturn("primary-name");
        final SpannableString secondaryName = Mockito.mock(SpannableString.class);
        Mockito.when(secondaryName.toString()).thenReturn("secondary-name");

        Mockito.when(prediction.getPrimaryText(Mockito.any())).thenReturn(primaryName);
        Mockito.when(prediction.getSecondaryText(Mockito.any())).thenReturn(secondaryName);
        Mockito.when(prediction.getPlaceId()).thenReturn("id-1");
        Mockito.when(response.getAutocompletePredictions()).thenReturn(Collections.singletonList(prediction));

        final TestObserver<List<LocationAutocompleteResult>> testObserver = interactorUnderTest
            .getAutocompleteResults("test", new LatLngBounds(LAT_LNG, LAT_LNG))
            .subscribeOn(Schedulers.trampoline())
            .test();

        onSuccessCaptor.getValue().onSuccess(response);
        testObserver.assertValueCount(1)
            .assertValueAt(0, list ->
                list.size() == 1 &&
                    list.get(0).equals(new LocationAutocompleteResult("primary-name", "secondary-name", "id-1"))
            )
            .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testObserveAutocompleteErrorWhenClientFails() {
        final ArgumentCaptor<OnFailureListener> onFailureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        final Task<FindAutocompletePredictionsResponse> mockTask =
            (Task<FindAutocompletePredictionsResponse>) Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnSuccessListener(Mockito.any())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(onFailureCaptor.capture())).thenReturn(mockTask);

        Mockito.when(client.findAutocompletePredictions(Mockito.any())).thenReturn(mockTask);

        final TestObserver<List<LocationAutocompleteResult>> testObserver = interactorUnderTest
            .getAutocompleteResults("test", new LatLngBounds(LAT_LNG, LAT_LNG))
            .subscribeOn(Schedulers.trampoline())
            .test();

        final IOException ioException = new IOException("test");
        onFailureCaptor.getValue().onFailure(ioException);
        testObserver.assertValueCount(0)
            .assertError(ioException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLocationFromAutocompleteResultOnClientSuccess() {
        final ArgumentCaptor<OnSuccessListener> onSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        final Task<FetchPlaceResponse> mockTask = (Task<FetchPlaceResponse>) Mockito.mock(Task.class);
        Mockito.when(mockTask.addOnSuccessListener(onSuccessCaptor.capture())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(Mockito.any())).thenReturn(mockTask);
        Mockito.when(client.fetchPlace(Mockito.any())).thenReturn(mockTask);

        final FetchPlaceResponse response = Mockito.mock(FetchPlaceResponse.class);
        final Place place = Mockito.mock(Place.class);
        Mockito.when(place.getName()).thenReturn("place-name");
        Mockito.when(place.getLatLng()).thenReturn(Locations.toGoogleLatLng(LAT_LNG));
        Mockito.when(response.getPlace()).thenReturn(place);

        final TestObserver<NamedTaskLocation> testObserver = interactorUnderTest
            .getLocationFromAutocompleteResult(new LocationAutocompleteResult("place", "id-1"))
            .subscribeOn(Schedulers.trampoline())
            .test();

        onSuccessCaptor.getValue().onSuccess(response);
        testObserver.assertValueCount(1)
            .assertValueAt(0, new NamedTaskLocation("place-name", LAT_LNG))
            .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testObserveGetLocationErrorWhenClientFails() {
        final ArgumentCaptor<OnFailureListener> onFailureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        final Task<FetchPlaceResponse> mockTask = (Task<FetchPlaceResponse>) Mockito.mock(Task.class);
        Mockito.when(mockTask.addOnSuccessListener(Mockito.any())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(onFailureCaptor.capture())).thenReturn(mockTask);
        Mockito.when(client.fetchPlace(Mockito.any())).thenReturn(mockTask);

        final FetchPlaceResponse response = Mockito.mock(FetchPlaceResponse.class);
        final Place place = Mockito.mock(Place.class);
        Mockito.when(place.getName()).thenReturn("place-name");
        Mockito.when(place.getLatLng()).thenReturn(Locations.toGoogleLatLng(LAT_LNG));
        Mockito.when(response.getPlace()).thenReturn(place);

        final TestObserver<NamedTaskLocation> testObserver = interactorUnderTest
            .getLocationFromAutocompleteResult(new LocationAutocompleteResult("place", "id-1"))
            .subscribeOn(Schedulers.trampoline())
            .test();

        final IOException ioException = new IOException("test");
        onFailureCaptor.getValue().onFailure(ioException);
        testObserver.assertValueCount(0)
            .assertError(ioException);
    }
}
