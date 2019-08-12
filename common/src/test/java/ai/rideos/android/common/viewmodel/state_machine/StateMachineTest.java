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

import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;

public class StateMachineTest {
    private enum TestState {
        STATE_1,
        STATE_2
    }

    private StateMachine<TestState> stateMachineUnderTest;

    @Before
    public void setUp() {
        stateMachineUnderTest = new StateMachine<>(TestState.STATE_1, new TrampolineSchedulerProvider());
        stateMachineUnderTest.start();
    }

    @Test
    public void testInitialStateIsEmitted() {
        stateMachineUnderTest.observeCurrentState().test()
            .assertValueCount(1)
            .assertValueAt(0, TestState.STATE_1);
    }

    @Test
    public void testCanApplySynchronousEvents() {
        stateMachineUnderTest.transition(state -> TestState.STATE_2);
        stateMachineUnderTest.observeCurrentState().test()
            .assertValueCount(1)
            .assertValueAt(0, TestState.STATE_2);
    }

    @Test
    public void testCanApplyAsynchronousEvents() {
        stateMachineUnderTest.transitionAsync(state -> Single.just(TestState.STATE_2));
        stateMachineUnderTest.observeCurrentState().test()
            .assertValueCount(1)
            .assertValueAt(0, TestState.STATE_2);
    }

    @Test
    public void testOnlyMostRecentStateIsCached() {
        stateMachineUnderTest.transition(state -> TestState.STATE_2);
        stateMachineUnderTest.transition(state -> TestState.STATE_1);
        stateMachineUnderTest.observeCurrentState().test()
            .assertValueCount(1)
            .assertValueAt(0, TestState.STATE_1);
    }
}
