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

import android.os.Bundle;
import java.io.Serializable;

/**
 * Helper class to create and read fragment bundles given args and a listener.
 */
public class Bundler<Args extends Serializable, Listener> {
    private static final String ARGUMENTS_KEY = "args";
    private static final String LISTENER_KEY = "listener";

    private final ViewController<Args, Listener> viewController;
    private ListenerRegistry listenerRegistry;

    public Bundler(final ViewController<Args, Listener> viewController,
                   final ListenerRegistry listenerRegistry) {
        this.viewController = viewController;
        this.listenerRegistry = listenerRegistry;
    }

    /**
     * Write an argument and listener to a bundle. The argument is stored as a serializable object. The coordinator
     * is stored by its class name.
     */
    public <I extends Listener> Bundle createBundle(final Args args, final I listenerInstance) {
        listenerRegistry.registerListenerForViewController(viewController, listenerInstance);
        final Bundle bundle = new Bundle();
        bundle.putSerializable(ARGUMENTS_KEY, args);
        bundle.putString(LISTENER_KEY, listenerInstance.getClass().getName());
        return bundle;
    }

    /**
     * Retrieve the listener from a bundle. The listener key is used to retrieve the class that will be listening
     * to the fragment.
     */
    public Listener getListener(final Bundle bundle) {
        final String listener = bundle.getString(LISTENER_KEY);
        return listenerRegistry.getListenerForViewController(viewController, listener);
    }

    /**
     * Get the arguments from a bundle.
     */
    public Args getArgs(final Bundle bundle) {
        return viewController.getTypes().getArgsType().cast(bundle.getSerializable(ARGUMENTS_KEY));
    }
}
