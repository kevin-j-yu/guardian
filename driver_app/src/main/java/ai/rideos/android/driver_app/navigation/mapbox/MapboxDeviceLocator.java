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
package ai.rideos.android.driver_app.navigation.mapbox;

import ai.rideos.android.common.device.AndroidPermissionsChecker;
import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.device.PermissionsChecker;
import ai.rideos.android.common.device.PermissionsNotGrantedException;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposables;
import timber.log.Timber;

/**
 * MapboxDeviceLocator is a device locator that uses Mapbox's LocationEngine class. This class itself uses almost the
 * same interface as FusedLocationProviderClient, so this implementation is very close to FusedLocationDeviceLocator.
 */
public class MapboxDeviceLocator implements DeviceLocator {
    private final LocationEngine mapboxLocationEngine;
    private final PermissionsChecker permissionsChecker;
    private final SchedulerProvider schedulerProvider;

    public MapboxDeviceLocator(final Context context, final LocationEngine mapboxLocationEngine) {
        this(mapboxLocationEngine, new AndroidPermissionsChecker(context), new DefaultSchedulerProvider());
    }

    public MapboxDeviceLocator(final LocationEngine mapboxLocationEngine,
                               final PermissionsChecker permissionsChecker,
                               final SchedulerProvider schedulerProvider) {
        this.mapboxLocationEngine = mapboxLocationEngine;
        this.permissionsChecker = permissionsChecker;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<LocationAndHeading> observeCurrentLocation(final int pollIntervalMillis) {
        final LocationEngineRequest locationRequest = new LocationEngineRequest.Builder(pollIntervalMillis)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build();
        return Observable.create(new LocationEngineObservableOnSubscribe(locationRequest))
            .subscribeOn(schedulerProvider.mainThread());
    }

    @SuppressLint("MissingPermission") // This is checked in PermissionsChecker
    @Override
    public Single<LocationAndHeading> getLastKnownLocation() {
        if (!permissionsChecker.areLocationPermissionsGranted()) {
            return Single.error(new PermissionsNotGrantedException("Location permissions not granted"));
        }
        return Single.<Location>create(
            emitter -> mapboxLocationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(final LocationEngineResult result) {
                    emitter.onSuccess(result.getLastLocation());
                }

                @Override
                public void onFailure(@NonNull final Exception exception) {
                    emitter.onError(exception);
                }
            })
        )
            .map(location -> new LocationAndHeading(
                Locations.getLatLngFromAndroidLocation(location),
                location.getBearing()
            ))
            .subscribeOn(schedulerProvider.mainThread());
    }

    /**
     * Create an ObservableOnSubscribe, which starts emitting items when subscribed to. In this case, when the observable
     * is subscribed to, location updates begin and when disposed, the locations stop.
     */
    private class LocationEngineObservableOnSubscribe implements ObservableOnSubscribe<LocationAndHeading> {
        private final LocationEngineRequest locationRequest;

        LocationEngineObservableOnSubscribe(final LocationEngineRequest locationRequest) {
            this.locationRequest = locationRequest;
        }

        @SuppressLint("MissingPermission") // This is checked in PermissionsChecker
        @Override
        public void subscribe(final ObservableEmitter<LocationAndHeading> emitter) {
            if (!permissionsChecker.areLocationPermissionsGranted()) {
                emitter.onError(new PermissionsNotGrantedException("Location permissions not granted"));
            }
            final DefaultLocationCallback callback = new DefaultLocationCallback(emitter);
            mapboxLocationEngine.requestLocationUpdates(locationRequest, callback, null);
            emitter.setDisposable(Disposables.fromAction(() -> dispose(callback)));
        }

        private void dispose(final DefaultLocationCallback listener) {
            mapboxLocationEngine.removeLocationUpdates(listener);
        }
    }

    /**
     * Callback that emits a LatLng coordinate whenever onLocationResult is called.
     */
    private static class DefaultLocationCallback implements LocationEngineCallback<LocationEngineResult> {
        private final ObservableEmitter<LocationAndHeading> emitter;

        DefaultLocationCallback(final ObservableEmitter<LocationAndHeading> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onSuccess(final LocationEngineResult result) {
            final Location lastLocation = result.getLastLocation();
            if (lastLocation != null) {
                this.emitter.onNext(new LocationAndHeading(
                    Locations.getLatLngFromAndroidLocation(lastLocation),
                    lastLocation.getBearing()
                ));
            }
        }

        @Override
        public void onFailure(@NonNull final Exception exception) {
            // Don't throw exceptions for transient errors, just log.
            Timber.e(exception, "Failed to get location");
        }
    }
}
