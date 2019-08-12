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
package ai.rideos.android.common.app.menu_navigator.menu.navigation_header;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import com.auth0.android.result.UserProfile;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultNavigationHeaderViewModelTest {
    private static final String PICTURE_URL = "https://rideos.ai";
    private static final String EMAIL = "bot@rideos.ai";

    private DefaultNavigationHeaderViewModel viewModelUnderTest;
    private UserStorageReader userStorageReader;

    @Before
    public void setUp() {
        final User user = Mockito.mock(User.class);
        userStorageReader = Mockito.mock(UserStorageReader.class);

        final UserProfile userProfile = Mockito.mock(UserProfile.class);
        Mockito.when(userProfile.getPictureURL()).thenReturn(PICTURE_URL);
        Mockito.when(userProfile.getEmail()).thenReturn(EMAIL);
        Mockito.when(user.fetchUserProfile()).thenReturn(Single.just(userProfile));

        viewModelUnderTest = new DefaultNavigationHeaderViewModel(
            user,
            userStorageReader,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testCanGetProfilePictureUrlFromUserProfile() {
        viewModelUnderTest.getProfilePictureUrl().test()
            .assertValueAt(0, PICTURE_URL);
    }

    @Test
    public void testCanGetFullNameFromUserStorage() {
        final String preferredName = "preferredName";
        Mockito.when(userStorageReader.observeStringPreference(StorageKeys.PREFERRED_NAME))
            .thenReturn(Observable.just(preferredName));

        viewModelUnderTest.getFullName().test()
            .assertValueAt(0, preferredName);
    }

    @Test
    public void testCanGetEmailFromUserProfile() {
        viewModelUnderTest.getEmail().test()
            .assertValueAt(0, EMAIL);
    }
}
