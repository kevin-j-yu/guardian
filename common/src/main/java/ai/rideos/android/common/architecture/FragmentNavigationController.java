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
import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import java.io.Serializable;

public class FragmentNavigationController implements NavigationController {
    private final FragmentManager fragmentManager;
    @IdRes
    private final int fragmentContainer;
    private final ListenerRegistry listenerRegistry;

    public FragmentNavigationController(final FragmentManager fragmentManager,
                                        @IdRes final int fragmentContainer,
                                        final ListenerRegistry listenerRegistry) {
        this.fragmentManager = fragmentManager;
        this.fragmentContainer = fragmentContainer;
        this.listenerRegistry = listenerRegistry;
    }

    @Override
    public <Args extends Serializable, Listener, I extends Listener> void navigateTo(
        final ViewController<Args, Listener> viewController,
        final Args input,
        final I listenerInstance
    ) {
        if (!(viewController instanceof Fragment)) {
            throw new RuntimeException("Cannot display non-fragment");
        }
        final Fragment fragmentToDisplay = (Fragment) viewController;

        final Bundler<Args, Listener> bundler = new Bundler<>(viewController, listenerRegistry);
        final Bundle args = bundler.createBundle(input, listenerInstance);
        fragmentToDisplay.setArguments(args);
        fragmentManager.beginTransaction()
            .replace(fragmentContainer, fragmentToDisplay)
            .commitAllowingStateLoss();
    }

    @Override
    public <Args extends Serializable, Listener, I extends Listener> void showModal(
        final ViewController<Args, Listener> viewController,
        final Args input,
        final I listenerInstance
    ) {
        if (!(viewController instanceof ModalFragmentViewController)) {
            throw new RuntimeException("Can only display ModalFragmentViewController as modals currently");
        }
        final ModalFragmentViewController fragmentToDisplay = (ModalFragmentViewController) viewController;
        final Bundler<Args, Listener> bundler = new Bundler<>(viewController, listenerRegistry);
        final Bundle args = bundler.createBundle(input, listenerInstance);
        fragmentToDisplay.setArguments(args);
        fragmentToDisplay.show(fragmentManager, "modal_fragment");
    }

    @Override
    public ViewController getActiveViewController() {
        final Fragment fragment = fragmentManager.findFragmentById(fragmentContainer);
        if (fragment instanceof ViewController) {
            return (ViewController) fragment;
        }
        return null;
    }
}
