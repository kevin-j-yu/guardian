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
package ai.rideos.android.common.user_storage;

/**
 * StorageKeys has common keys used across both apps.
 */
public class StorageKeys {
    public static final StorageKey<String> FLEET_ID = new StorageKey<>("fleet_id", "");
    public static final StorageKey<String> RIDEOS_API_ENV
        = new StorageKey<>("rideos_api_env", ApiEnvironment.PRODUCTION.getStoredName());
    public static final StorageKey<String> PREFERRED_NAME = new StorageKey<>("preferred_name", "");
}
