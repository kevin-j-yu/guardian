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
package ai.rideos.android.rider_app.pre_trip.confirm_location;

import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.common.device.FusedLocationDeviceLocator;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.settings.RiderMetadataKeys;
import android.content.Context;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class ConfirmLocationViewModelBuilder {
    public static ConfirmLocationViewModel buildPickupViewModel(final Context context,
                                                                final Consumer<DesiredAndAssignedLocation> listener,
                                                                final @Nullable NamedTaskLocation initialLocation) {
        if (areLocationsFixed(context)) {
            return new FixedLocationConfirmLocationViewModel(
                listener,
                RiderDependencyRegistry.riderDependencyFactory().getStopInteractor(context),
                RiderDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(context),
                new FusedLocationDeviceLocator(context),
                ResolvedFleet.get().observeFleetInfo(),
                AndroidResourceProvider.forContext(context),
                R.drawable.ic_pin,
                AndroidResourceProvider.forContext(context).getDrawableId(R.attr.rideos_pickup_pin),
                initialLocation == null ? null : initialLocation.getLocation().getLatLng()
            );
        } else {
            return new DefaultConfirmLocationViewModel(
                RiderDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(context),
                new FusedLocationDeviceLocator(context),
                listener,
                AndroidResourceProvider.forContext(context),
                initialLocation,
                R.drawable.ic_pin
            );
        }
    }

    public static ConfirmLocationViewModel buildDropOffViewModel(final Context context,
                                                                 final Consumer<DesiredAndAssignedLocation> listener,
                                                                 final @Nullable NamedTaskLocation initialLocation) {
        if (areLocationsFixed(context)) {
            return new FixedLocationConfirmLocationViewModel(
                listener,
                RiderDependencyRegistry.riderDependencyFactory().getStopInteractor(context),
                RiderDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(context),
                new FusedLocationDeviceLocator(context),
                ResolvedFleet.get().observeFleetInfo(),
                AndroidResourceProvider.forContext(context),
                R.drawable.ic_pin_destination,
                AndroidResourceProvider.forContext(context).getDrawableId(R.attr.rideos_drop_off_pin),
                initialLocation == null ? null : initialLocation.getLocation().getLatLng()
            );
        } else {
            return new DefaultConfirmLocationViewModel(
                RiderDependencyRegistry.mapDependencyFactory().getGeocodeInteractor(context),
                new FusedLocationDeviceLocator(context),
                listener,
                AndroidResourceProvider.forContext(context),
                initialLocation,
                R.drawable.ic_pin_destination
            );
        }
    }

    private static boolean areLocationsFixed(final Context context) {
        final MetadataReader metadataReader = new MetadataReader(context);
        return metadataReader.getBooleanMetadata(RiderMetadataKeys.ENABLE_FIXED_LOCATIONS_METADATA_KEY)
            .getOrDefault(false)
            .equals(true);
    }
}
