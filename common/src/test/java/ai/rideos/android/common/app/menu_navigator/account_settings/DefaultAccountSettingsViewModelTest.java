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
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import com.auth0.android.result.UserProfile;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultAccountSettingsViewModelTest {
    private static final String PREFERRED_NAME = "preferred";

    private DefaultAccountSettingsViewModel viewModelUnderTest;
    private UserStorageWriter userStorageWriter;
    private User user;

    @Before
    public void setUp() {
        user = Mockito.mock(User.class);

        final UserStorageReader reader = Mockito.mock(UserStorageReader.class);
        Mockito.when(reader.getStringPreference(StorageKeys.PREFERRED_NAME)).thenReturn(PREFERRED_NAME);

        userStorageWriter = Mockito.mock(UserStorageWriter.class);

        viewModelUnderTest = new DefaultAccountSettingsViewModel(
            user,
            reader,
            userStorageWriter,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testCanGetEmailFromProfile() {
        final String email = "email@email.com";
        final UserProfile userProfile = Mockito.mock(UserProfile.class);
        Mockito.when(userProfile.getEmail()).thenReturn(email);
        Mockito.when(user.fetchUserProfile()).thenReturn(Single.just(userProfile));
        viewModelUnderTest.getEmail().test().assertValueAt(0, email);
    }

    @Test
    public void testCanGetPreferredNameFromUserStorage() {
        viewModelUnderTest.getPreferredName().test().assertValueAt(0, PREFERRED_NAME);
    }

    @Test
    public void testCanSavePreferredNameAfterEditing() {
        final String newName = "new name";
        viewModelUnderTest.editPreferredName(newName);
        viewModelUnderTest.save();
        Mockito.verify(userStorageWriter).storeStringPreference(StorageKeys.PREFERRED_NAME, newName);
        Mockito.verifyNoMoreInteractions(userStorageWriter);
    }

    @Test
    public void testSavingWithNoEditsDoesNotWriteAnything() {
        viewModelUnderTest.save();
        Mockito.verifyNoMoreInteractions(userStorageWriter);
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
}
