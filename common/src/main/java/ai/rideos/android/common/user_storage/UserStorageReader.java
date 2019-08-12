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

import io.reactivex.Observable;

/**
 * UserStorageReader reads user preferences from some arbitrary key-value store
 */
public interface UserStorageReader {
    /**
     * Retrieve a preferred string value by its key.
     */
    String getStringPreference(final StorageKey<String> key);

    /**
     * Observe a preferred string value by its key.
     */
    Observable<String> observeStringPreference(final StorageKey<String> key);

    /**
     * Retrieve a preferred boolean value by its key.
     */
    boolean getBooleanPreference(final StorageKey<Boolean> key);

    /**
     * Observe a preferred boolean value by its key.
     */
    Observable<Boolean> observeBooleanPreference(final StorageKey<Boolean> key);
}
