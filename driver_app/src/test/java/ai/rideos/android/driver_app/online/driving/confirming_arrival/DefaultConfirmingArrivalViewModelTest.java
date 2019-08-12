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
package ai.rideos.android.driver_app.online.driving.confirming_arrival;

import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultConfirmingArrivalViewModelTest {
    private final LatLng DESTINATION = new LatLng(0, 1);

    private DefaultConfirmingArrivalViewModel viewModelUnderTest;
    private GeocodeInteractor geocodeInteractor;

    @Before
    public void setUp() {
        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        viewModelUnderTest = new DefaultConfirmingArrivalViewModel(
            geocodeInteractor,
            DESTINATION,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testGetArrivalDetailTestReturnsGeocodedName() {
        final String destinationName = "San Francisco";
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(DESTINATION))
            .thenReturn(Observable.just(Result.success(new NamedTaskLocation(destinationName, DESTINATION))));

        viewModelUnderTest.getArrivalDetailText().test()
            .assertValueCount(1)
            .assertValueAt(0, destinationName);
    }

    @Test
    public void testGeocodeFailureReturnsEmptyDetailString() {
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(DESTINATION))
            .thenReturn(Observable.error(new IOException()));

        viewModelUnderTest.getArrivalDetailText().test()
            .assertValueCount(1)
            .assertValueAt(0, String::isEmpty);
    }

    @Test
    public void testMapStateProviderFunctionsClearMap() {
        viewModelUnderTest.getCameraUpdates().test().assertValueCount(0).assertComplete();
        viewModelUnderTest.getMarkers().test().assertValueAt(0, Map::isEmpty);
        viewModelUnderTest.getPaths().test().assertValueAt(0, List::isEmpty);
    }

}
