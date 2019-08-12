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
package ai.rideos.android.driver_app.menu.developer_options;

import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.settings.DriverStorageKeys;
import io.reactivex.Observable;

public class DefaultDriverDeveloperOptionsViewModel implements DriverDeveloperOptionsViewModel {
    private final UserStorageReader reader;
    private final UserStorageWriter writer;

    public DefaultDriverDeveloperOptionsViewModel(final UserStorageReader reader,
                                                  final UserStorageWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public Observable<Boolean> isNavigationSimulationEnabled() {
        return reader.observeBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION)
            .firstOrError()
            .toObservable();
    }

    @Override
    public void setNavigationSimulationEnabled(final boolean enabled) {
        writer.storeBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION, enabled);
    }
}
