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

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposables;
import java.util.function.Supplier;

/**
 * FusedLocationDeviceLocator uses the FusedLocationProviderClient built into Android to request updates about the
 * device's current location. To do this, it uses a custom observable. When this observable is subscribed to, it requests
 * location updates and emits them. When this subscription is disposed of, the updates stop.
 */
public class FusedLocationDeviceLocator implements DeviceLocator {
    private final Supplier<FusedLocationProviderClient> clientSupplier;
    private final PermissionsChecker permissionsChecker;
    private final SchedulerProvider schedulerProvider;

    public FusedLocationDeviceLocator(final Context context) {
        this(
            () -> LocationServices.getFusedLocationProviderClient(context),
            new AndroidPermissionsChecker(context),
            new DefaultSchedulerProvider()
        );
    }

    public FusedLocationDeviceLocator(final Supplier<FusedLocationProviderClient> clientSupplier,
                                      final PermissionsChecker permissionsChecker,
                                      final SchedulerProvider schedulerProvider) {
        this.clientSupplier = clientSupplier;
        this.permissionsChecker = permissionsChecker;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<LocationAndHeading> observeCurrentLocation(final int pollIntervalMillis) {
        final LocationRequest locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(pollIntervalMillis);
        return Observable.create(new FusedLocationObservableOnSubscribe(locationRequest))
            .subscribeOn(schedulerProvider.mainThread())
            .observeOn(schedulerProvider.computation())
            // Use the last known heading in the even that the current location contains no heading.
            // To do this, this Rx pipeline scans locations using an empty initial seed with no heading. Whenever it
            // receives a new location, it checks if the new location has a heading. If it does, it uses it. If it
            // doesn't, it uses the previous location's heading. The initial seed is discarded and ignored
            .scan(
                // Empty initial seed
                new LocationAndHeading(new LatLng(0, 0), 0f),
                (oldLocation, newLocation) -> new LocationAndHeading(
                    Locations.getLatLngFromAndroidLocation(newLocation),
                    Locations.getHeadingFromAndroidLocationOrDefault(newLocation, oldLocation.getHeading())
                )
            )
            // Discard initial seed
            .skip(1);
    }

    @SuppressLint("MissingPermission") // This is checked in PermissionsChecker
    @Override
    public Single<LocationAndHeading> getLastKnownLocation() {
        if (!permissionsChecker.areLocationPermissionsGranted()) {
            return Single.error(new PermissionsNotGrantedException("Location permissions not granted"));
        }
        return Single.<Location>create(
            emitter -> clientSupplier.get().getLastLocation()
                .addOnSuccessListener(emitter::onSuccess)
                .addOnFailureListener(emitter::onError)
        )
            .subscribeOn(schedulerProvider.mainThread())
            .observeOn(schedulerProvider.computation())
            .map(location -> new LocationAndHeading(
                Locations.getLatLngFromAndroidLocation(location),
                Locations.getHeadingFromAndroidLocationOrDefault(location, 0f)
            ));
    }

    /**
     * Create an ObservableOnSubscribe, which starts emitting items when subscribed to. In this case, when the observable
     * is subscribed to, location updates begin and when disposed, the locations stop.
     */
    private class FusedLocationObservableOnSubscribe implements ObservableOnSubscribe<Location> {
        private final FusedLocationProviderClient locationClient;
        private final LocationRequest locationRequest;

        FusedLocationObservableOnSubscribe(final LocationRequest locationRequest) {
            this.locationClient = clientSupplier.get();
            this.locationRequest = locationRequest;
        }

        @SuppressLint("MissingPermission") // This is checked in PermissionsChecker
        @Override
        public void subscribe(final ObservableEmitter<Location> emitter) {
            // Check for permissions and emit an error if location access is not permitted. Permissions should be granted
            // in the view. This should be checked only when subscribed so we can get the most updated information
            // about the device permissions.
            if (!permissionsChecker.areLocationPermissionsGranted()) {
                emitter.onError(new PermissionsNotGrantedException("Location permissions not granted"));
            }
            final LocationCallback callback = new DefaultLocationCallback(emitter);
            locationClient.requestLocationUpdates(locationRequest, callback, null);
            emitter.setDisposable(Disposables.fromAction(() -> dispose(callback)));
        }

        private void dispose(final LocationCallback listener) {
            locationClient.removeLocationUpdates(listener);
        }
    }

    /**
     * Callback that emits a LatLng coordinate whenever onLocationResult is called.
     */
    private static class DefaultLocationCallback extends LocationCallback {
        private final ObservableEmitter<Location> emitter;

        DefaultLocationCallback(final ObservableEmitter<Location> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onLocationResult(final LocationResult result) {
            final Location lastLocation = result.getLastLocation();
            if (lastLocation != null) {
                this.emitter.onNext(lastLocation);
            }
        }
    }
}
