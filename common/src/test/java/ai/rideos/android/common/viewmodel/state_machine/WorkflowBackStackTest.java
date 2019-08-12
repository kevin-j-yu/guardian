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

import static junit.framework.TestCase.assertEquals;

import io.reactivex.subjects.ReplaySubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class WorkflowBackStackTest {
    private ReplaySubject<TestState> stateSubject;
    private StateMachine<TestState> stateMachine;
    private WorkflowBackStack<TestState> backStack;
    private Runnable onBackStackEmpty;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        stateSubject = ReplaySubject.create();
        onBackStackEmpty = Mockito.mock(Runnable.class);
        stateMachine = Mockito.mock(StateMachine.class);
        Mockito.when(stateMachine.observeCurrentState()).thenReturn(stateSubject);
        // Mock the behavior of a state machine by adding the new state to the state subject
        Mockito.doAnswer(invocation -> {
            final StateTransition<TestState> transition = (StateTransition) invocation.getArguments()[0];
            stateSubject.onNext(transition.applyChange(new TestState(StateType.TYPE_1, "")));
            return null;
        })
            .when(stateMachine).transition(Mockito.any());

        backStack = new WorkflowBackStack<>(
            (state1, state2) -> state1.stateType == state2.stateType,
            state -> state.stateType == StateType.TYPE_4
        );
        backStack.follow(stateMachine);
    }

    @Test
    public void testCanGoBackInHistoryWhenBackStackNotEmpty() {
        stateSubject.onNext(new TestState(StateType.TYPE_1, "data1"));
        stateSubject.onNext(new TestState(StateType.TYPE_2, "data2"));

        backStack.back(onBackStackEmpty);
        Mockito.verifyNoMoreInteractions(onBackStackEmpty);
        Mockito.verify(stateMachine).transition(Mockito.any());
        assertEquals(stateSubject.getValues().length, 3);
        assertEquals(stateSubject.getValue().stateType, StateType.TYPE_1);
    }

    @Test
    public void testEmptyListenerCalledWhenBackStackEmpty() {
        stateSubject.onNext(new TestState(StateType.TYPE_1, "data1"));

        backStack.back(onBackStackEmpty);
        Mockito.verify(onBackStackEmpty).run();
        Mockito.verify(stateMachine, Mockito.never()).transition(Mockito.any());
        assertEquals(stateSubject.getValues().length, 1);
    }

    @Test
    public void testBackStackClearedWhenDuplicateStateAdded() {
        stateSubject.onNext(new TestState(StateType.TYPE_1, "data1"));
        stateSubject.onNext(new TestState(StateType.TYPE_2, "data2"));
        stateSubject.onNext(new TestState(StateType.TYPE_3, "data3"));

        stateSubject.onNext(new TestState(StateType.TYPE_2, "updated-data"));
        stateSubject.onNext(new TestState(StateType.TYPE_3, "data3"));

        backStack.back(onBackStackEmpty);
        Mockito.verifyNoMoreInteractions(onBackStackEmpty);
        assertEquals(stateSubject.getValue().stateType, StateType.TYPE_2);
        assertEquals(stateSubject.getValue().data, "updated-data");

        backStack.back(onBackStackEmpty);
        Mockito.verifyNoMoreInteractions(onBackStackEmpty);
        assertEquals(stateSubject.getValue().stateType, StateType.TYPE_1);
        assertEquals(stateSubject.getValue().data, "data1");

        backStack.back(onBackStackEmpty);
        Mockito.verify(onBackStackEmpty).run();
    }

    @Test
    public void testStateDoesNotTransitionIfMatchingDisabledStatePredicate() {
        stateSubject.onNext(new TestState(StateType.TYPE_4, "data4"));

        backStack.back(onBackStackEmpty);
        Mockito.verifyNoMoreInteractions(onBackStackEmpty);
        assertEquals(stateSubject.getValues().length, 1);
        assertEquals(stateSubject.getValue().stateType, StateType.TYPE_4);
    }

    @Test
    public void testCanAddIdenticalStateAfterClearing() {
        stateSubject.onNext(new TestState(StateType.TYPE_1, "data1"));

        backStack.clear();

        // Add same state type as before clear. This should be added to the back stack even though the previous state
        // was the same type
        stateSubject.onNext(new TestState(StateType.TYPE_1, "data3"));
        stateSubject.onNext(new TestState(StateType.TYPE_2, "data2"));

        backStack.back(onBackStackEmpty);
        Mockito.verifyNoMoreInteractions(onBackStackEmpty);
        assertEquals(stateSubject.getValues().length, 4);
        assertEquals(stateSubject.getValue().stateType, StateType.TYPE_1);
        assertEquals(stateSubject.getValue().data, "data3");

        // Make sure back-stack was actually cleared
        backStack.back(onBackStackEmpty);
        Mockito.verify(onBackStackEmpty).run();
    }

    private enum StateType {
        TYPE_1,
        TYPE_2,
        TYPE_3,
        TYPE_4
    }

    private static class TestState {
        private final StateType stateType;
        private final String data;

        private TestState(final StateType stateType, final String data) {
            this.stateType = stateType;
            this.data = data;
        }
    }
}
