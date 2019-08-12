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

import static ai.rideos.android.common.user_storage.SharedPreferencesFileNames.USER_STORAGE_FILE;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUserStorageWriter implements UserStorageWriter {
    private final SharedPreferences sharedPreferences;

    private SharedPreferencesUserStorageWriter(final Context context) {
        sharedPreferences = context.getSharedPreferences(USER_STORAGE_FILE, Context.MODE_PRIVATE);
    }

    public static SharedPreferencesUserStorageWriter forContext(final Context context) {
        return new SharedPreferencesUserStorageWriter(context);
    }

    @Override
    public void storeStringPreference(final StorageKey key, final String value) {
        sharedPreferences.edit().putString(key.getKey(), value).apply();
    }

    @Override
    public void storeBooleanPreference(final StorageKey<Boolean> key, final boolean value) {
        sharedPreferences.edit().putBoolean(key.getKey(), value).apply();
    }

    @Override
    public void clearStorage() {
        sharedPreferences.edit().clear().apply();
    }
}
