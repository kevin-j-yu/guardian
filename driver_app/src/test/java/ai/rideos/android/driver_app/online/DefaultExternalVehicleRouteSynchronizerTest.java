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

import static org.mockito.Matchers.any;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehicleDisplayRouteLeg;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Action;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.core.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultExternalVehicleRouteSynchronizerTest {
    private static final String USER_ID = "user";
    private static final LocationAndHeading CURRENT_LOCATION = new LocationAndHeading(new LatLng(1, 2), 1.0f);
    private static final TripResourceInfo TRIP_INFO = new TripResourceInfo(1);

    private DefaultExternalVehicleRouteSynchronizer synchronizer;
    private DriverVehicleInteractor vehicleInteractor;
    private RouteInteractor routeInteractor;

    @Before
    public void setUp() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);

        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);
        Mockito.when(vehicleInteractor.updateVehicleLocation(USER_ID, CURRENT_LOCATION))
            .thenReturn(Completable.complete());
        Mockito.when(vehicleInteractor.updateVehicleRoute(Mockito.eq(USER_ID), any()))
            .thenReturn(Completable.complete());

        routeInteractor = Mockito.mock(RouteInteractor.class);

        synchronizer = new DefaultExternalVehicleRouteSynchronizer(
            vehicleInteractor,
            routeInteractor,
            user,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testEmptyPlanJustUpdatesLocation() {
        synchronizer.synchronizeForPlan(new VehiclePlan(Collections.emptyList()), CURRENT_LOCATION)
            .test()
            .assertComplete();
        Mockito.verify(vehicleInteractor).updateVehicleLocation(USER_ID, CURRENT_LOCATION);
        Mockito.verifyNoMoreInteractions(vehicleInteractor, routeInteractor);
    }

    @Test
    public void testCanSynchronizePlanWithSingleStep() {
        final Waypoint waypoint = new Waypoint(
            "trip-1",
            Collections.singletonList("step-1"),
            new Action(new LatLng(2, 3), ActionType.DRIVE_TO_DROP_OFF, TRIP_INFO)
        );
        final VehiclePlan planToSync = new VehiclePlan(Collections.singletonList(waypoint));
        final RouteInfoModel route = new RouteInfoModel(
            Arrays.asList(CURRENT_LOCATION.getLatLng(), waypoint.getAction().getDestination()),
            6000,
            3000
        );

        Mockito.when(routeInteractor.getRouteForWaypoints(any()))
            .thenReturn(Observable.just(Collections.singletonList(route)));

        synchronizer.synchronizeForPlan(planToSync, CURRENT_LOCATION).test().assertComplete();

        Mockito.verify(routeInteractor).getRouteForWaypoints(
            Arrays.asList(CURRENT_LOCATION.getLatLng(), waypoint.getAction().getDestination())
        );
        Mockito.verify(vehicleInteractor).updateVehicleLocation(USER_ID, CURRENT_LOCATION);
        final List<VehicleDisplayRouteLeg> legs = Collections.singletonList(new VehicleDisplayRouteLeg(
            null,
            Pair.create(waypoint.getTaskId(), waypoint.getStepIds().get(0)),
            route
        ));
        Mockito.verify(vehicleInteractor).updateVehicleRoute(USER_ID, legs);
    }

    @Test
    public void testCanSynchronizePlanWithMultipleSteps() {
        final Waypoint waypoint0 = new Waypoint(
            "trip-1",
            Collections.singletonList("step-1"),
            new Action(new LatLng(2, 3), ActionType.DRIVE_TO_PICKUP, TRIP_INFO)
        );
        final Waypoint waypoint1 = new Waypoint(
            "trip-1",
            Collections.singletonList("step-2"),
            new Action(new LatLng(4, 5), ActionType.DRIVE_TO_DROP_OFF, TRIP_INFO)
        );

        final VehiclePlan planToSync = new VehiclePlan(Arrays.asList(waypoint0, waypoint1));

        final RouteInfoModel route0 = new RouteInfoModel(
            Arrays.asList(CURRENT_LOCATION.getLatLng(), waypoint0.getAction().getDestination()),
            6000,
            3000
        );

        final RouteInfoModel route1 = new RouteInfoModel(
            Arrays.asList(waypoint0.getAction().getDestination(), waypoint1.getAction().getDestination()),
            6001,
            3001
        );

        Mockito.when(routeInteractor.getRouteForWaypoints(any()))
            .thenReturn(Observable.just(Arrays.asList(route0, route1)));

        synchronizer.synchronizeForPlan(planToSync, CURRENT_LOCATION).test().assertComplete();

        Mockito.verify(routeInteractor).getRouteForWaypoints(
            Arrays.asList(CURRENT_LOCATION.getLatLng(), waypoint0.getAction().getDestination(), waypoint1.getAction().getDestination())
        );
        Mockito.verify(vehicleInteractor).updateVehicleLocation(USER_ID, CURRENT_LOCATION);
        final List<VehicleDisplayRouteLeg> legs = Arrays.asList(
            new VehicleDisplayRouteLeg(
                null,
                Pair.create(waypoint0.getTaskId(), waypoint0.getStepIds().get(0)),
                route0
            ),
            new VehicleDisplayRouteLeg(
                Pair.create(waypoint0.getTaskId(), waypoint0.getStepIds().get(0)),
                Pair.create(waypoint1.getTaskId(), waypoint1.getStepIds().get(0)),
                route1
            )
        );
        Mockito.verify(vehicleInteractor).updateVehicleRoute(USER_ID, legs);
    }

    @Test
    public void testNoErrorsAreEmittedIfExceptionOccurs() {
        final Waypoint waypoint = new Waypoint(
            "trip-1",
            Collections.singletonList("step-1"),
            new Action(new LatLng(2, 3), ActionType.DRIVE_TO_DROP_OFF, TRIP_INFO)
        );
        final VehiclePlan planToSync = new VehiclePlan(Collections.singletonList(waypoint));

        Mockito.when(routeInteractor.getRouteForWaypoints(any()))
            .thenReturn(Observable.error(new IOException()));

        synchronizer.synchronizeForPlan(planToSync, CURRENT_LOCATION).test().assertNoErrors().assertComplete();
    }
}
