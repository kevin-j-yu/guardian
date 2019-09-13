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
package ai.rideos.android.driver_app.online.trip_details;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.driver_app.online.trip_details.TripDetail.ActionToPerform;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Action;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import io.reactivex.Completable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultTripDetailsViewModelTest {
    private static final String VEHICLE_ID = "vehicle-1";
    private static final TripResourceInfo RESOURCE = new TripResourceInfo(4, "Robby Rider");
    private static final LatLng DESTINATION = new LatLng(0, 1);

    private DefaultTripDetailsViewModel viewModelUnderTest;
    private DriverVehicleInteractor driverVehicleInteractor;

    private void setUpWithPlan(final VehiclePlan vehiclePlan) {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(VEHICLE_ID);

        driverVehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);

        viewModelUnderTest = new DefaultTripDetailsViewModel(
            driverVehicleInteractor,
            user,
            vehiclePlan
        );
    }

    @Test
    public void testPerformRejectActionOnTripDetail() {
        final TripDetail tripDetail = new TripDetail(
            new Waypoint(
                "trip-1",
                Collections.singletonList("step-1"),
                new Action(null, ActionType.DRIVE_TO_PICKUP, null)
            ),
            ActionToPerform.REJECT_TRIP,
            "Robby Rider",
            "123-456-7890"
        );
        setUpWithPlan(new VehiclePlan(Collections.emptyList()));

        Mockito.when(driverVehicleInteractor.rejectTrip(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Completable.complete());

        viewModelUnderTest.performActionOnTrip(tripDetail);

        Mockito.verify(driverVehicleInteractor).rejectTrip(VEHICLE_ID, "trip-1");
    }

    @Test
    public void testPerformEndTripActionOnTripDetail() {
        final TripDetail tripDetail = new TripDetail(
            new Waypoint(
                "trip-1",
                Collections.singletonList("step-1"),
                new Action(DESTINATION, ActionType.DRIVE_TO_DROP_OFF, RESOURCE)
            ),
            ActionToPerform.END_TRIP,
            "Robby Rider",
            "123-456-7890"
        );
        setUpWithPlan(new VehiclePlan(Collections.emptyList()));

        Mockito.when(driverVehicleInteractor.finishSteps(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(Completable.complete());

        viewModelUnderTest.performActionOnTrip(tripDetail);

        Mockito.verify(driverVehicleInteractor).finishSteps(VEHICLE_ID, "trip-1", Collections.singletonList("step-1"));
    }

    @Test
    public void testGetTripDetailsSetsCorrectActionWhenNextWaypointIsDropOff() {
        final Waypoint dropOffWaypoint = new Waypoint(
            "trip-1",
            Collections.singletonList("step-1"),
            new Action(DESTINATION, ActionType.DRIVE_TO_DROP_OFF, RESOURCE)
        );
        setUpWithPlan(new VehiclePlan(Collections.singletonList(dropOffWaypoint)));

        final TripDetail expectedTripDetail = new TripDetail(
            dropOffWaypoint,
            ActionToPerform.END_TRIP,
            RESOURCE.getNameOfTripRequester(),
            null
        );

        viewModelUnderTest.getTripDetails().test()
            .assertValueAt(0, Collections.singletonList(expectedTripDetail));
    }

    @Test
    public void testGetTripDetailsSetsCorrectActionWhenNextWaypointIsPickup() {
        final Waypoint pickupWaypoint = new Waypoint(
            "trip-1",
            Collections.singletonList("step-1"),
            new Action(DESTINATION, ActionType.DRIVE_TO_PICKUP, RESOURCE)
        );
        setUpWithPlan(new VehiclePlan(Collections.singletonList(pickupWaypoint)));

        final TripDetail expectedTripDetail = new TripDetail(
            pickupWaypoint,
            ActionToPerform.REJECT_TRIP,
            RESOURCE.getNameOfTripRequester(),
            null
        );

        viewModelUnderTest.getTripDetails().test()
            .assertValueAt(0, Collections.singletonList(expectedTripDetail));
    }

    @Test
    public void testGetTripDetailsTakesFirstWaypointForEachTrip() {
        final Waypoint trip1pickup = new Waypoint(
            "trip-1",
            Collections.singletonList("step-1"),
            new Action(DESTINATION, ActionType.DRIVE_TO_PICKUP, RESOURCE)
        );
        final Waypoint trip1load = new Waypoint(
            "trip-1",
            Collections.singletonList("step-2"),
            new Action(DESTINATION, ActionType.LOAD_RESOURCE, RESOURCE)
        );
        final Waypoint trip1dropOff = new Waypoint(
            "trip-1",
            Collections.singletonList("step-3"),
            new Action(DESTINATION, ActionType.DRIVE_TO_DROP_OFF, RESOURCE)
        );
        final Waypoint trip2dropOff = new Waypoint(
            "trip-2",
            Collections.singletonList("step-1"),
            new Action(DESTINATION, ActionType.DRIVE_TO_DROP_OFF, RESOURCE)
        );

        setUpWithPlan(new VehiclePlan(Arrays.asList(
            trip1pickup,
            trip1load,
            trip2dropOff,
            trip1dropOff
        )));

        final List<TripDetail> expectedTripDetailsInOrder = Arrays.asList(
            new TripDetail(
                trip1pickup,
                ActionToPerform.REJECT_TRIP,
                RESOURCE.getNameOfTripRequester(),
                null
            ),
            new TripDetail(
                trip2dropOff,
                ActionToPerform.END_TRIP,
                RESOURCE.getNameOfTripRequester(),
                null
            )
        );
        viewModelUnderTest.getTripDetails().test()
            .assertValueAt(0, expectedTripDetailsInOrder);
    }
}
