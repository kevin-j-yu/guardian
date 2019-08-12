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
package ai.rideos.android.driver_app.dependency;

import ai.rideos.android.common.app.CommonMetadataKeys;
import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.common.app.dependency.DefaultCommonDependencyFactory;
import ai.rideos.android.common.app.menu_navigator.DefaultMenuOptions;
import ai.rideos.android.common.app.menu_navigator.MenuOptionFragmentRegistry;
import ai.rideos.android.common.app.menu_navigator.account_settings.AccountSettingsFragment;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.grpc.ChannelProvider;
import ai.rideos.android.common.interactors.mapbox.MapboxApiInteractor;
import ai.rideos.android.common.model.MenuOption;
import ai.rideos.android.driver_app.MainFragment;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.menu.developer_options.DriverDeveloperOptionsFragment;
import ai.rideos.android.interactors.DefaultDriverPlanInteractor;
import ai.rideos.android.interactors.DefaultDriverVehicleInteractor;
import ai.rideos.android.interactors.DriverPlanInteractor;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import android.content.Context;

public class DefaultDriverDependencyFactory extends DefaultCommonDependencyFactory implements DriverDependencyFactory {
    @Override
    public DriverPlanInteractor getDriverPlanInteractor(final Context context) {
        return new DefaultDriverPlanInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
    }

    @Override
    public DriverVehicleInteractor getDriverVehicleInteractor(final Context context) {
        return new DefaultDriverVehicleInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
    }

    @Override
    public MapboxApiInteractor getMapboxApiInteractor(final Context context) {
        return new MapboxApiInteractor(context);
    }

    @Override
    public MenuOptionFragmentRegistry getMenuOptions(final Context context) {
        final MenuOptionFragmentRegistry registry = new MenuOptionFragmentRegistry(
            new MenuOption(
                DefaultMenuOptions.HOME.getId(),
                context.getString(R.string.home_menu_option_title),
                R.drawable.ic_home_24dp
            ),
            MainFragment::new
        )
            .registerOption(
                new MenuOption(
                    DefaultMenuOptions.ACCOUNT_SETTINGS.getId(),
                    context.getString(R.string.account_settings_menu_option_title),
                    R.drawable.ic_person_24dp
                ),
                AccountSettingsFragment::new
            );
        final boolean shouldShowDeveloperOptions = new MetadataReader(context)
            .getBooleanMetadata(CommonMetadataKeys.ENABLE_DEVELOPER_OPTIONS_KEY)
            .getOrDefault(false);
        if (shouldShowDeveloperOptions) {
            registry.registerOption(
                new MenuOption(
                    DefaultMenuOptions.DEVELOPER_OPTIONS.getId(),
                    context.getString(R.string.developer_options_menu_option_title),
                    R.drawable.ic_settings
                ),
                DriverDeveloperOptionsFragment::new
            );
        }
        return registry;
    }
}
