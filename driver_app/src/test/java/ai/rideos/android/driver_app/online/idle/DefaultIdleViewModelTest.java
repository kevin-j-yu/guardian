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
package ai.rideos.android.driver_app.online.idle;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.driver_app.online.idle.IdleViewModel.IdleViewState;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultIdleViewModelTest {
    private static final String VEHICLE_ID = "vehicle-1";

    private DefaultIdleViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;

    @Before
    public void setUp() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(VEHICLE_ID);
        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);
        viewModelUnderTest = new DefaultIdleViewModel(user, vehicleInteractor, new TrampolineSchedulerProvider());
    }

    @Test
    public void testInitialStateDefaultsToOnline() {
        viewModelUnderTest.getIdleViewState().test()
            .assertValueAt(0, IdleViewState.ONLINE);
    }

    @Test
    public void testCanGoOfflineWhenOnline() {
        final TestObserver<IdleViewState> stateObserver = viewModelUnderTest.getIdleViewState().test();
        Mockito.when(vehicleInteractor.markVehicleNotReady(Mockito.anyString())).thenReturn(Completable.complete());
        viewModelUnderTest.goOffline();

        stateObserver.assertValueCount(3)
            .assertValueAt(0, IdleViewState.ONLINE)
            .assertValueAt(1, IdleViewState.GOING_OFFLINE)
            .assertValueAt(2, IdleViewState.OFFLINE);

        Mockito.verify(vehicleInteractor).markVehicleNotReady(VEHICLE_ID);
    }

    @Test
    public void testStateReflectsFailureWhenGoingOfflineFails() {
        Mockito.when(vehicleInteractor.markVehicleNotReady(Mockito.anyString()))
            .thenReturn(Completable.error(new IOException()));
        viewModelUnderTest.goOffline();

        viewModelUnderTest.getIdleViewState().test()
            .assertValueAt(0, IdleViewState.FAILED_TO_GO_OFFLINE);
        Mockito.verify(vehicleInteractor).markVehicleNotReady(VEHICLE_ID);
    }

    @Test
    public void testCannotGoOfflineAfterAlreadyOffline() {
        Mockito.when(vehicleInteractor.markVehicleNotReady(Mockito.anyString())).thenReturn(Completable.complete());
        viewModelUnderTest.goOffline();

        final TestObserver<IdleViewState> stateObserver = viewModelUnderTest.getIdleViewState().test();
        Mockito.verify(vehicleInteractor).markVehicleNotReady(VEHICLE_ID);

        viewModelUnderTest.goOffline();
        stateObserver.assertValueCount(1).assertValueAt(0, IdleViewState.OFFLINE);
        Mockito.verifyNoMoreInteractions(vehicleInteractor);
    }
}
