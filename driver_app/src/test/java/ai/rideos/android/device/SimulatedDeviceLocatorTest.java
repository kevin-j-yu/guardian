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
package ai.rideos.android.device;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.BehaviorSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SimulatedDeviceLocatorTest {
    private static final int POLL_INTERVAL_MS = 100;
    private static final LocationAndHeading INITIAL_LOCATION = new LocationAndHeading(new LatLng(0, 1), 20);
    private static final LocationAndHeading NEXT_LOCATION = new LocationAndHeading(new LatLng(2, 3), 30);

    private SimulatedDeviceLocator simulatedDeviceLocator;
    private DeviceLocator initialDeviceLocator;

    @Before
    public void setUp() {
        initialDeviceLocator = Mockito.mock(DeviceLocator.class);
        simulatedDeviceLocator = new SimulatedDeviceLocator(initialDeviceLocator);
    }

    @Test
    public void testCanObserveCurrentLocationInitially() {
        Mockito.when(initialDeviceLocator.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(Observable.just(INITIAL_LOCATION));

        simulatedDeviceLocator.observeCurrentLocation(POLL_INTERVAL_MS).test()
            .assertValueCount(1)
            .assertValueAt(0, INITIAL_LOCATION);
        Mockito.verify(initialDeviceLocator).observeCurrentLocation(POLL_INTERVAL_MS);
    }

    @Test
    public void testInitialDeviceLocationUpdatesStopWhenSimulatedLocationSet() {
        final BehaviorSubject<LocationAndHeading> initialLocationSubject = BehaviorSubject.create();
        Mockito.when(initialDeviceLocator.observeCurrentLocation(Mockito.anyInt()))
            .thenReturn(initialLocationSubject);
        initialLocationSubject.onNext(INITIAL_LOCATION);

        final TestObserver<LocationAndHeading> locationObserver = simulatedDeviceLocator
            .observeCurrentLocation(POLL_INTERVAL_MS)
            .test();

        locationObserver.assertValueCount(1);

        simulatedDeviceLocator.updateSimulatedLocation(NEXT_LOCATION);
        locationObserver.assertValueCount(2)
            .assertValueAt(1, NEXT_LOCATION);

        // Try sending another location to the initial device locator
        initialLocationSubject.onNext(new LocationAndHeading(new LatLng(4, 5), 40));
        locationObserver.assertValueCount(2);
    }

    @Test
    public void testCanGetInitialLastKnownLocation() {
        Mockito.when(initialDeviceLocator.getLastKnownLocation()).thenReturn(Single.just(INITIAL_LOCATION));
        simulatedDeviceLocator.getLastKnownLocation().test()
            .assertValueCount(1)
            .assertValueAt(0, INITIAL_LOCATION);
    }

    @Test
    public void testGetLastLocationReturnsSimulatedLocationOnceSet() {
        Mockito.when(initialDeviceLocator.getLastKnownLocation()).thenReturn(Single.just(INITIAL_LOCATION));
        simulatedDeviceLocator.updateSimulatedLocation(NEXT_LOCATION);
        simulatedDeviceLocator.getLastKnownLocation().test()
            .assertValueCount(1)
            .assertValueAt(0, NEXT_LOCATION);
    }
}
