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
package ai.rideos.android.rider_app.pre_trip;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.model.ContactInfo;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.PreTripLocation;
import ai.rideos.android.model.PreTripState;
import ai.rideos.android.model.PreTripState.Step;
import ai.rideos.android.model.VehicleSelectionOption;
import ai.rideos.android.settings.RiderStorageKeys;
import com.auth0.android.result.UserProfile;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultPreTripViewModelTest {
    private static final String FLEET_ID = "fleet-1";
    private static final String PASSENGER_ID = "passenger-1";
    private static final ContactInfo CONTACT_INFO = new ContactInfo("Passenger 1");
    private static final String TASK_ID = "task-1";

    private static final TaskLocation PICKUP = new TaskLocation(new LatLng(0, 1));
    private static final NamedTaskLocation GEO_PICKUP = new NamedTaskLocation("pickup", PICKUP);
    private static final PreTripLocation CONFIRMED_PICKUP = new PreTripLocation(new DesiredAndAssignedLocation(GEO_PICKUP), true);
    private static final PreTripLocation UNCONFIRMED_PICKUP = new PreTripLocation(new DesiredAndAssignedLocation(GEO_PICKUP), false);

    private static final TaskLocation DROP_OFF = new TaskLocation(new LatLng(1, 2));
    private static final NamedTaskLocation GEO_DROP_OFF = new NamedTaskLocation("drop-off", DROP_OFF);
    private static final PreTripLocation CONFIRMED_DROP_OFF = new PreTripLocation(new DesiredAndAssignedLocation(GEO_DROP_OFF), true);
    private static final PreTripLocation UNCONFIRMED_DROP_OFF = new PreTripLocation(new DesiredAndAssignedLocation(GEO_DROP_OFF), false);

    private static final int NUM_PASSENGERS = 1;
    private static final String VEHICLE_ID = "test-vehicle";
    private static final VehicleSelectionOption VEHICLE = VehicleSelectionOption.manual(VEHICLE_ID);
    private static final int TASK_RETRY_COUNT = 1;
    private DefaultPreTripViewModel viewModelUnderTest;
    private TestObserver<PreTripState> stateObserver;
    private RiderTripInteractor tripInteractor;
    private PreTripListener listener;
    private UserStorageReader userStorageReader;
    private User user;

    private void setUpWithInitialState(final PreTripState initialState) {
        tripInteractor = Mockito.mock(RiderTripInteractor.class);
        Mockito.when(tripInteractor.createTripForPassenger(PASSENGER_ID, CONTACT_INFO, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF))
            .thenReturn(Observable.just(TASK_ID));
        Mockito.when(tripInteractor.createTripForPassengerAndVehicle(PASSENGER_ID, CONTACT_INFO, VEHICLE_ID, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF))
            .thenReturn(Observable.just(TASK_ID));
        listener = Mockito.mock(PreTripListener.class);
        userStorageReader = Mockito.mock(UserStorageReader.class);
        Mockito.when(userStorageReader.getBooleanPreference(RiderStorageKeys.MANUAL_VEHICLE_SELECTION)).thenReturn(false);
        Mockito.when(userStorageReader.getStringPreference(StorageKeys.PREFERRED_NAME)).thenReturn(CONTACT_INFO.getName());

        user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(PASSENGER_ID);
        viewModelUnderTest = new DefaultPreTripViewModel(
            listener,
            tripInteractor,
            user,
            Observable.just(new FleetInfo(FLEET_ID)),
            userStorageReader,
            new TrampolineSchedulerProvider(),
            initialState,
            RetryBehaviors.retryAtMost(TASK_RETRY_COUNT)
        );
        viewModelUnderTest.initialize();
        stateObserver = viewModelUnderTest.getPreTripState().test();
    }

    @Test
    public void testStateMachineCanGiveInitialState() {
        final PreTripState initialState = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);
        setUpWithInitialState(initialState);

        stateObserver
            .assertValueCount(1)
            .assertValueAt(0, initialState);
    }

    @Test
    public void testLocationSearchTransitionsToConfirmDropOffWhenNotSelectedOnMap() {
        final PreTripState initialState = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);
        setUpWithInitialState(initialState);

        viewModelUnderTest.selectPickupDropOff(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(
                UNCONFIRMED_PICKUP,
                UNCONFIRMED_DROP_OFF,
                0,
                null,
                Step.CONFIRMING_DROP_OFF
            ));
    }

    @Test
    public void testLocationSearchTransitionsToConfirmPickupWhenDropOffSelectedOnMap() {
        final PreTripState initialState = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);
        setUpWithInitialState(initialState);

        viewModelUnderTest.selectPickupDropOff(UNCONFIRMED_PICKUP, CONFIRMED_DROP_OFF);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(
                UNCONFIRMED_PICKUP,
                CONFIRMED_DROP_OFF,
                0,
                null,
                Step.CONFIRMING_PICKUP
            ));
    }

    @Test
    public void testLocationSearchTransitionsToConfirmTripWhenPickupDropOffSelectedOnMap() {
        final PreTripState initialState = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);
        setUpWithInitialState(initialState);

        viewModelUnderTest.selectPickupDropOff(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(
                CONFIRMED_PICKUP,
                CONFIRMED_DROP_OFF,
                0,
                null,
                Step.CONFIRMING_TRIP)
            );
    }

    @Test
    public void testConfirmPickupTransitionsToConfirmTrip() {
        final PreTripState initialState = new PreTripState(
            UNCONFIRMED_PICKUP,
            CONFIRMED_DROP_OFF,
            0,
            null,
            Step.CONFIRMING_PICKUP
        );
        setUpWithInitialState(initialState);
        final DesiredAndAssignedLocation newLocation = new DesiredAndAssignedLocation(
            new NamedTaskLocation("new", PICKUP)
        );

        viewModelUnderTest.confirmPickup(newLocation);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(
                new PreTripLocation(newLocation, true),
                CONFIRMED_DROP_OFF,
                0,
                null,
                Step.CONFIRMING_TRIP
            ));
    }

    @Test
    public void testConfirmDropOffTransitionsToConfirmPickupWhenPickupNotSelectedOnMap() {
        final PreTripState initialState = new PreTripState(UNCONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_DROP_OFF);
        setUpWithInitialState(initialState);
        final DesiredAndAssignedLocation newLocation = new DesiredAndAssignedLocation(
            new NamedTaskLocation("new", DROP_OFF)
        );

        viewModelUnderTest.confirmDropOff(newLocation);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(
                UNCONFIRMED_PICKUP,
                new PreTripLocation(newLocation, true),
                0,
                null,
                Step.CONFIRMING_PICKUP)
            );
    }

    @Test
    public void testConfirmTripTransitionsToConfirmed() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_TRIP);
        setUpWithInitialState(initialState);

        viewModelUnderTest.confirmTrip(NUM_PASSENGERS);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, NUM_PASSENGERS, null, Step.CONFIRMED));
    }

    @Test
    public void testConfirmTripTriggersTaskCreationAndListener() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_TRIP);
        setUpWithInitialState(initialState);

        viewModelUnderTest.confirmTrip(NUM_PASSENGERS);
        Mockito.verify(tripInteractor).createTripForPassenger(PASSENGER_ID, CONTACT_INFO, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF);
        Mockito.verify(listener).onTripCreated(TASK_ID);
    }

    @Test
    public void testTaskCreationUsesUserEmailWhenPreferredNameNotDefined() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_TRIP);
        setUpWithInitialState(initialState);
        Mockito.when(userStorageReader.getStringPreference(StorageKeys.PREFERRED_NAME)).thenReturn("");

        final String userEmail = "test@rideos.ai";
        final UserProfile userProfile = Mockito.mock(UserProfile.class);
        Mockito.when(userProfile.getEmail()).thenReturn(userEmail);
        Mockito.when(user.fetchUserProfile()).thenReturn(Single.just(userProfile));

        final ContactInfo expectedContact = new ContactInfo(userEmail);
        Mockito.when(tripInteractor.createTripForPassenger(PASSENGER_ID, expectedContact, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF))
            .thenReturn(Observable.just(TASK_ID));

        viewModelUnderTest.confirmTrip(NUM_PASSENGERS);
        Mockito.verify(tripInteractor).createTripForPassenger(PASSENGER_ID, expectedContact, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF);
        Mockito.verify(listener).onTripCreated(TASK_ID);
    }

    @Test
    public void testTaskCreationFailureTransitionsBackToConfirmingTrip() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_TRIP);
        setUpWithInitialState(initialState);
        Mockito.when(tripInteractor.createTripForPassenger(PASSENGER_ID, CONTACT_INFO, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF))
            .thenAnswer(i -> Observable.error(new IOException()));
        viewModelUnderTest.confirmTrip(NUM_PASSENGERS);

        stateObserver.assertValueCount(3)
            .assertValueAt(2, state -> state.getStep() == Step.CONFIRMING_TRIP);
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void testTaskCreationFailureEmitsFailure() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_TRIP);
        setUpWithInitialState(initialState);
        Mockito.when(tripInteractor.createTripForPassenger(PASSENGER_ID, CONTACT_INFO, FLEET_ID, NUM_PASSENGERS, PICKUP, DROP_OFF))
            .thenAnswer(i -> Observable.error(new IOException()));
        final TestObserver<Notification> testObserver = viewModelUnderTest.getTripCreationFailures().test();

        viewModelUnderTest.confirmTrip(NUM_PASSENGERS);

        testObserver.assertValueCount(1);
    }

    @Test
    public void testCallingBackFromFirstStepCallsParent() {
        final PreTripState initialState = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);
        setUpWithInitialState(initialState);

        viewModelUnderTest.back();
        stateObserver
            .assertValueCount(1)
            .assertValueAt(0, initialState);
        Mockito.verify(listener).back();
    }

    @Test
    public void testCallingBackFromOtherStepsGoesBackInHistory() {
        final PreTripState initialState = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);
        setUpWithInitialState(initialState);

        viewModelUnderTest.selectPickupDropOff(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);

        viewModelUnderTest.back();
        stateObserver
            .assertValueCount(3)
            .assertValueAt(2, initialState);
    }

    @Test
    public void testConfirmTripTransitionsToConfirmVehicleWhenManualSelectionEnabled() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, 0, null, Step.CONFIRMING_TRIP);
        setUpWithInitialState(initialState);
        Mockito.when(userStorageReader.getBooleanPreference(RiderStorageKeys.MANUAL_VEHICLE_SELECTION)).thenReturn(true);

        viewModelUnderTest.confirmTrip(NUM_PASSENGERS);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, NUM_PASSENGERS, null, Step.CONFIRMING_VEHICLE));
    }

    @Test
    public void testConfirmVehicleTransitionsToConfirmed() {
        final PreTripState initialState = new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, NUM_PASSENGERS, null, Step.CONFIRMING_VEHICLE);
        setUpWithInitialState(initialState);

        viewModelUnderTest.confirmVehicle(VEHICLE);
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new PreTripState(CONFIRMED_PICKUP, CONFIRMED_DROP_OFF, NUM_PASSENGERS, VEHICLE, Step.CONFIRMED));
    }
}
