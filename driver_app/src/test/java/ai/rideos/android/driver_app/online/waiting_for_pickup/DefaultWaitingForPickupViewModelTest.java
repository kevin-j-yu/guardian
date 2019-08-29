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
package ai.rideos.android.driver_app.online.waiting_for_pickup;

import static junit.framework.TestCase.assertEquals;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.model.TripResourceInfo;
import io.reactivex.Observable;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultWaitingForPickupViewModelTest {
    private static final TripResourceInfo TRIP_RESOURCE = new TripResourceInfo(3, "Robby Rider");
    private static final LatLng PICKUP_LOCATION = new LatLng(1, 1);
    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(new LatLng(2, 2), 1);
    private static final int DRAWABLE_PIN = 1;

    private DefaultWaitingForPickupViewModel viewModelUnderTest;
    private ResourceProvider resourceProvider;

    @Before
    public void setUp() {
        setUpWithTripResource(TRIP_RESOURCE);
    }

    private void setUpWithTripResource(final TripResourceInfo tripResourceInfo) {
        resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getDrawableId(Mockito.anyInt())).thenReturn(DRAWABLE_PIN);

        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(Observable.just(CURRENT_LOCATION));

        viewModelUnderTest = new DefaultWaitingForPickupViewModel(
            tripResourceInfo,
            PICKUP_LOCATION,
            resourceProvider,
            deviceLocator,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void getPassengerTextWithSingleRider() {
        setUpWithTripResource(new TripResourceInfo(1, "Single Rider"));
        Mockito.when(resourceProvider.getString(Mockito.eq(R.string.waiting_for_pickup_title_format), Mockito.anyString()))
            .thenAnswer(invocation -> invocation.getArguments()[1]);

        assertEquals("Single Rider", viewModelUnderTest.getPassengersToPickupText());
    }

    @Test
    public void getPassengerTextWithMultipleRiders() {
        setUpWithTripResource(new TripResourceInfo(4, "Multi Rider"));
        Mockito.when(resourceProvider.getString(Mockito.eq(R.string.waiting_for_pickup_title_format), Mockito.anyString()))
            .thenAnswer(invocation -> invocation.getArguments()[1]);

        assertEquals("Multi Rider + 3", viewModelUnderTest.getPassengersToPickupText());
    }

    @Test
    public void testCameraUpdatesFollowCurrentLocationAndPickupLocation() {
        viewModelUnderTest.getCameraUpdates().test()
            .assertValueCount(1)
            .assertValueAt(0, CameraUpdate.fitToBounds(Paths.getBoundsForPath(Arrays.asList(
                CURRENT_LOCATION.getLatLng(),
                PICKUP_LOCATION
            ))));
    }

    @Test
    public void testGetMarkersReturnsPickupAndCurrentLocation() {
        viewModelUnderTest.getMarkers().test()
            .assertValueCount(1)
            .assertValueAt(0, map -> map.get(Markers.PICKUP_MARKER_KEY).getPosition().equals(PICKUP_LOCATION)
                && map.get(Markers.VEHICLE_KEY).getPosition().equals(CURRENT_LOCATION.getLatLng())
            );
    }
}
