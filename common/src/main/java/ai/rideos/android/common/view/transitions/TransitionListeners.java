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
package ai.rideos.android.common.view.transitions;

import android.transition.Transition;
import android.transition.Transition.TransitionListener;

/**
 * Create a new TransitionListener that is only triggered at the end of a transition.
 */
public class TransitionListeners {
    public static TransitionListener onEnd(final Runnable onEndListener) {
        return new TransitionListener() {
            @Override
            public void onTransitionStart(final Transition transition) {

            }

            @Override
            public void onTransitionEnd(final Transition transition) {
                onEndListener.run();
            }

            @Override
            public void onTransitionCancel(final Transition transition) {

            }

            @Override
            public void onTransitionPause(final Transition transition) {

            }

            @Override
            public void onTransitionResume(final Transition transition) {

            }
        };
    }
}
