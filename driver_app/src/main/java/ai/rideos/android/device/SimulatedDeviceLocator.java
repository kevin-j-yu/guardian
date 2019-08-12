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
import ai.rideos.android.common.device.FusedLocationDeviceLocator;
import ai.rideos.android.common.model.LocationAndHeading;
import android.content.Context;
import androidx.annotation.VisibleForTesting;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;

public class SimulatedDeviceLocator implements DeviceLocator {
    private static volatile SimulatedDeviceLocator INSTANCE = null;

    private final BehaviorSubject<LocationAndHeading> simulatedLocationSubject = BehaviorSubject.create();
    private final DeviceLocator initialDeviceLocator;

    public static SimulatedDeviceLocator get(final Context context) {
        if (INSTANCE == null) {
            synchronized (SimulatedDeviceLocator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SimulatedDeviceLocator(context);
                }
            }
        }
        return INSTANCE;
    }

    private SimulatedDeviceLocator(final Context context) {
        this(new FusedLocationDeviceLocator(context));
    }

    @VisibleForTesting
    SimulatedDeviceLocator(final DeviceLocator initialDeviceLocator) {
        this.initialDeviceLocator = initialDeviceLocator;
    }

    public void updateSimulatedLocation(final LocationAndHeading locationAndHeading) {
        simulatedLocationSubject.onNext(locationAndHeading);
    }

    @Override
    public Observable<LocationAndHeading> observeCurrentLocation(final int pollIntervalMillis) {
        final Observable<LocationAndHeading> locationUntilSimulated = initialDeviceLocator
            .observeCurrentLocation(pollIntervalMillis)
            .takeUntil(simulatedLocationSubject);

        return Observable.concat(locationUntilSimulated, simulatedLocationSubject);
    }

    @Override
    public Single<LocationAndHeading> getLastKnownLocation() {
        final LocationAndHeading lastSimulatedLocation = simulatedLocationSubject.getValue();
        if (lastSimulatedLocation == null) {
            return initialDeviceLocator.getLastKnownLocation();
        }
        return Single.just(lastSimulatedLocation);
    }
}
