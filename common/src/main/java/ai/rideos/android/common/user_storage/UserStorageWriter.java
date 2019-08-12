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
 * UserStorageWriter writes user preferences to an arbitrary key-value store
 */
public interface UserStorageWriter {
    /**
     * Store a key-string pair as a user preference. These can be things like the user's preferred fleet ID or API env.
     */
    void storeStringPreference(final StorageKey<String> key, final String value);

    /**
     * Store a key-string pair as a user preference. These can be things like feature flags.
     */
    void storeBooleanPreference(final StorageKey<Boolean> key, final boolean value);

    /**
     * Clear all user related information including credentials and preferences from the storage.
     */
    void clearStorage();
}
