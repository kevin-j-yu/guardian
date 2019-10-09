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
package ai.rideos.android.common.app.menu_navigator.account_settings;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.UserProfile;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultAccountSettingsViewModelTest {
    private static final String USER_ID = "user";
    private static final String PREFERRED_NAME = "preferred";
    private static final String PHONE = "123-456-7890";

    private DefaultAccountSettingsViewModel viewModelUnderTest;
    private UserProfileInteractor userProfileInteractor;
    private User user;

    @Before
    public void setUp() {
        user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);

        userProfileInteractor = Mockito.mock(UserProfileInteractor.class);

        Mockito.when(userProfileInteractor.getUserProfile(USER_ID))
            .thenReturn(Single.just(new UserProfile(PREFERRED_NAME, PHONE)));
        Mockito.when(userProfileInteractor.storeUserProfile(Mockito.eq(USER_ID), Mockito.any()))
            .thenReturn(Completable.complete());

        viewModelUnderTest = new DefaultAccountSettingsViewModel(
            user,
            userProfileInteractor,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testCanGetEmailFromProfile() {
        final String email = "email@email.com";
        final com.auth0.android.result.UserProfile userProfile
            = Mockito.mock(com.auth0.android.result.UserProfile.class);
        Mockito.when(userProfile.getEmail()).thenReturn(email);
        Mockito.when(user.fetchUserProfile()).thenReturn(Single.just(userProfile));
        viewModelUnderTest.getEmail().test().assertValueAt(0, email);
    }

    @Test
    public void testCanGetPreferredNameFromInteractor() {
        viewModelUnderTest.getPreferredName().test().assertValueAt(0, PREFERRED_NAME);
    }

    @Test
    public void testCanGetPhoneNumberFromInteractor() {
        viewModelUnderTest.getPhoneNumber().test().assertValueAt(0, PHONE);
    }

    @Test
    public void testCanSavePreferredNameAndPhoneAfterEditing() {
        final String newName = "new name";
        final String newPhone = "111-222-3333";
        viewModelUnderTest.editPreferredName(newName);
        viewModelUnderTest.editPhoneNumber(newPhone);
        viewModelUnderTest.save();
        Mockito.verify(userProfileInteractor).storeUserProfile(USER_ID, new UserProfile(newName, newPhone));
    }

    @Test
    public void testCanSavePreferredNameAndPhoneAndDefaultIfNotEdited() {
        final String newName = "new name";
        viewModelUnderTest.editPreferredName(newName);
        viewModelUnderTest.save();
        Mockito.verify(userProfileInteractor).storeUserProfile(USER_ID, new UserProfile(newName, PHONE));
    }

    @Test
    public void testSavingIsDisabledByDefault() {
        viewModelUnderTest.isSavingEnabled().test().assertValueAt(0, false);
    }

    @Test
    public void testSavingIsEnabledWhenPreferredNameIsEdited() {
        final String newName = "new name";
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editPreferredName(newName);
        testObserver.assertValueCount(2).assertValueAt(1, true);
    }

    @Test
    public void testSavingIsDisabledWhenEditedNameIsTheSame() {
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editPreferredName(PREFERRED_NAME);
        testObserver.assertValueCount(1).assertValueAt(0, false);
    }

    @Test
    public void testSavingIsDisabledWhenEditedNamesAreSaved() {
        final String newPreferred = "new given";
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editPreferredName(newPreferred);
        viewModelUnderTest.save();
        testObserver.assertValueCount(3)
            .assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false);
    }

    @Test
    public void testSavingIsEnabledWhenPhoneNumberIsEdited() {
        final String newPhone = "111-222-3333";
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editPhoneNumber(newPhone);
        testObserver.assertValueCount(2).assertValueAt(1, true);
    }

    @Test
    public void testSavingIsDisabledWhenEditedPhoneNumberIsTheSame() {
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editPhoneNumber(PHONE);
        testObserver.assertValueCount(1).assertValueAt(0, false);
    }

    @Test
    public void testSavingIsDisabledWhenEditedPhoneIsSaved() {
        final String newPhone = "111-222-3333";
        final TestObserver<Boolean> testObserver = viewModelUnderTest.isSavingEnabled().test();
        viewModelUnderTest.editPhoneNumber(newPhone);
        viewModelUnderTest.save();
        testObserver.assertValueCount(3)
            .assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false);
    }
}
