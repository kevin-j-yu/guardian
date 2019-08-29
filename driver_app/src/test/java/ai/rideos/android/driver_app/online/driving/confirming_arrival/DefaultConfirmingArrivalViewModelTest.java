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

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultConfirmingArrivalViewModelTest {
    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(new LatLng(0, 0), 1);
    private static final LatLng DESTINATION = new LatLng(1, 1);
    private static final int DESTINATION_PIN = 1;
    private static final int VEHICLE_PIN = 2;

    private DefaultConfirmingArrivalViewModel viewModelUnderTest;
    private GeocodeInteractor geocodeInteractor;
    private ResourceProvider resourceProvider;

    @Before
    public void setUp() {
        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(Observable.just(CURRENT_LOCATION));
        resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getDrawableId(Mockito.anyInt()))
            .thenReturn(VEHICLE_PIN);
        viewModelUnderTest = new DefaultConfirmingArrivalViewModel(
            geocodeInteractor,
            DESTINATION,
            DESTINATION_PIN,
            deviceLocator,
            resourceProvider,
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
    public void testCameraUpdatesZoomInOnCurrentLocationAndDestination() {
        viewModelUnderTest.getCameraUpdates().test()
            .assertValueAt(0, CameraUpdate.fitToBounds(
                Paths.getBoundsForPath(Arrays.asList(CURRENT_LOCATION.getLatLng(), DESTINATION))
            ));
    }

    @Test
    public void testGetMarkersReturnsDestinationAndVehicle() {
        final Map<String, DrawableMarker> expectedMarkers = new HashMap<>();
        expectedMarkers.put("destination", new DrawableMarker(DESTINATION, 0, DESTINATION_PIN, Anchor.BOTTOM));
        expectedMarkers.put(
            Markers.VEHICLE_KEY,
            Markers.getVehicleMarker(CURRENT_LOCATION.getLatLng(), CURRENT_LOCATION.getHeading(), resourceProvider)
        );
        viewModelUnderTest.getMarkers().test()
            .assertValueAt(0, map -> map.get("destination").getPosition().equals(DESTINATION)
                && map.get(Markers.VEHICLE_KEY).getPosition().equals(CURRENT_LOCATION.getLatLng())
            );
    }
}
