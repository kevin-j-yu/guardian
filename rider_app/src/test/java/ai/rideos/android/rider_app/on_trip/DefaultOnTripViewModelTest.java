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
package ai.rideos.android.rider_app.on_trip;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.OnTripDisplayState;
import ai.rideos.android.model.OnTripDisplayState.Display;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultOnTripViewModelTest {
    private static final String TASK_ID = "task-id";
    private static final String PASSENGER_ID = "passenger-id";

    private DefaultOnTripViewModel viewModelUnderTest;
    private RiderTripInteractor tripInteractor;
    private OnTripListener onTripListener;

    @Before
    public void setUp() {
        tripInteractor = Mockito.mock(RiderTripInteractor.class);
        onTripListener = Mockito.mock(OnTripListener.class);

        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(PASSENGER_ID);

        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getString(Mockito.anyInt())).thenReturn("");

        viewModelUnderTest = new DefaultOnTripViewModel(
            user,
            tripInteractor,
            onTripListener,
            new TrampolineSchedulerProvider()
        );
        viewModelUnderTest.initialize(TASK_ID);
    }

    @Test
    public void testInitialStateIsFollowTrip() {
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new OnTripDisplayState(Display.CURRENT_TRIP, TASK_ID));
    }

    @Test
    public void testCallingEditPickupTransitionsToEditPickupDisplay() {
        viewModelUnderTest.changePickup();
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new OnTripDisplayState(Display.EDITING_PICKUP, TASK_ID));
    }

    @Test
    public void testEditingPickupLocationInitiatesTripUpdate() {
        final NamedTaskLocation newPickup = new NamedTaskLocation("new-pickup", new LatLng(0, 1));
        Mockito.when(tripInteractor.editPickup(TASK_ID, newPickup.getLocation()))
            .thenReturn(Observable.just("new-task"));
        viewModelUnderTest.changePickup();
        viewModelUnderTest.confirmPickup(new DesiredAndAssignedLocation(newPickup));
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new OnTripDisplayState(Display.CONFIRMING_EDIT_PICKUP, TASK_ID, newPickup));
        Mockito.verify(tripInteractor).editPickup(TASK_ID, newPickup.getLocation());
    }

    @Test
    public void testTripUpdateFailureTransitionsBackToFollowTrip() {
        final NamedTaskLocation newPickup = new NamedTaskLocation("new-pickup", new LatLng(0, 1));
        Mockito.when(tripInteractor.editPickup(TASK_ID, newPickup.getLocation()))
            .thenReturn(Observable.error(new IOException()));
        viewModelUnderTest.changePickup();
        viewModelUnderTest.confirmPickup(new DesiredAndAssignedLocation(newPickup));
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new OnTripDisplayState(Display.CURRENT_TRIP, TASK_ID));
    }

    @Test
    public void testCallingCancelTripTransitionsToCancellingThenCurrentTrip() {
        Mockito.when(tripInteractor.cancelTrip(PASSENGER_ID, TASK_ID))
            .thenReturn(Completable.complete());
        final TestObserver<OnTripDisplayState> displayState = viewModelUnderTest.getDisplayState().test();
        viewModelUnderTest.cancelTrip();
        displayState.assertValueCount(3)
            .assertValueAt(0, state -> state.getDisplay() == Display.CURRENT_TRIP)
            .assertValueAt(1, state -> state.getDisplay() == Display.CONFIRMING_CANCEL)
            .assertValueAt(2, state -> state.getDisplay() == Display.CURRENT_TRIP);
    }

    @Test
    public void testCancellationFailureTransitionsBackToFollowTrip() {
        Mockito.when(tripInteractor.cancelTrip(PASSENGER_ID, TASK_ID))
            .thenReturn(Completable.error(new IOException()));
        viewModelUnderTest.cancelTrip();
        Mockito.verifyNoMoreInteractions(onTripListener);
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new OnTripDisplayState(Display.CURRENT_TRIP, TASK_ID));
    }

    @Test
    public void testCallingBackFromEditPickupGoesToFollowTrip() {
        viewModelUnderTest.changePickup();
        viewModelUnderTest.back();
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new OnTripDisplayState(Display.CURRENT_TRIP, TASK_ID));
    }
}
