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
package ai.rideos.android.common.app.menu_navigator.developer_options;

import static org.junit.Assert.assertEquals;

import ai.rideos.android.common.R;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.fleets.FleetResolver;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.interactors.FleetInteractor;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.ApiEnvironment;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.common.view.resources.ResourceProvider;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultDeveloperOptionsViewModelTest {
    private static final List<FleetInfo> FLEETS = Arrays.asList(new FleetInfo(""), new FleetInfo("fleet1"));
    private static final int PREFERRED_FLEET_INDEX = 0;
    private static final String USER_ID = "user-id";

    private DefaultDeveloperOptionsViewModel viewModelUnderTest;
    private UserStorageReader userStorageReader;
    private ResourceProvider resourceProvider;
    private ResolvedFleet resolvedFleet;

    @Before
    public void setUp() {
        final FleetInteractor fleetInteractor = Mockito.mock(FleetInteractor.class);
        Mockito.when(fleetInteractor.getFleets()).thenReturn(Observable.just(FLEETS));

        userStorageReader = Mockito.mock(UserStorageReader.class);
        final UserStorageWriter userStorageWriter = Mockito.mock(UserStorageWriter.class);
        final ApiEnvironment preferredEnv = ApiEnvironment.values()[0];
        Mockito.when(userStorageReader.getStringPreference(StorageKeys.RIDEOS_API_ENV))
            .thenReturn(preferredEnv.getStoredName());
        Mockito.when(userStorageReader.getStringPreference(StorageKeys.FLEET_ID))
            .thenReturn(FLEETS.get(PREFERRED_FLEET_INDEX).getId());

        resourceProvider = Mockito.mock(ResourceProvider.class);

        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(USER_ID);

        resolvedFleet = Mockito.mock(ResolvedFleet.class);

        viewModelUnderTest = new DefaultDeveloperOptionsViewModel(
            fleetInteractor,
            userStorageReader,
            userStorageWriter,
            resourceProvider,
            user,
            resolvedFleet,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testGetEnvOptionsReturnsPreferredEnvAsSelected() {
        viewModelUnderTest.getEnvironmentOptions().test()
            .assertValueCount(1)
            .assertValueAt(0, options -> options.getSelectionIndex().get() == 0);
    }

    @Test
    public void testGetEnvOptionsReturnsAllApiEnvironmentsAsOptions() {
        final SingleSelectOptions<ApiEnvironment> options = viewModelUnderTest.getEnvironmentOptions().test()
            .values().get(0);

        assertEquals(ApiEnvironment.values().length, options.getOptions().size());
        for (int i = 0; i < ApiEnvironment.values().length; i++) {
            assertEquals(ApiEnvironment.values()[i], options.getOptions().get(i).getValue());
        }
    }

    @Test
    public void testGetFleetIdsReturnsPreferredFleetId() {
        viewModelUnderTest.getFleetOptions().test()
            .assertValueCount(1)
            // Automatic fleet is inserted first
            .assertValueAt(0, options -> options.getSelectionIndex().get() == PREFERRED_FLEET_INDEX + 1);
    }

    @Test
    public void testGetFleetIdsReturnsNoSelectedFleetIdWhenPreferredFleetDoesNotExist() {
        Mockito.when(userStorageReader.getStringPreference(StorageKeys.FLEET_ID)).thenReturn("unknown_fleet");
        viewModelUnderTest.getFleetOptions().test()
            .assertValueCount(1)
            .assertValueAt(0, options -> !options.getSelectionIndex().isPresent());
    }

    @Test
    public void testCanGetInitialFleetIdOptionsWithoutSelectingEnv() {
        final SingleSelectOptions<String> options = viewModelUnderTest.getFleetOptions().test()
            .values().get(0);
        assertEquals(FLEETS.size() + 1, options.getOptions().size());
        assertEquals(FleetResolver.AUTOMATIC_FLEET_ID, options.getOptions().get(0).getValue());
        for (int i = 0; i < FLEETS.size(); i++) {
            assertEquals(FLEETS.get(i).getId(), options.getOptions().get(i + 1).getValue());
        }
    }

    @Test
    public void testGetFleetIdsFillsInDefaultFleetDisplayName() {
        final String expectedDisplayName = "default-fleet";
        Mockito.when(resourceProvider.getString(R.string.default_fleet_id_option_display))
            .thenReturn(expectedDisplayName);
        viewModelUnderTest.getFleetOptions().test()
            .assertValueAt(0, options -> options.getOptions().get(1)
                .getDisplayText().equals(expectedDisplayName)
            );
    }

    @Test
    public void testResolvedFleetFillsInDefaultFleetDisplayName() {
        final String expectedDisplayName = "default-fleet";
        Mockito.when(resourceProvider.getString(R.string.default_fleet_id_option_display))
            .thenReturn(expectedDisplayName);
        Mockito.when(resolvedFleet.observeFleetInfo()).thenReturn(Observable.just(new FleetInfo("")));
        viewModelUnderTest.getResolvedFleetId().test()
            .assertValueAt(0, expectedDisplayName);
    }
}
