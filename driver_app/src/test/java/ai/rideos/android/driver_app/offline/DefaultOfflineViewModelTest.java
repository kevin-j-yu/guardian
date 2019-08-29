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
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.driver_app.offline.OfflineViewModel.OfflineViewState;
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
        viewModelUnderTest = new DefaultOfflineViewModel(user, vehicleInteractor, new TrampolineSchedulerProvider());
    }

    @Test
    public void testInitialStateDefaultsToOffline() {
        viewModelUnderTest.getOfflineViewState().test()
            .assertValueAt(0, OfflineViewState.OFFLINE);
    }

    @Test
    public void testCanGoOnlineWhenOffline() {
        final TestObserver<OfflineViewState> stateObserver = viewModelUnderTest.getOfflineViewState().test();
        Mockito.when(vehicleInteractor.markVehicleReady(Mockito.anyString())).thenReturn(Completable.complete());
        viewModelUnderTest.goOnline();

        stateObserver.assertValueCount(3)
            .assertValueAt(0, OfflineViewState.OFFLINE)
            .assertValueAt(1, OfflineViewState.GOING_ONLINE)
            .assertValueAt(2, OfflineViewState.ONLINE);

        Mockito.verify(vehicleInteractor).markVehicleReady(VEHICLE_ID);
    }

    @Test
    public void testStateReflectsFailureWhenGoingOnlineFails() {
        Mockito.when(vehicleInteractor.markVehicleReady(Mockito.anyString()))
            .thenReturn(Completable.error(new IOException()));
        viewModelUnderTest.goOnline();

        viewModelUnderTest.getOfflineViewState().test()
            .assertValueAt(0, OfflineViewState.FAILED_TO_GO_ONLINE);
        Mockito.verify(vehicleInteractor).markVehicleReady(VEHICLE_ID);
    }

    @Test
    public void testCannotGoOnlineAfterAlreadyOnline() {
        Mockito.when(vehicleInteractor.markVehicleReady(Mockito.anyString())).thenReturn(Completable.complete());
        viewModelUnderTest.goOnline();

        final TestObserver<OfflineViewState> stateObserver = viewModelUnderTest.getOfflineViewState().test();
        Mockito.verify(vehicleInteractor).markVehicleReady(VEHICLE_ID);

        viewModelUnderTest.goOnline();
        stateObserver.assertValueCount(1).assertValueAt(0, OfflineViewState.ONLINE);
        Mockito.verifyNoMoreInteractions(vehicleInteractor);
    }
}
