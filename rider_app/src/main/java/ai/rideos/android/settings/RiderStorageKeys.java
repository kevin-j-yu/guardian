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
package ai.rideos.android.settings;

import ai.rideos.android.common.user_storage.StorageKey;

public class RiderStorageKeys {
    public static final StorageKey<Boolean> MANUAL_VEHICLE_SELECTION
        = new StorageKey<>("manual_vehicle_selection", false);
    public static final StorageKey<String> TRIP_STATE = new StorageKey<>("trip_state", "");
}
