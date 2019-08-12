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
package ai.rideos.android.interactors;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.VehiclePosition;
import io.reactivex.Observable;
import java.util.List;

public interface PreviewVehicleInteractor {
    /**
     * Retrieves a preview of the vehicles in an area. There is no guarantee these vehicle ids match the actual backend
     * objects.
     */
    Observable<List<VehiclePosition>> getVehiclesInVicinity(final LatLng center, final String fleetId);

    void shutDown();
}
