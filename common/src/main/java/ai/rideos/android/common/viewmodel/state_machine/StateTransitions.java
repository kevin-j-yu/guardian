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

import java.util.function.Predicate;

public class StateTransitions {

    public static <T> AsyncStateTransition<T> asyncTransitionIf(final Predicate<T> stateMatcher, final AsyncStateTransition<T> event) {
        return currentState -> {
            if (stateMatcher.test(currentState)) {
                return event.applyAsyncChange(currentState);
            }
            throw new InvalidStateTransition(String.format(
                "Invalid state transition. Current state %s does not match predicate %s",
                currentState.toString(),
                stateMatcher.toString()
            ));
        };
    }

    public static <T> StateTransition<T> transitionIf(final Predicate<T> stateMatcher, final StateTransition<T> stateTransition) {
        return currentState -> {
            if (stateMatcher.test(currentState)) {
                return stateTransition.applyChange(currentState);
            }
            throw new InvalidStateTransition(String.format(
                "Invalid state transition. Current state %s does not match predicate %s",
                currentState.toString(),
                stateMatcher.toString()
            ));
        };
    }
}
