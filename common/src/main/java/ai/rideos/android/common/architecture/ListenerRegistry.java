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

import com.google.common.collect.MutableClassToInstanceMap;
import java.util.HashMap;
import java.util.Map;

/**
 * ListenerRegistry registers global listeners by the parent class name. This is crucial for using Fragments because
 * they cannot take functions/listeners in as arguments.
 *
 * To use this, a Coordinator or View Model would register itself as a listener when the app first starts up. The listener is stored
 * and identified by the class name of the listener instance. When using Fragments, the navigation controller could then pass in the
 * class name identifier into the arguments of a Fragment.
 *
 * When the Fragment starts, it would then read the identifier of the listener and look it up in the registry. When a
 * Fragment is recycled, it would just re-read this identifier. Fragments can be re-used by different Coordinators by
 * passing in different identifiers of the listener.
 */
public class ListenerRegistry {
    private static final ListenerRegistry instance = new ListenerRegistry();

    private Map<String, MutableClassToInstanceMap<Object>> listenerMap = new HashMap<>();

    private ListenerRegistry() {
    }

    public static ListenerRegistry get() {
        return instance;
    }

    /**
     * Register a the listener for a view controller
     */
    public <L, I extends L> void registerListenerForViewController(final ViewController<?, L> viewController,
                                                                   final I listenerInstance) {
        final MutableClassToInstanceMap<Object> classMap = getOrCreateClassMap(listenerInstance.getClass().getName());
        classMap.put(viewController.getTypes().getListenerType(), listenerInstance);
    }

    /**
     * Get a listener for a particular view controller.
     */
    public <L> L getListenerForViewController(final ViewController<?, L> viewController,
                                              final String id) {
        final MutableClassToInstanceMap<Object> classMap = getOrCreateClassMap(id);
        return classMap.getInstance(viewController.getTypes().getListenerType());
    }

    private MutableClassToInstanceMap<Object> getOrCreateClassMap(final String id) {
        final MutableClassToInstanceMap<Object> classMap = listenerMap.get(id);
        if (classMap == null) {
            final MutableClassToInstanceMap<Object> createdMap = MutableClassToInstanceMap.create();
            listenerMap.put(id, createdMap);
            return createdMap;
        }
        return classMap;
    }
}
