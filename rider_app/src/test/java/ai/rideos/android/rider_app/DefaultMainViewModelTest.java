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
package ai.rideos.android.rider_app;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProviders.TestSchedulerProvider;
import ai.rideos.android.common.viewmodel.BackListener;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.model.MainViewState;
import ai.rideos.android.model.MainViewState.Step;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultMainViewModelTest {
    private static final String TASK_ID = "task-1";
    private static final String PASSENGER_ID = "passenger-1";
    private static final int POLL_INTERVAL_MILLIS = 100;

    private DefaultMainViewModel viewModelUnderTest;
    private BackListener listener;
    private TestObserver<MainViewState> stateObserver;
    private TestScheduler testScheduler;

    private void setUpWithInitialState(final MainViewState initialState) {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID)).thenReturn(Observable.just(Optional.empty()));

        setUpWithInitialState(initialState, tripInteractor);
    }

    private void setUpWithInitialState(final MainViewState initialState,
                                       final RiderTripInteractor tripInteractor) {
        setUpWithInitialState(initialState, tripInteractor, new TestScheduler());
    }

    private void setUpWithInitialState(final MainViewState initialState,
                                       final RiderTripInteractor tripInteractor,
                                       final TestScheduler testScheduler) {
        listener = Mockito.mock(BackListener.class);
        this.testScheduler = testScheduler;
        final User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(PASSENGER_ID);
        viewModelUnderTest = new DefaultMainViewModel(
            listener,
            tripInteractor,
            user,
            initialState,
            // needed for polling
            new TestSchedulerProvider(testScheduler),
            POLL_INTERVAL_MILLIS
        );
        viewModelUnderTest.initialize();
        stateObserver = viewModelUnderTest.getMainViewState().test();
        // Because the main view state also triggers polling the task id on the test scheduler,
        // we need to trigger actions on the test scheduler in order to observe the main view state.
        testScheduler.triggerActions();
    }

    @Test
    public void testStateMachineCanGetInitialState() {
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null));
        stateObserver
            .assertValueCount(1)
            .assertValueAt(0, new MainViewState(Step.START_SCREEN, null));
    }

    @Test
    public void testStartScreenTransitionsToPreTrip() {
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null));
        viewModelUnderTest.startPreTripFlow();
        testScheduler.triggerActions();
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.PRE_TRIP, null));
    }

    @Test
    public void testPreTripTransitionsToOnTrip() {
        setUpWithInitialState(new MainViewState(Step.PRE_TRIP, null));
        viewModelUnderTest.onTripCreated(TASK_ID);
        testScheduler.triggerActions();
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.ON_TRIP, TASK_ID));
    }

    @Test
    public void testOnTripTransitionsToStartScreen() {
        setUpWithInitialState(new MainViewState(Step.ON_TRIP, null));
        viewModelUnderTest.tripFinished();
        testScheduler.triggerActions();
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.START_SCREEN, null));
    }

    @Test
    public void testPreTripCanGoBackToStartScreen() {
        setUpWithInitialState(new MainViewState(Step.PRE_TRIP, null));
        viewModelUnderTest.back();
        testScheduler.triggerActions();
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.START_SCREEN, null));
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void testCallingBackFromStartScreenCallsParent() {
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null));
        viewModelUnderTest.back();
        testScheduler.triggerActions();
        stateObserver.assertValueCount(1);
        Mockito.verify(listener).back();
    }

    @Test
    public void testCallingBackFromOnTripCallsParent() {
        setUpWithInitialState(new MainViewState(Step.ON_TRIP, TASK_ID));
        viewModelUnderTest.back();
        testScheduler.triggerActions();
        stateObserver.assertValueCount(1);
        Mockito.verify(listener).back();
    }

    @Test
    public void testStateDoesNotChangeWhenNoCurrentTaskExists() {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID))
            .thenReturn(Observable.just(Optional.empty()));
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null), tripInteractor);

        testScheduler.advanceTimeBy(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        stateObserver.assertValueCount(1);
    }

    @Test
    public void testStateTransitionsToOnTripWhenTaskExists() {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID))
            .thenReturn(Observable.just(Optional.of(TASK_ID)));
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null), tripInteractor);

        testScheduler.advanceTimeBy(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        stateObserver.assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.ON_TRIP, TASK_ID));
    }

    @Test
    public void testOnlyOneStateTransitionOccursAfterPolling() {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        // Mock a situation where a task id is returned, then an error occurs, and then the same task id is returned
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID))
            .thenReturn(Observable.just(Optional.of(TASK_ID)))
            .thenReturn(Observable.just(Optional.empty()))
            .thenReturn(Observable.just(Optional.of(TASK_ID)));
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null), tripInteractor);

        testScheduler.advanceTimeBy(3 * POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        stateObserver.assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.ON_TRIP, TASK_ID));
    }

    @Test
    public void testTaskIdIsUpdatedWhenItChangesInBackend() {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID))
            .thenReturn(Observable.just(Optional.of(TASK_ID)))
            .thenReturn(Observable.just(Optional.of("task-2")));
        setUpWithInitialState(new MainViewState(Step.START_SCREEN, null), tripInteractor);

        testScheduler.advanceTimeBy(2 * POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        stateObserver.assertValueCount(3)
            .assertValueAt(1, new MainViewState(Step.ON_TRIP, "task-1"))
            .assertValueAt(2, new MainViewState(Step.ON_TRIP, "task-2"));
    }

    @Test
    public void testOnlyOneStateTransitionIfTaskIsCreatedAndTaskIsPolled() {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID))
            .thenReturn(Observable.just(Optional.of(TASK_ID)));
        setUpWithInitialState(new MainViewState(Step.PRE_TRIP, null), tripInteractor);
        viewModelUnderTest.onTripCreated(TASK_ID);
        stateObserver.assertValueCount(2);

        // Verify that the current task is polled, but the state isn't updated
        testScheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS);
        stateObserver.assertValueCount(2);
        Mockito.verify(tripInteractor).getCurrentTripForPassenger(PASSENGER_ID);
    }

    @Test
    public void testTaskIdRequestsDoNotBufferIfTakingLongerThanPollInterval() {
        final RiderTripInteractor tripInteractor = Mockito.mock(RiderTripInteractor.class);
        // Return task 1 after a long delay, then task 2
        final TestScheduler scheduler = new TestScheduler();
        Mockito.when(tripInteractor.getCurrentTripForPassenger(PASSENGER_ID))
            .thenReturn(
                Observable.just(Optional.of(TASK_ID))
                    .delay(3 * POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, scheduler)
            )
            .thenReturn(Observable.just(Optional.of("task-2")));
        setUpWithInitialState(new MainViewState(Step.PRE_TRIP, null), tripInteractor, scheduler);

        scheduler.advanceTimeBy(3 * POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        // Using the "onBackPressureDrop" strategy, it should drop all incoming requests until the first completes
        // When the first completes after 3 poll intervals, another one should emit
        stateObserver.assertValueCount(3)
            .assertValueAt(0, new MainViewState(Step.PRE_TRIP, null))
            .assertValueAt(1, new MainViewState(Step.ON_TRIP, TASK_ID))
            .assertValueAt(2, new MainViewState(Step.ON_TRIP, "task-2"));
    }

    @Test
    public void testCancelTripRequestGoesBackToStartScreen() {
        setUpWithInitialState(new MainViewState(Step.PRE_TRIP, null));
        viewModelUnderTest.cancelTripRequest();
        testScheduler.triggerActions();
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1, new MainViewState(Step.START_SCREEN, null));
    }

}
