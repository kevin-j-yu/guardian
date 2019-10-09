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
package ai.rideos.android.driver_app.online;

import static org.junit.Assert.assertEquals;

import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan.Action;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultOnTripViewModelTest {
    private static final int DETAIL_TEMPLATE = 1;
    private static final LatLng DESTINATION = new LatLng(0, 1);
    private static final String TRIP_ID = "trip-1";
    private static final List<String> STEP_IDS = Collections.singletonList("step-1");
    private static final Waypoint DEFAULT_WAYPOINT = new Waypoint(
        TRIP_ID,
        STEP_IDS,
        new Action(
            DESTINATION,
            ActionType.DRIVE_TO_PICKUP,
            new TripResourceInfo(1, "Single Rider")
        )
    );

    private DefaultOnTripViewModel viewModelUnderTest;
    private GeocodeInteractor geocodeInteractor;

    private void setUpWithWaypoint(final Waypoint waypoint) {
        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getString(Mockito.anyInt(), Mockito.any()))
            .thenAnswer(invocation -> invocation.getArguments()[1]);

        geocodeInteractor = Mockito.mock(GeocodeInteractor.class);
        viewModelUnderTest = new DefaultOnTripViewModel(
            geocodeInteractor,
            waypoint,
            resourceProvider,
            DETAIL_TEMPLATE,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void getPassengerTextWithSingleRider() {
        setUpWithWaypoint(new Waypoint(
            TRIP_ID,
            STEP_IDS,
            new Action(
                DESTINATION,
                ActionType.DRIVE_TO_PICKUP,
                new TripResourceInfo(1, "Single Rider")
            )
        ));

        assertEquals("Single Rider", viewModelUnderTest.getPassengerDetailText());
    }

    @Test
    public void getPassengerTextWithMultipleRiders() {
        setUpWithWaypoint(new Waypoint(
            TRIP_ID,
            STEP_IDS,
            new Action(
                DESTINATION,
                ActionType.DRIVE_TO_PICKUP,
                new TripResourceInfo(2, "Multi Rider")
            )
        ));

        assertEquals("Multi Rider + 1", viewModelUnderTest.getPassengerDetailText());
    }

    @Test
    public void testGetArrivalDetailTestReturnsGeocodedName() {
        setUpWithWaypoint(DEFAULT_WAYPOINT);
        final String destinationName = "San Francisco";
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(DESTINATION))
            .thenReturn(Observable.just(Result.success(new NamedTaskLocation(destinationName, DESTINATION))));

        viewModelUnderTest.getDestinationAddress().test()
            .assertValueCount(1)
            .assertValueAt(0, destinationName);
    }

    @Test
    public void testGeocodeFailureReturnsEmptyDetailString() {
        setUpWithWaypoint(DEFAULT_WAYPOINT);
        Mockito.when(geocodeInteractor.getBestReverseGeocodeResult(DESTINATION))
            .thenReturn(Observable.error(new IOException()));

        viewModelUnderTest.getDestinationAddress().test()
            .assertValueCount(1)
            .assertValueAt(0, String::isEmpty);
    }
}
