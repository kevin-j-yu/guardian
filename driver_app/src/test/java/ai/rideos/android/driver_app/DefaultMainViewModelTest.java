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
package ai.rideos.android.driver_app;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.MainViewState;
import ai.rideos.android.model.VehicleStatus;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultMainViewModelTest {
    private static final String VEHICLE_ID = "vehicle-1";

    private DefaultMainViewModel viewModelUnderTest;

    @Test
    public void testViewModelEmitsInitialStateBasedOnVehicleStatus() {
        setUpWithInitialState(VehicleStatus.UNREGISTERED);
        viewModelUnderTest.getMainViewState().test()
            .assertValueCount(1)
            .assertValueAt(0, MainViewState.UNREGISTERED);
    }

    @Test
    public void testStateTransitionsFromOfflineToOnline() {
        setUpWithInitialState(VehicleStatus.NOT_READY);

        final TestObserver<MainViewState> stateObserver = viewModelUnderTest.getMainViewState().test();
        stateObserver.assertValueAt(0, MainViewState.OFFLINE);

        viewModelUnderTest.didGoOnline();
        stateObserver.assertValueAt(1, MainViewState.ONLINE);
    }

    @Test
    public void testStateTransitionsFromOnlineToOffline() {
        setUpWithInitialState(VehicleStatus.READY);

        final TestObserver<MainViewState> stateObserver = viewModelUnderTest.getMainViewState().test();
        stateObserver.assertValueAt(0, MainViewState.ONLINE);

        viewModelUnderTest.didGoOffline();
        stateObserver.assertValueAt(1, MainViewState.OFFLINE);
    }

    @Test
    public void testStateTransitionsFromUnregisteredToOffline() {
        setUpWithInitialState(VehicleStatus.UNREGISTERED);

        final TestObserver<MainViewState> stateObserver = viewModelUnderTest.getMainViewState().test();
        stateObserver.assertValueAt(0, MainViewState.UNREGISTERED);

        viewModelUnderTest.doneRegistering();
        stateObserver.assertValueAt(1, MainViewState.OFFLINE);
    }

    private void setUpWithInitialState(final VehicleStatus vehicleStatus) {
        final DriverVehicleInteractor vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(VEHICLE_ID);
        Mockito.when(vehicleInteractor.getVehicleStatus(VEHICLE_ID))
            .thenReturn(Single.just(vehicleStatus));
        viewModelUnderTest = new DefaultMainViewModel(
            vehicleInteractor,
            user,
            new TrampolineSchedulerProvider()
        );
        viewModelUnderTest.initialize();
    }
}
