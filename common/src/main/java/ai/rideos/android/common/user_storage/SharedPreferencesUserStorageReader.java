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

import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.content.Context;
import android.content.SharedPreferences;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import io.reactivex.Observable;

public class SharedPreferencesUserStorageReader implements UserStorageReader {
    private final RxSharedPreferences rxSharedPreferences;
    private final SchedulerProvider schedulerProvider;

    private SharedPreferencesUserStorageReader(final Context context, final SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
        final SharedPreferences androidStorage = context.getSharedPreferences(USER_STORAGE_FILE, Context.MODE_PRIVATE);
        rxSharedPreferences = RxSharedPreferences.create(androidStorage);
    }

    public static SharedPreferencesUserStorageReader forContext(final Context context) {
        return new SharedPreferencesUserStorageReader(context, new DefaultSchedulerProvider());
    }

    @Override
    public String getStringPreference(final StorageKey<String> key) {
        return rxSharedPreferences.getString(key.getKey(), key.getDefaultValue()).get();
    }

    @Override
    public Observable<String> observeStringPreference(final StorageKey<String> key) {
        return rxSharedPreferences.getString(key.getKey(), key.getDefaultValue()).asObservable()
            .subscribeOn(schedulerProvider.io());
    }

    @Override
    public boolean getBooleanPreference(final StorageKey<Boolean> key) {
        return rxSharedPreferences.getBoolean(key.getKey(), key.getDefaultValue()).get();
    }

    @Override
    public Observable<Boolean> observeBooleanPreference(final StorageKey<Boolean> key) {
        return rxSharedPreferences.getBoolean(key.getKey(), key.getDefaultValue()).asObservable()
            .subscribeOn(schedulerProvider.io());
    }
}
