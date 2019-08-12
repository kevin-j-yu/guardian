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
 * ViewController defines an object that takes in some arguments and calls the given listener as the user interacts
 * with a View.
 * @param <Args> - input to view controller
 * @param <Listener> - listener that is called after various UI events
 */
public interface ViewController<Args extends Serializable, Listener> {
    ControllerTypes<Args, Listener> getTypes();
}
