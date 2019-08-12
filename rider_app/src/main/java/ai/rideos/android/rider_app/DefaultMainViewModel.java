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

import static ai.rideos.android.common.viewmodel.state_machine.StateTransitions.transitionIf;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.BackListener;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.model.MainViewState;
import ai.rideos.android.model.MainViewState.Step;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultMainViewModel implements MainViewModel {
    private static final MainViewState INITIAL_STATE = new MainViewState(Step.START_SCREEN, null);
    private static final int DEFAULT_TASK_POLL_INTERVAL_MILLIS = 1000;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final StateMachine<MainViewState> stateMachine;
    private final Flowable<String> currentTask;
    private final MainViewState initialState;
    private final SchedulerProvider schedulerProvider;
    private final BackListener backListener;
    private final RiderTripInteractor tripInteractor;

    public DefaultMainViewModel(final BackListener backListener,
                                final RiderTripInteractor tripInteractor,
                                final User user) {
        this(
            backListener,
            tripInteractor,
            user,
            INITIAL_STATE,
            new DefaultSchedulerProvider(),
            DEFAULT_TASK_POLL_INTERVAL_MILLIS
        );
    }

    // Allow injection of state machine to allow different schedulers for polling and the state machine
    public DefaultMainViewModel(final BackListener backListener,
                                final RiderTripInteractor tripInteractor,
                                final User user,
                                final MainViewState initialState,
                                final SchedulerProvider schedulerProvider,
                                final int pollIntervalMilli) {
        this.schedulerProvider = schedulerProvider;
        this.initialState = initialState;
        this.stateMachine = new StateMachine<>(schedulerProvider);
        this.backListener = backListener;
        this.tripInteractor = tripInteractor;
        compositeDisposable.addAll(
            stateMachine.start()
        );
        currentTask = pollForCurrentTask(tripInteractor, user.getId(), pollIntervalMilli);
    }

    @Override
    public void initialize() {
        stateMachine.initialize(initialState);
    }

    @Override
    public Observable<MainViewState> getMainViewState() {
        // When main view state is subscribed to, the current task will be polled
        return Observable.combineLatest(
            currentTask.toObservable().startWith(""),
            stateMachine.observeCurrentState(),
            (task, state) -> state
        )
            .distinctUntilChanged();
    }

    @Override
    public void startPreTripFlow() {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.START_SCREEN,
            state -> new MainViewState(Step.PRE_TRIP, null)
        ));
    }

    @Override
    public void cancelTripRequest() {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.PRE_TRIP,
            state -> new MainViewState(Step.START_SCREEN, null)
        ));
    }

    @Override
    public void onTripCreated(final String tripId) {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.PRE_TRIP,
            state -> new MainViewState(Step.ON_TRIP, tripId)
        ));
    }

    @Override
    public void tripFinished() {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.ON_TRIP,
            state -> new MainViewState(Step.START_SCREEN, null)
        ));
    }

    @Override
    public void back() {
        // Always apply event
        stateMachine.transition(state -> {
            // Only go back if the current step is pre-trip
            if (state.getStep() == Step.PRE_TRIP) {
                return new MainViewState(Step.START_SCREEN, null);
            }
            // Otherwise propagate back call to parent and return current state
            backListener.back();
            return state;
        });
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        tripInteractor.shutDown();
    }

    private Flowable<String> pollForCurrentTask(final RiderTripInteractor tripInteractor,
                                                final String passengerId,
                                                final int pollIntervalMilli) {
        // Use `flowable` to take advantage of back-pressure strategies
        // If the interval is emitting faster than the taskInteractor is consuming, it will drop incoming requests
        // so that the queue doesn't fill up
        return Flowable.interval(0, pollIntervalMilli, TimeUnit.MILLISECONDS, schedulerProvider.io())
            .onBackpressureDrop()
            // get current task
            .flatMap(
                // getCurrentTaskForPassenger should only return one value, so drop the rest
                time -> tripInteractor.getCurrentTripForPassenger(passengerId)
                    .observeOn(schedulerProvider.computation())
                    .toFlowable(BackpressureStrategy.DROP),
                1
            )
            .observeOn(schedulerProvider.computation())
            // filter out when task does not exist
            .filter(Optional::isPresent)
            .map(Optional::get)
            // ensure the same task doesn't get reported twice
            .distinctUntilChanged()
            .doOnNext(taskId -> stateMachine.transition(currentState -> new MainViewState(Step.ON_TRIP, taskId)));
    }
}
