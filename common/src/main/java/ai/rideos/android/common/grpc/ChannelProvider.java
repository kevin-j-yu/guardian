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
package ai.rideos.android.common.grpc;

import ai.rideos.android.common.user_storage.ApiEnvironment;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import android.content.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.function.Supplier;

public class ChannelProvider {
    public static Supplier<ManagedChannel> getChannelSupplierForContext(final Context context) {
        final UserStorageReader userStorage = SharedPreferencesUserStorageReader.forContext(context);
        return getChannelSupplierForUser(userStorage);
    }

    public static Supplier<ManagedChannel> getChannelSupplierForUser(final UserStorageReader userStorage) {
        final ApiEnvironment environment = ApiEnvironment.fromStoredNameOrThrow(
            userStorage.getStringPreference(StorageKeys.RIDEOS_API_ENV)
        );
        return () -> ManagedChannelBuilder
            .forTarget(environment.getEndpoint())
            .useTransportSecurity()
            .build();
    }
}
