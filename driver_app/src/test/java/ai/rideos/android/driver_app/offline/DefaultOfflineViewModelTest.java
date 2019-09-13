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
package ai.rideos.android.driver_app.offline;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultOfflineViewModelTest {
    private static final String VEHICLE_ID = "vehicle-1";

    private DefaultOfflineViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;

    @Before
    public void setUp() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(VEHICLE_ID);
        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);
        viewModelUnderTest = new DefaultOfflineViewModel(user, vehicleInteractor);
    }

    @Test
    public void testInitialStateDefaultsToIdle() {
        viewModelUnderTest.getGoingOnlineProgress().test()
            .assertValueAt(0, ProgressState.IDLE);
    }

    @Test
    public void testCanGoOnlineWhenOffline() {
        final TestObserver<ProgressState> stateObserver = viewModelUnderTest.getGoingOnlineProgress().test();
        Mockito.when(vehicleInteractor.markVehicleReady(Mockito.anyString())).thenReturn(Completable.complete());
        viewModelUnderTest.goOnline();

        stateObserver.assertValueCount(3)
            .assertValueAt(0, ProgressState.IDLE)
            .assertValueAt(1, ProgressState.LOADING)
            .assertValueAt(2, ProgressState.SUCCEEDED);

        Mockito.verify(vehicleInteractor).markVehicleReady(VEHICLE_ID);
    }

    @Test
    public void testStateReflectsFailureWhenGoingOnlineFails() {
        Mockito.when(vehicleInteractor.markVehicleReady(Mockito.anyString()))
            .thenReturn(Completable.error(new IOException()));
        viewModelUnderTest.goOnline();

        viewModelUnderTest.getGoingOnlineProgress().test()
            .assertValueAt(0, ProgressState.FAILED);
        Mockito.verify(vehicleInteractor).markVehicleReady(VEHICLE_ID);
    }

    @Test
    public void testCannotGoOnlineAfterAlreadyOnline() {
        Mockito.when(vehicleInteractor.markVehicleReady(Mockito.anyString())).thenReturn(Completable.complete());
        viewModelUnderTest.goOnline();

        final TestObserver<ProgressState> stateObserver = viewModelUnderTest.getGoingOnlineProgress().test();
        Mockito.verify(vehicleInteractor).markVehicleReady(VEHICLE_ID);

        viewModelUnderTest.goOnline();
        stateObserver.assertValueCount(1).assertValueAt(0, ProgressState.SUCCEEDED);
    }
}
