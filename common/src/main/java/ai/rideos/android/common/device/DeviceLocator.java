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
package ai.rideos.android.common.device;

import ai.rideos.android.common.model.LocationAndHeading;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface DeviceLocator {
    Observable<LocationAndHeading> observeCurrentLocation(final int pollIntervalMillis);

    /**
     * Rather than polling, return the last recorded location of the device.
     * @return a single coordinate
     */
    Single<LocationAndHeading> getLastKnownLocation();
}
