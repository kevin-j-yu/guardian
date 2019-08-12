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
package ai.rideos.android.rider_app.dependency;

import ai.rideos.android.common.app.CommonMetadataKeys;
import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.common.app.dependency.DefaultCommonDependencyFactory;
import ai.rideos.android.common.app.menu_navigator.DefaultMenuOptions;
import ai.rideos.android.common.app.menu_navigator.MenuOptionFragmentRegistry;
import ai.rideos.android.common.app.menu_navigator.account_settings.AccountSettingsFragment;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.grpc.ChannelProvider;
import ai.rideos.android.common.model.MenuOption;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageWriter;
import ai.rideos.android.interactors.AvailableVehicleInteractor;
import ai.rideos.android.interactors.DefaultAvailableVehicleInteractor;
import ai.rideos.android.interactors.DefaultPreviewVehicleInteractor;
import ai.rideos.android.interactors.DefaultStopInteractor;
import ai.rideos.android.interactors.DefaultRiderTripInteractor;
import ai.rideos.android.interactors.DefaultRiderTripStateInteractor;
import ai.rideos.android.interactors.HistoricalSearchInteractor;
import ai.rideos.android.interactors.PreviewVehicleInteractor;
import ai.rideos.android.interactors.StopInteractor;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.interactors.RiderTripStateInteractor;
import ai.rideos.android.interactors.UserStorageHistoricalSearchInteractor;
import ai.rideos.android.rider_app.MainFragment;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.developer_settings.RiderDeveloperOptionsFragment;
import android.content.Context;

public class DefaultRiderDependencyFactory extends DefaultCommonDependencyFactory implements RiderDependencyFactory {
    @Override
    public AvailableVehicleInteractor getAvailableVehicleInteractor(final Context context) {
        return new DefaultAvailableVehicleInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
    }

    @Override
    public RiderTripStateInteractor getTripStateInteractor(final Context context) {
        return new DefaultRiderTripStateInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context), getRouteInteractor(context));
    }

    @Override
    public PreviewVehicleInteractor getPreviewVehicleInteractor(final Context context) {
        return new DefaultPreviewVehicleInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
    }

    @Override
    public StopInteractor getStopInteractor(final Context context) {
        return new DefaultStopInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
    }

    @Override
    public HistoricalSearchInteractor getHistoricalSearchInteractor(final Context context) {
        return new UserStorageHistoricalSearchInteractor(
            SharedPreferencesUserStorageReader.forContext(context),
            SharedPreferencesUserStorageWriter.forContext(context)
        );
    }

    @Override
    public RiderTripInteractor getTripInteractor(final Context context) {
        return new DefaultRiderTripInteractor(ChannelProvider.getChannelSupplierForContext(context), User.get(context));
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
                RiderDeveloperOptionsFragment::new
            );
        }
        return registry;
    }
}
