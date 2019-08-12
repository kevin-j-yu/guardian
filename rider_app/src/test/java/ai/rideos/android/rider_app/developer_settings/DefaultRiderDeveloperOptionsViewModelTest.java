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
package ai.rideos.android.rider_app.developer_settings;

import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.settings.RiderStorageKeys;
import io.reactivex.Observable;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRiderDeveloperOptionsViewModelTest {
    @Test
    public void testIsManualSelectionEnabledReturnsValueFromUserStorage() {
        final UserStorageReader userStorageReader = Mockito.mock(UserStorageReader.class);
        Mockito.when(userStorageReader.observeBooleanPreference(RiderStorageKeys.MANUAL_VEHICLE_SELECTION))
            .thenReturn(Observable.just(true));

        final DefaultRiderDeveloperOptionsViewModel viewModel = new DefaultRiderDeveloperOptionsViewModel(
            userStorageReader,
            Mockito.mock(UserStorageWriter.class)
        );

        viewModel.isManualVehicleSelectionEnabled().test()
            .assertValueCount(1)
            .assertValueAt(0, true);
    }

    @Test
    public void testCanStoreVehicleSelectionInUserStorage() {
        final UserStorageWriter userStorageWriter = Mockito.mock(UserStorageWriter.class);

        final DefaultRiderDeveloperOptionsViewModel viewModel = new DefaultRiderDeveloperOptionsViewModel(
            Mockito.mock(UserStorageReader.class),
            userStorageWriter
        );

        viewModel.setManualVehicleSelection(true);
        Mockito.verify(userStorageWriter).storeBooleanPreference(RiderStorageKeys.MANUAL_VEHICLE_SELECTION, true);
    }
}
