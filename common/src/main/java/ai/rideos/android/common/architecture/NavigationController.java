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
package ai.rideos.android.common.architecture;

import java.io.Serializable;

/**
 * NavigationController defines an interface for displaying view controllers. This can be used for Fragments or
 * view-based View Controllers.
 */
public interface NavigationController {
    <Args extends Serializable, Listener, I extends Listener> void navigateTo(
        final ViewController<Args, Listener> viewController,
        final Args input,
        final I listenerInstance);

    ViewController getActiveViewController();
}
