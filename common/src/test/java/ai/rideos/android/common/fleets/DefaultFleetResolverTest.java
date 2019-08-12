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
package ai.rideos.android.common.fleets;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.FleetInteractor;
import ai.rideos.android.common.location.DistanceCalculator;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultFleetResolverTest {
    private static final LocationAndHeading CURRENT_LOCATION
        = new LocationAndHeading(new LatLng(5, 5), 1.0f);
    private static final String MANUAL_FLEET_ID = "fleet-1";

    private DefaultFleetResolver resolver;
    private FleetInteractor fleetInteractor;

    @Before
    public void setUp() {
        fleetInteractor = Mockito.mock(FleetInteractor.class);
        final DeviceLocator deviceLocator = Mockito.mock(DeviceLocator.class);
        Mockito.when(deviceLocator.getLastKnownLocation())
            .thenReturn(Single.just(CURRENT_LOCATION));

        resolver = new DefaultFleetResolver(
            fleetInteractor,
            deviceLocator,
            new TestDistanceCalculator(),
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testResolvingFleetIdReturnsFleetInfoWhenPresent() {
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(
            new FleetInfo(MANUAL_FLEET_ID),
            new FleetInfo("fleet-2"),
            new FleetInfo("fleet-3")
        )));
        resolver.resolveFleet(Observable.just(MANUAL_FLEET_ID)).test()
            .assertValueAt(0, new FleetInfo(MANUAL_FLEET_ID));
    }

    @Test
    public void testResolvingReturnsDefaultFleetWhenFleetAndLocationsUnknown() {
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(
            new FleetInfo("fleet-2"),
            new FleetInfo("fleet-3")
        )));
        resolver.resolveFleet(Observable.just(MANUAL_FLEET_ID)).test()
            .assertValueAt(0, FleetInfo.DEFAULT_FLEET);
    }

    @Test
    public void testResolvingAutomaticFleetReturnsClosestFleetWhenLocationsKnown() {
        final FleetInfo closestFleet = new FleetInfo("fleet-2", "Fleet", CURRENT_LOCATION.getLatLng(), false);
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(
            closestFleet,
            new FleetInfo("fleet-3", "Fleet", new LatLng(15, 15), false),
            new FleetInfo("fleet-4", "Fleet", new LatLng(30, 30), false)
        )));
        resolver.resolveFleet(Observable.just(FleetResolver.AUTOMATIC_FLEET_ID)).test()
            .assertValueAt(0, closestFleet);
    }

    @Test
    public void testResolvingAutomaticFleetReturnsClosestPhantomFleetWhenOnlyPhantomsExist() {
        final FleetInfo closestFleet = new FleetInfo("fleet-2", "Fleet", CURRENT_LOCATION.getLatLng(), true);
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(
            closestFleet,
            new FleetInfo("fleet-3", "Fleet", new LatLng(15, 15), true),
            new FleetInfo("fleet-4", "Fleet", new LatLng(30, 30), true)
        )));
        resolver.resolveFleet(Observable.just(FleetResolver.AUTOMATIC_FLEET_ID)).test()
            .assertValueAt(0, closestFleet);
    }

    @Test
    public void testResolvingAutomaticFleetReturnsClosestNonPhantomFleetWhenBothExist() {
        final FleetInfo nonPhantom = new FleetInfo("fleet-3", "Fleet", new LatLng(15, 15), false);
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(
            new FleetInfo("fleet-2", "Fleet", CURRENT_LOCATION.getLatLng(), true),
            nonPhantom,
            new FleetInfo("fleet-4", "Fleet", new LatLng(30, 30), false)
        )));
        resolver.resolveFleet(Observable.just(FleetResolver.AUTOMATIC_FLEET_ID)).test()
            .assertValueAt(0, nonPhantom);
    }

    @Test
    public void testResolvingManualFleetReturnsAutomaticFleetWhenIdNotFound() {
        final FleetInfo closestFleet = new FleetInfo("fleet-2", "Fleet", CURRENT_LOCATION.getLatLng(), true);
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(
            closestFleet,
            new FleetInfo("fleet-3", "Fleet", new LatLng(15, 15), true),
            new FleetInfo("fleet-4", "Fleet", new LatLng(30, 30), true)
        )));
        resolver.resolveFleet(Observable.just("unknown-fleet")).test()
            .assertValueAt(0, closestFleet);
    }

    @Test
    public void testDefaultFleetReturnedWhenNoFleetsFound() {
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Collections.emptyList()));
        resolver.resolveFleet(Observable.just("unknown-fleet")).test()
            .assertValueAt(0, FleetInfo.DEFAULT_FLEET);
    }

    @Test
    public void testDefaultFleetReturnedWhenFleetInteractorErrors() {
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.error(new IOException()));
        resolver.resolveFleet(Observable.just("unknown-fleet")).test()
            .assertValueAt(0, FleetInfo.DEFAULT_FLEET);
    }

    @Test
    public void testFleetResolutionUpdatesWhenSelectedFleetIdChanges() {
        final FleetInfo firstFleet = new FleetInfo("fleet-1");
        final FleetInfo secondFleet = new FleetInfo("fleet-2");
        final Subject<String> fleetIdSubject = BehaviorSubject.createDefault(firstFleet.getId());

        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(Arrays.asList(firstFleet, secondFleet)));
        final TestObserver<FleetInfo> fleetObserver = resolver.resolveFleet(fleetIdSubject).test();
        fleetObserver.assertValueCount(1).assertValueAt(0, firstFleet);

        fleetIdSubject.onNext(secondFleet.getId());
        fleetObserver.assertValueCount(2).assertValueAt(1, secondFleet);
    }

    // For testing purposes, just return the total difference in lat/lng. Note this will not work when wrapping around.
    private static class TestDistanceCalculator implements DistanceCalculator {
        @Override
        public double getDistanceInMeters(final LatLng origin, final LatLng destination) {
            return Math.abs(origin.getLatitude() - destination.getLatitude())
                + Math.abs(origin.getLongitude() - destination.getLongitude());
        }
    }
}
