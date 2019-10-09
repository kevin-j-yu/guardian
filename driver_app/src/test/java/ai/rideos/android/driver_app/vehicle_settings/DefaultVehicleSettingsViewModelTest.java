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
package ai.rideos.android.driver_app.vehicle_settings;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.common.model.VehicleInfo.ContactInfo;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultVehicleSettingsViewModelTest {
    private static final String USER_ID = "user";
    private static final String LICENSE = "license";

    private DefaultVehicleSettingsViewModel viewModelUnderTest;
    private DriverVehicleInteractor vehicleInteractor;

    @Before
    public void setUp() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);

        vehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);

        Mockito.when(vehicleInteractor.getVehicleInfo(USER_ID))
            .thenReturn(Single.just(new VehicleInfo(
                LICENSE,
                new ContactInfo("", "", "")
            )));

        Mockito.when(vehicleInteractor.updateLicensePlate(Mockito.eq(USER_ID), Mockito.anyString()))
            .thenReturn(Completable.complete());

        viewModelUnderTest = new DefaultVehicleSettingsViewModel(
            user,
            vehicleInteractor,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testCanGetLicenseFromInteractor() {
        viewModelUnderTest.getLicensePlate().test().assertValueAt(0, LICENSE);
    }

    @Test
    public void testCanSaveLicenseAfterEditing() {
        final String newLicense = "new license";
        viewModelUnderTest.editLicensePlate(newLicense);
        viewModelUnderTest.save();
        Mockito.verify(vehicleInteractor).updateLicensePlate(USER_ID, newLicense);
    }

    @Test
    public void testSavingIsDisabledByDefault() {
        viewModelUnderTest.isSavingEnabled().test().assertValueAt(0, false);
    }

    @Test
    public void testSavingIsDisabledWhenEditedLicenseIsSaved() {
        final String newLicense = "new license";
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editLicensePlate(newLicense);
        viewModelUnderTest.save();
        testObserver.assertValueCount(3)
            .assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false);
    }
}
