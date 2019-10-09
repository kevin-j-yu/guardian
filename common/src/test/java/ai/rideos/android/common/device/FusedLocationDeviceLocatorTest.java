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
package ai.rideos.android.common.device;

import static org.mockito.Mockito.any;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class FusedLocationDeviceLocatorTest {
    private static final LatLng LAT_LNG_0 = new LatLng(0, 0);
    private static final LatLng LAT_LNG_1 = new LatLng(0, 1);
    private static final LatLng LAT_LNG_2 = new LatLng(0, 2);
    private static final LatLng LAT_LNG_3 = new LatLng(0, 3);
    private static final int POLL_INTERVAL_MILLIS = 100;

    private FusedLocationProviderClient client;
    private FusedLocationDeviceLocator locatorUnderTest;
    private PermissionsChecker permissionsChecker;

    @Before
    public void setUp() {
        client = Mockito.mock(FusedLocationProviderClient.class);
        permissionsChecker = Mockito.mock(PermissionsChecker.class);
        locatorUnderTest = new FusedLocationDeviceLocator(
            () -> client,
            permissionsChecker,
            new TrampolineSchedulerProvider()
        );
        Mockito.when(permissionsChecker.areLocationPermissionsGranted()).thenReturn(true);
    }

    @Test
    public void testObserveErrorWhenPermissionsNotGranted() {
        Mockito.when(permissionsChecker.areLocationPermissionsGranted()).thenReturn(false);
        locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS).test()
            .assertValueCount(0)
            .assertError(error -> error instanceof PermissionsNotGrantedException);
    }

    @Test
    public void testLocatorRequestsLocationUpdatesWhenObserverSubscribes() {
        final Observable<LocationAndHeading> observableLocation = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS);
        Mockito.verifyNoMoreInteractions(client);
        observableLocation.subscribeOn(Schedulers.trampoline()).subscribe();
        Mockito.verify(client).requestLocationUpdates(any(), any(), any());
    }

    @Test
    public void testCanReceiveLocationUpdatesAfterSubscription() {
        final ArgumentCaptor<LocationCallback> callbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);
        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS)
            .subscribeOn(Schedulers.trampoline())
            .test();
        Mockito.verify(client).requestLocationUpdates(any(), callbackCaptor.capture(), any());
        final LocationCallback callback = callbackCaptor.getValue();

        final Location mockLocation = mockLocation(LAT_LNG_0, 10.0f);
        callback.onLocationResult(LocationResult.create(Collections.singletonList(mockLocation)));

        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(LAT_LNG_0, 10.0f));
    }

    @Test
    public void testLocationUpdatesUseLastKnownHeadingWhenHeadingUnknown() {
        final ArgumentCaptor<LocationCallback> callbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);
        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS)
            .subscribeOn(Schedulers.trampoline())
            .test();
        Mockito.verify(client).requestLocationUpdates(any(), callbackCaptor.capture(), any());
        final LocationCallback callback = callbackCaptor.getValue();

        final List<Location> locationsEmitted = Arrays.asList(
            mockLocationWithoutHeading(LAT_LNG_0), // heading should default to 0.0
            mockLocation(LAT_LNG_1, 10.0f), // heading should be updated
            mockLocationWithoutHeading(LAT_LNG_2), // heading should be remembered as 10.0f
            mockLocation(LAT_LNG_3, 15.0f) // heading should be updated
        );

        locationsEmitted.forEach(location -> callback.onLocationResult(
            LocationResult.create(Collections.singletonList(location))
        ));

        testObserver.assertValueCount(4)
            .assertValueAt(0, new LocationAndHeading(LAT_LNG_0, 0.0f))
            .assertValueAt(1, new LocationAndHeading(LAT_LNG_1, 10.0f))
            .assertValueAt(2, new LocationAndHeading(LAT_LNG_2, 10.0f))
            .assertValueAt(3, new LocationAndHeading(LAT_LNG_3, 15.0f));
    }

    @Test
    public void testLocatorStopsLocationUpdatesWhenSubscriptionIsDisposed() {
        final Observable<LocationAndHeading> observableLocation = locatorUnderTest.observeCurrentLocation(POLL_INTERVAL_MILLIS);
        final Disposable disposable = observableLocation.subscribeOn(Schedulers.trampoline()).subscribe();
        disposable.dispose();
        Mockito.verify(client).removeLocationUpdates(any(LocationCallback.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastKnownLocationWhenLocationExists() {
        final ArgumentCaptor<OnSuccessListener> onSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        final Task<Location> mockTask = (Task<Location>) Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnSuccessListener(onSuccessCaptor.capture())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(Mockito.any())).thenReturn(mockTask);
        Mockito.when(client.getLastLocation()).thenReturn(mockTask);

        final Location location = mockLocation(LAT_LNG_1, 10.0f);

        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.getLastKnownLocation().test();

        onSuccessCaptor.getValue().onSuccess(location);
        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(LAT_LNG_1, 10.0f));
        Mockito.verify(client, Mockito.never()).requestLocationUpdates(any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastKnownLocationFallsBackToLocationUpdatesWhenLastLocationUnknown() {
        final ArgumentCaptor<OnSuccessListener> onSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        final Task<Location> mockTask = (Task<Location>) Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnSuccessListener(onSuccessCaptor.capture())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(Mockito.any())).thenReturn(mockTask);
        Mockito.when(client.getLastLocation()).thenReturn(mockTask);

        final ArgumentCaptor<LocationCallback> callbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);

        final Location mockLocation = mockLocation(LAT_LNG_1, 10.0f);

        // Try to get last known location
        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.getLastKnownLocation().test();

        // Send null as last known location
        onSuccessCaptor.getValue().onSuccess(null);
        // Assert that no value should appear in the observer
        testObserver.assertValueCount(0);

        // Assert that location updates have started
        Mockito.verify(client).requestLocationUpdates(any(), callbackCaptor.capture(), any());
        // Get the location update callback and send a location
        final LocationCallback callback = callbackCaptor.getValue();
        callback.onLocationResult(LocationResult.create(Collections.singletonList(mockLocation)));

        // Assert that this location is received
        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(LAT_LNG_1, 10.0f));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLastKnownLocationFallsBackToLocationUpdatesWhenLastLocationErrors() {
        final ArgumentCaptor<OnFailureListener> onFailureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        final Task<Location> mockTask = (Task<Location>) Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnSuccessListener(Mockito.any())).thenReturn(mockTask);
        Mockito.when(mockTask.addOnFailureListener(onFailureCaptor.capture())).thenReturn(mockTask);
        Mockito.when(client.getLastLocation()).thenReturn(mockTask);

        final ArgumentCaptor<LocationCallback> callbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);

        final Location mockLocation = mockLocation(LAT_LNG_1, 10.0f);

        // Try to get last known location
        final TestObserver<LocationAndHeading> testObserver = locatorUnderTest.getLastKnownLocation().test();

        // Send error to failure callback
        onFailureCaptor.getValue().onFailure(new IOException());
        // Assert that no value should appear in the observer
        testObserver.assertValueCount(0);

        // Assert that location updates have started
        Mockito.verify(client).requestLocationUpdates(any(), callbackCaptor.capture(), any());
        // Get the location update callback and send a location
        final LocationCallback callback = callbackCaptor.getValue();
        callback.onLocationResult(LocationResult.create(Collections.singletonList(mockLocation)));

        // Assert that this location is received
        testObserver.assertValueCount(1)
            .assertValueAt(0, new LocationAndHeading(LAT_LNG_1, 10.0f));
    }

    private static Location mockLocation(final LatLng latLng, final float heading) {
        final Location mockLocation = Mockito.mock(Location.class);
        Mockito.when(mockLocation.getLatitude()).thenReturn(latLng.getLatitude());
        Mockito.when(mockLocation.getLongitude()).thenReturn(latLng.getLongitude());
        Mockito.when(mockLocation.getBearing()).thenReturn(heading);
        Mockito.when(mockLocation.hasBearing()).thenReturn(true);
        return mockLocation;
    }

    private static Location mockLocationWithoutHeading(final LatLng latLng) {
        final Location mockLocation = Mockito.mock(Location.class);
        Mockito.when(mockLocation.getLatitude()).thenReturn(latLng.getLatitude());
        Mockito.when(mockLocation.getLongitude()).thenReturn(latLng.getLongitude());
        Mockito.when(mockLocation.getBearing()).thenReturn(0.0f);
        Mockito.when(mockLocation.hasBearing()).thenReturn(false);
        return mockLocation;
    }
}
