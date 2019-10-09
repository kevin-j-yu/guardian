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

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.common.utils.Markers;
import ai.rideos.android.common.utils.Paths;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan.Action;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultWaitingForPickupViewModelTest {
    private static final TripResourceInfo TRIP_RESOURCE = new TripResourceInfo(3, "Robby Rider");
    private static final LatLng PICKUP_LOCATION = new LatLng(1, 1);
    private static final String USER_ID = "user-1";

    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(new LatLng(2, 2), 1);
    private static final int DRAWABLE_PIN = 1;

    private DefaultWaitingForPickupViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;

    @Before
    public void setUp() {
        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getDrawableId(Mockito.anyInt())).thenReturn(DRAWABLE_PIN);

        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(Observable.just(CURRENT_LOCATION));

        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);

        viewModelUnderTest = new DefaultWaitingForPickupViewModel(
            vehicleInteractor,
            Mockito.mock(GeocodeInteractor.class),
            user,
            new Waypoint(
                "trip-1",
                Collections.singletonList("step-1"),
                new Action(PICKUP_LOCATION, ActionType.LOAD_RESOURCE, TRIP_RESOURCE)
            ),
            resourceProvider,
            deviceLocator,
            Mockito.mock(UserStorageReader.class),
            Mockito.mock(UserStorageWriter.class),
            Mockito.mock(WaitingForPickupListener.class),
            new TrampolineSchedulerProvider()
        );
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

    @Test
    public void testConfirmingPickupUpdatesProgress() {
        Mockito.when(vehicleInteractor.finishSteps(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(Completable.complete());
        final TestObserver<ProgressState> progressObserver = viewModelUnderTest.getConfirmingPickupProgress().test();
        progressObserver.assertValueCount(1).assertValueAt(0, ProgressState.IDLE);

        viewModelUnderTest.confirmPickup();

        progressObserver.assertValueCount(3)
            .assertValueAt(1, ProgressState.LOADING)
            .assertValueAt(2, ProgressState.SUCCEEDED);
        Mockito.verify(vehicleInteractor).finishSteps(USER_ID, "trip-1", Collections.singletonList("step-1"));
    }
}
