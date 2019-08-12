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
package ai.rideos.android.driver_app.unregistered.register_vehicle;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.driver_app.vehicle_unregistered.register_vehicle.DefaultRegisterVehicleViewModel;
import ai.rideos.android.driver_app.vehicle_unregistered.register_vehicle.RegisterVehicleListener;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.VehicleRegistration;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRegisterVehicleViewModelTest {
    private static final String VEHICLE_ID = "vehicle-1";
    private static final String FLEET_ID = "fleet-1";

    private DefaultRegisterVehicleViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;
    private RegisterVehicleListener listener;

    @Before
    public void setUp() {
        listener = Mockito.mock(RegisterVehicleListener.class);
        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);

        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(VEHICLE_ID);

        viewModelUnderTest = new DefaultRegisterVehicleViewModel(
            vehicleInteractor,
            user,
            Observable.just(new FleetInfo(FLEET_ID)),
            listener,
            number -> number.length() > 0,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testSavingIsDisabledByDefault() {
        viewModelUnderTest.isSavingEnabled().test()
            .assertValueCount(1)
            .assertValueAt(0, false);
    }

    @Test
    public void testSavingIsEnabledWhenAllFieldsAreValidated() {
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);
        viewModelUnderTest.isSavingEnabled().test()
            .assertValueAt(0, true);
    }

    @Test
    public void testSavingIsDisabledAfterBeingEnabled() {
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);

        final TestObserver<Boolean> enabledObserver = viewModelUnderTest.isSavingEnabled().test();
        enabledObserver.assertValueAt(0, true);

        viewModelUnderTest.setLicensePlate("");
        enabledObserver.assertValueAt(1, false);
    }

    @Test
    public void testSavingRegistrationFailsIfFieldsAreInvalid() {
        viewModelUnderTest.save();
        Mockito.verifyNoMoreInteractions(listener, vehicleInteractor);
    }

    @Test
    public void testSavingRegistrationSucceedsIfAllFieldsAreValid() {
        Mockito.when(vehicleInteractor.createVehicle(anyString(), anyString(), any()))
            .thenReturn(Completable.complete());
        viewModelUnderTest.setPreferredName("Driver");
        viewModelUnderTest.setPhoneNumber("1234567890");
        viewModelUnderTest.setLicensePlate("abc123");
        viewModelUnderTest.setRiderCapacity(4);

        viewModelUnderTest.save();
        Mockito.verify(vehicleInteractor).createVehicle(VEHICLE_ID, FLEET_ID, new VehicleRegistration(
            "Driver",
            "1234567890",
            "abc123",
            4
        ));
        Mockito.verify(listener).doneRegistering();
    }
}
