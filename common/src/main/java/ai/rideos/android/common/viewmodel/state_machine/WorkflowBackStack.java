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

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import java.util.OptionalInt;
import java.util.Stack;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * WorkflowBackStack is a back stack that keeps track of states in a sequential workflow. As new states are emitted
 * from a state machine, they are added to the back stack. Calling `back` pushes the previous state to the state machine.
 * Calling `clear` removes all states.
 *
 * You can also specify that the WorkflowBackStack "rolls back" states in the stack when it notices a previously emitted
 * state. For example, say you have a workflow with the sequence A -> B -> C and the following events occurred:
 * - Started state machine on state A (back stack : [A])
 * - Navigated from state A to state B (back stack : [A, B])
 * - Navigated from state B to state C (back stack : [A, B, C])
 * Now, if you navigate "up" from state C to state A, the WorkflowBackStack rolls back the back stack to [A].
 *
 * Additionally, the WorkflowBackStack can track states in which the back button is disabled. For example, if there is
 * an intermediate loading screen, you probably don't want the user to hit back when some background task is being
 * executed.
 * @param <T> - type of states to track
 */
public class WorkflowBackStack<T> implements BackStack<T> {
    private final BiPredicate<T, T> stateComparer;
    private final Predicate<T> disabledBackStates;
    private final PublishSubject<Event<T>> eventSubject = PublishSubject.create();
    private final Stack<T> previousStates = new Stack<>();

    /**
     * Create a new WorkflowBackStack
     * @param stateComparer - function to call to check if a state already exists in the back stack
     * @param disabledBackStates - function to call to check if a state can be removed from the back stack
     */
    public WorkflowBackStack(final BiPredicate<T, T> stateComparer,
                             final Predicate<T> disabledBackStates) {
        this.stateComparer = stateComparer;
        this.disabledBackStates = disabledBackStates;
    }

    @Override
    public Disposable follow(final StateMachine<T> stateMachine) {
        final CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            eventSubject.subscribe(event -> {
                switch (event.eventType) {
                    case NEW_STATE:
                        checkForExistingStateAndPop(event.newState);
                        previousStates.push(event.newState);
                        return;
                    case BACK:
                        if (!previousStates.isEmpty() && disabledBackStates.test(previousStates.peek())) {
                            return;
                        }
                        if (previousStates.size() < 2) {
                            event.onBackStackEmpty.run();
                        } else {
                            previousStates.pop();
                            stateMachine.transition(state -> previousStates.peek());
                        }
                        return;
                    case CLEAR:
                        previousStates.clear();
                }
            })
        );

        compositeDisposable.add(
            stateMachine.observeCurrentState()
                .subscribe(newState -> eventSubject.onNext(Event.newState(newState)))
        );

        return compositeDisposable;
    }

    @Override
    public void back(final Runnable onBackStackEmpty) {
        eventSubject.onNext(Event.back(onBackStackEmpty));
    }

    @Override
    public void clear() {
        eventSubject.onNext(Event.clear());
    }

    private void checkForExistingStateAndPop(final T newState) {
        final OptionalInt previousIndex = IntStream.range(0, previousStates.size())
            .filter(i -> stateComparer.test(previousStates.get(previousStates.size() - i - 1), newState))
            .findFirst();
        if (previousIndex.isPresent()) {
            final int numToPop = previousIndex.getAsInt() + 1;
            for (int i = 0; i < numToPop; i++) {
                previousStates.pop();
            }
        }
    }

    private static class Event<T> {
        private enum EventType {
            NEW_STATE,
            BACK,
            CLEAR
        }

        private final EventType eventType;
        private final T newState;
        private final Runnable onBackStackEmpty;

        private static <T> Event<T> newState(final T newState) {
            return new Event<>(EventType.NEW_STATE, newState, null);
        }

        private static <T> Event<T> back(final Runnable onBackStackEmpty) {
            return new Event<>(EventType.BACK, null, onBackStackEmpty);
        }

        private static <T> Event<T> clear() {
            return new Event<>(EventType.CLEAR, null, null);
        }

        private Event(final EventType eventType,
                      final T newState,
                      final Runnable onBackStackEmpty) {
            this.eventType = eventType;
            this.newState = newState;
            this.onBackStackEmpty = onBackStackEmpty;
        }
    }
}
