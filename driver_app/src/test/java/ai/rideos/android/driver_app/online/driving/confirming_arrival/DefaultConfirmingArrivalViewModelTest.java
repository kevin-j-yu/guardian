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

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.VehiclePlan.Action;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultConfirmingArrivalViewModelTest {
    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(new LatLng(0, 0), 1);
    private static final LatLng DESTINATION = new LatLng(1, 1);
    private static final Waypoint WAYPOINT = new Waypoint(
        "trip-1",
        Collections.singletonList("step-1"),
        new Action(DESTINATION, ActionType.DRIVE_TO_PICKUP, null)
    );
    private static final String USER_ID = "user-1";
    private static final int DESTINATION_PIN = 1;
    private static final int VEHICLE_PIN = 2;
    private static final int PASSENGER_TEMPLATE = 3;

    private DefaultConfirmingArrivalViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;
    private ResourceProvider resourceProvider;

    @Before
    public void setUp() {
        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(Observable.just(CURRENT_LOCATION));
        resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getDrawableId(Mockito.anyInt()))
            .thenReturn(VEHICLE_PIN);
        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);
        viewModelUnderTest = new DefaultConfirmingArrivalViewModel(
            Mockito.mock(GeocodeInteractor.class),
            vehicleInteractor,
            user,
            deviceLocator,
            resourceProvider,
            WAYPOINT,
            DESTINATION_PIN,
            PASSENGER_TEMPLATE,
            new TrampolineSchedulerProvider()
        );
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

    @Test
    public void testConfirmingArrivalUpdatesProgress() {
        Mockito.when(vehicleInteractor.finishSteps(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(Completable.complete());
        final TestObserver<ProgressState> progressObserver = viewModelUnderTest.getConfirmingArrivalProgress().test();
        progressObserver.assertValueCount(1).assertValueAt(0, ProgressState.IDLE);

        viewModelUnderTest.confirmArrival();

        progressObserver.assertValueCount(3)
            .assertValueAt(1, ProgressState.LOADING)
            .assertValueAt(2, ProgressState.SUCCEEDED);
        Mockito.verify(vehicleInteractor).finishSteps(USER_ID, WAYPOINT.getTaskId(), WAYPOINT.getStepIds());
    }
}
