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

import ai.rideos.android.common.app.menu_navigator.account_settings.UserProfileInteractor;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import com.auth0.android.result.UserProfile;
import io.reactivex.Single;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultNavigationHeaderViewModelTest {
    private static final String USER_ID = "user-1";
    private static final String PICTURE_URL = "https://rideos.ai";
    private static final String EMAIL = "bot@rideos.ai";

    private DefaultNavigationHeaderViewModel viewModelUnderTest;
    private UserProfileInteractor userProfileInteractor;

    @Before
    public void setUp() {
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);
        userProfileInteractor = Mockito.mock(UserProfileInteractor.class);

        final UserProfile userProfile = Mockito.mock(UserProfile.class);
        Mockito.when(userProfile.getPictureURL()).thenReturn(PICTURE_URL);
        Mockito.when(userProfile.getEmail()).thenReturn(EMAIL);
        Mockito.when(user.fetchUserProfile()).thenReturn(Single.just(userProfile));

        viewModelUnderTest = new DefaultNavigationHeaderViewModel(
            user,
            userProfileInteractor,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testCanGetProfilePictureUrlFromUserProfile() {
        viewModelUnderTest.getProfilePictureUrl().test()
            .assertValueAt(0, PICTURE_URL);
    }

    @Test
    public void testCanGetFullNameFromUserProfile() {
        final String preferredName = "preferredName";
        Mockito.when(userProfileInteractor.getUserProfile(USER_ID))
            .thenReturn(Single.just(new ai.rideos.android.common.model.UserProfile(preferredName, "")));

        viewModelUnderTest.getFullName().firstOrError()
            .test()
            .assertValueAt(0, preferredName);
    }

    @Test
    public void testGettingFullNameNeverEmitsErrors() {
        final String preferredName = "preferredName";
        Mockito.when(userProfileInteractor.getUserProfile(USER_ID))
            .thenReturn(Single.error(new IOException()))
            .thenReturn(Single.just(new ai.rideos.android.common.model.UserProfile(preferredName, "")));

        viewModelUnderTest.getFullName().firstOrError()
            .test()
            .assertNoErrors()
            .assertValueAt(0, preferredName);
    }

    @Test
    public void testCanGetEmailFromUserProfile() {
        viewModelUnderTest.getEmail().test()
            .assertValueAt(0, EMAIL);
    }
}
