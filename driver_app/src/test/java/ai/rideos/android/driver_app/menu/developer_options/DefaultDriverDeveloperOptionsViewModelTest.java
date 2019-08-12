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
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultDriverDeveloperOptionsViewModelTest {
    @Test
    public void testIsSimulationEnabledReturnsValueFromUserStorage() {
        final UserStorageReader userStorageReader = Mockito.mock(UserStorageReader.class);
        Mockito.when(userStorageReader.observeBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION))
            .thenReturn(Observable.just(true));

        final DefaultDriverDeveloperOptionsViewModel viewModel = new DefaultDriverDeveloperOptionsViewModel(
            userStorageReader,
            Mockito.mock(UserStorageWriter.class)
        );

        viewModel.isNavigationSimulationEnabled().test()
            .assertValueCount(1)
            .assertValueAt(0, true);
    }

    @Test
    public void testCanStoreSimulationEnabledInUserStorage() {
        final UserStorageWriter userStorageWriter = Mockito.mock(UserStorageWriter.class);

        final DefaultDriverDeveloperOptionsViewModel viewModel = new DefaultDriverDeveloperOptionsViewModel(
            Mockito.mock(UserStorageReader.class),
            userStorageWriter
        );

        viewModel.setNavigationSimulationEnabled(true);
        Mockito.verify(userStorageWriter).storeBooleanPreference(DriverStorageKeys.SIMULATE_NAVIGATION, true);
    }
}
