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
package ai.rideos.android.common.app.menu_navigator;

import ai.rideos.android.common.model.MenuOption;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuOptionFragmentRegistry {
    public interface FragmentConstructor {
        Fragment construct();
    }

    private final MenuOption homeOption;
    private final List<MenuOption> menuOptions = new ArrayList<>();
    private final Map<Integer, FragmentConstructor> constructorsByOptionId = new HashMap<>();

    public MenuOptionFragmentRegistry(final MenuOption homeOption,
                                      final FragmentConstructor homeFragmentConstructor) {
        this.homeOption = homeOption;
        registerOption(homeOption, homeFragmentConstructor);
    }

    public MenuOptionFragmentRegistry registerOption(final MenuOption menuOption,
                                                     final FragmentConstructor fragmentConstructor) {
        menuOptions.add(menuOption);
        constructorsByOptionId.put(menuOption.getId(), fragmentConstructor);
        return this;
    }

    public MenuOption getHomeOption() {
        return homeOption;
    }

    public FragmentConstructor getConstructorForOption(final MenuOption menuOption) {
        return constructorsByOptionId.get(menuOption.getId());
    }

    public List<MenuOption> getMenuOptions() {
        return menuOptions;
    }
}
