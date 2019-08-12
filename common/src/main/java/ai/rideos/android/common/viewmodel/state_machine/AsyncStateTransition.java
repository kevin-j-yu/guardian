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

import io.reactivex.Single;

/**
 * AsyncStateTransition is a type of event that changes the state after some asynchronous call. This is useful in situations where
 * the state doesn't change immediately. E.g. you request a driver goes online, but this request takes some time to
 * complete
 * @param <State>
 */
public interface AsyncStateTransition<State> {

    Single<State> applyAsyncChange(final State currentState) throws InvalidStateTransition;

}
