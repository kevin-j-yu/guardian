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
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.settings.DriverStorageKeys;
import android.content.Context;
import androidx.annotation.VisibleForTesting;
import io.reactivex.Observable;
import io.reactivex.Single;

public class PotentiallySimulatedDeviceLocator implements DeviceLocator {
    private final DeviceLocator simulatedDeviceLocator;
    private final DeviceLocator nonSimulatedDeviceLocator;
    private final UserStorageReader userStorageReader;
    private final SchedulerProvider schedulerProvider;

    public PotentiallySimulatedDeviceLocator(final Context context) {
        this(
            SimulatedDeviceLocator.get(context),
            new FusedLocationDeviceLocator(context),
            SharedPreferencesUserStorageReader.forContext(context),
            new TrampolineSchedulerProvider()
        );
    }

    @VisibleForTesting
    PotentiallySimulatedDeviceLocator(final DeviceLocator simulatedDeviceLocator,
                                      final DeviceLocator nonSimulatedDeviceLocator,
                                      final UserStorageReader userStorageReader,
                                      final SchedulerProvider schedulerProvider) {
        this.simulatedDeviceLocator = simulatedDeviceLocator;
        this.nonSimulatedDeviceLocator = nonSimulatedDeviceLocator;
        this.userStorageReader = userStorageReader;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<LocationAndHeading> observeCurrentLocation(final int pollIntervalMillis) {
        return userStorageReader.observeBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION)
            .observeOn(schedulerProvider.computation())
            .flatMap(simulationEnabled ->
                getResolvedLocator(simulationEnabled).observeCurrentLocation(pollIntervalMillis)
            );
    }

    @Override
    public Single<LocationAndHeading> getLastKnownLocation() {
        return userStorageReader.observeBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION)
            .observeOn(schedulerProvider.computation())
            .firstOrError()
            .flatMap(simulationEnabled -> getResolvedLocator(simulationEnabled).getLastKnownLocation());
    }

    private DeviceLocator getResolvedLocator(final boolean isSimulationEnabled) {
        return isSimulationEnabled ? simulatedDeviceLocator : nonSimulatedDeviceLocator;
    }
}
