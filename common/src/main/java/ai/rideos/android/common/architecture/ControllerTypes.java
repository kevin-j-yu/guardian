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

public class ControllerTypes<Args extends Serializable, Listener> {
    private Class<Args> argsType;
    private Class<Listener> listenerType;

    public ControllerTypes(final Class<Args> argsType, final Class<Listener> listenerType) {
        this.argsType = argsType;
        this.listenerType = listenerType;
    }

    public Class<Args> getArgsType() {
        return argsType;
    }

    public Class<Listener> getListenerType() {
        return listenerType;
    }
}
