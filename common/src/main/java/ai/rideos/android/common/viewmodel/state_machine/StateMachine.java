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
package ai.rideos.android.common.viewmodel.state_machine;

import ai.rideos.android.common.reactive.SchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

/**
 * StateMachine is a useful class for view models that behave like a state machine. The class takes in various events
 * that modify the state. These events are applied serially. In the case that the event is asynchronous, the state
 * machine waits for the previous event to finish before starting the next. The current state can be retrieved at any
 * moment through getCurrentState
 * @param <T> - state type
 */
public class StateMachine<T> {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final PublishSubject<AsyncStateTransition<T>> transitionSubject = PublishSubject.create();
    private final BehaviorSubject<T> stateSubject;
    private final SchedulerProvider schedulerProvider;

    /**
     * Initialize the StateMachine with an initial state.
     * @param initialState - initial state of the state machine
     * @param schedulerProvider - scheduler provider for observing events
     */
    public StateMachine(final T initialState, final SchedulerProvider schedulerProvider) {
        stateSubject = BehaviorSubject.createDefault(initialState);
        this.schedulerProvider = schedulerProvider;
    }

    public StateMachine(final SchedulerProvider schedulerProvider) {
        stateSubject = BehaviorSubject.create();
        this.schedulerProvider = schedulerProvider;
    }

    public void initialize(final T initialState) {
        stateSubject.onNext(initialState);
    }

    /**
     * Begin listening to events and apply them to the current state.
     * @return disposable subscription to events
     */
    public Disposable start() {
        compositeDisposable.add(
            transitionSubject
                // The state machine should always be run on a single thread so that each transition is done serially.
                .subscribeOn(schedulerProvider.single())
                // Get the state asynchronously and if the transition fails, re-emit the current state
                // Errors do not stop the state machine, they are merely logged
                .flatMapSingle(event -> {
                    final T currentState = stateSubject.getValue();
                    try {
                        return event.applyAsyncChange(currentState)
                            .doOnError(error -> Timber.e(error, "Error while applying change"))
                            .onErrorReturnItem(currentState);
                    } catch (final InvalidStateTransition error) {
                        Timber.e(error, "Invalid state transition");
                        return Single.just(currentState);
                    }
                })
                .subscribe(stateSubject::onNext)
        );
        return compositeDisposable;
    }

    /**
     * Add a new stateTransition to the state machine stateTransition subject.
     * @param stateTransition - synchronous stateTransition to apply
     */
    public void transition(final StateTransition<T> stateTransition) {
        // Transform synchronous stateTransition into async stateTransition
        transitionSubject.onNext(currentState -> Single.just(stateTransition.applyChange(currentState)));
    }

    /**
     * Add a new async event to the state machine event subject.
     * @param asyncStateTransition - asynchronous event to apply
     */
    public void transitionAsync(final AsyncStateTransition<T> asyncStateTransition) {
        transitionSubject.onNext(asyncStateTransition);
    }

    /**
     * Get the current state of the state machine at any moment.
     * @return Observable state
     */
    public Observable<T> observeCurrentState() {
        return stateSubject.observeOn(schedulerProvider.mainThread());
    }

    public T getCurrentState() {
        return stateSubject.getValue();
    }
}
