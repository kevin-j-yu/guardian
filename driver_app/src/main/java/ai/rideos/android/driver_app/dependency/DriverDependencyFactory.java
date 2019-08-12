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

import ai.rideos.android.common.app.dependency.CommonDependencyFactory;
import ai.rideos.android.common.app.menu_navigator.MenuOptionFragmentRegistry;
import ai.rideos.android.common.interactors.mapbox.MapboxApiInteractor;
import ai.rideos.android.interactors.DriverPlanInteractor;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import android.content.Context;

public interface DriverDependencyFactory extends CommonDependencyFactory {
    DriverPlanInteractor getDriverPlanInteractor(final Context context);

    DriverVehicleInteractor getDriverVehicleInteractor(final Context context);

    MapboxApiInteractor getMapboxApiInteractor(final Context context);

    MenuOptionFragmentRegistry getMenuOptions(final Context context);
}
