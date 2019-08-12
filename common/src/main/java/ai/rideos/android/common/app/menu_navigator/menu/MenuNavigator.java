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
package ai.rideos.android.common.app.menu_navigator.menu;

import ai.rideos.android.common.app.menu_navigator.MenuOptionFragmentRegistry;
import ai.rideos.android.common.model.MenuOption;
import ai.rideos.android.common.view.BackPropagator;
import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MenuNavigator {
    private final MenuOptionFragmentRegistry registry;
    private final MenuPresenter menuViewController;
    private final FragmentManager fragmentManager;
    @IdRes
    private final int fragmentContainer;

    private MenuOption currentOption;

    public MenuNavigator(final MenuOptionFragmentRegistry registry,
                         final MenuPresenter menuViewController,
                         final FragmentManager fragmentManager,
                         @IdRes final int fragmentContainer) {
        this.registry = registry;
        this.fragmentManager = fragmentManager;
        this.fragmentContainer = fragmentContainer;
        this.menuViewController = menuViewController;
        menuViewController.setSelectionListener(this::selectedMenuOption);
        menuViewController.setSelectedOption(registry.getHomeOption());
    }

    /**
     * The back behavior implemented here does not use a simple back-stack. When a user navigates away from the home
     * option, the back button always goes back to the home option. For example, if the user navigates
     * home -> account settings -> developer settings -> account settings, then hits "back", they will just go home.
     * When back is clicked and the user is home, the system handles the back button.
     */
    public void propagateBackSignal(final Runnable onBackNotHandled) {
        if (currentOption == null) {
            onBackNotHandled.run();
        }

        if (currentOption.getId() == registry.getHomeOption().getId()) {
            final Fragment currentFragment = fragmentManager.findFragmentById(fragmentContainer);
            if (currentFragment instanceof BackPropagator) {
                ((BackPropagator) currentFragment).propagateBackSignal();
            } else {
                onBackNotHandled.run();
            }
        } else {
            menuViewController.setSelectedOption(registry.getHomeOption());
        }
    }

    private void selectedMenuOption(final MenuOption menuOption) {
        if (currentOption != null && menuOption.getId() == currentOption.getId()) {
            return;
        }
        currentOption = menuOption;
        final Fragment fragmentToAdd = registry.getConstructorForOption(menuOption).construct();
        fragmentManager.beginTransaction()
            .replace(fragmentContainer, fragmentToAdd)
            .commit();
    }
}
