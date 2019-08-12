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

import ai.rideos.android.common.app.dependency.CommonDependencyFactory;
import ai.rideos.android.common.app.menu_navigator.MenuOptionFragmentRegistry;
import ai.rideos.android.interactors.AvailableVehicleInteractor;
import ai.rideos.android.interactors.HistoricalSearchInteractor;
import ai.rideos.android.interactors.PreviewVehicleInteractor;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.interactors.RiderTripStateInteractor;
import ai.rideos.android.interactors.StopInteractor;
import android.content.Context;

public interface RiderDependencyFactory extends CommonDependencyFactory {
    AvailableVehicleInteractor getAvailableVehicleInteractor(final Context context);

    RiderTripStateInteractor getTripStateInteractor(final Context context);

    PreviewVehicleInteractor getPreviewVehicleInteractor(final Context context);

    StopInteractor getStopInteractor(final Context context);

    HistoricalSearchInteractor getHistoricalSearchInteractor(final Context context);

    RiderTripInteractor getTripInteractor(final Context context);

    MenuOptionFragmentRegistry getMenuOptions(final Context context);
}
