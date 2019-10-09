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
package ai.rideos.android.interactors;

import ai.rideos.android.common.model.UserProfile;
import ai.rideos.android.common.model.VehicleInfo;
import ai.rideos.android.common.model.VehicleInfo.ContactInfo;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DriverUserProfileInteractorTest {
    private static final String USER_ID = "user-1";
    private static final String NAME = "name";
    private static final String PHONE_NUMBER = "123-456-7890";

    private DriverUserProfileInteractor interactorUnderTest;
    private DriverVehicleInteractor driverVehicleInteractor;

    @Before
    public void setUp() {
        driverVehicleInteractor = Mockito.mock(DriverVehicleInteractor.class);

        interactorUnderTest = new DriverUserProfileInteractor(driverVehicleInteractor);
    }

    @Test
    public void testStoreUserProfile() {
        Mockito.when(driverVehicleInteractor.updateContactInfo(Mockito.anyString(), Mockito.any()))
            .thenReturn(Completable.complete());

        interactorUnderTest.storeUserProfile(USER_ID, new UserProfile(NAME, PHONE_NUMBER))
            .test()
            .assertComplete();
        Mockito.verify(driverVehicleInteractor)
            .updateContactInfo(USER_ID, new ContactInfo(NAME, PHONE_NUMBER, ""));
    }

    @Test
    public void testGetUserProfile() {
        Mockito.when(driverVehicleInteractor.getVehicleInfo(Mockito.anyString()))
            .thenReturn(Single.just(new VehicleInfo(
                "license",
                new ContactInfo(NAME, PHONE_NUMBER, "")
            )));

        interactorUnderTest.getUserProfile(USER_ID).test()
            .assertValueAt(0, new UserProfile(NAME, PHONE_NUMBER));

        Mockito.verify(driverVehicleInteractor).getVehicleInfo(USER_ID);
    }
}
