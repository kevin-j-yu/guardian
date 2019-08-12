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
package ai.rideos.android.common.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import androidx.annotation.Nullable;

public class MetadataReader {
    public static class MetadataResult<T> {
        private final String key;
        @Nullable
        private final T value;

        private MetadataResult(final String key, @Nullable final T value) {
            this.key = key;
            this.value = value;
        }

        public T getOrDefault(final T defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        public T getOrThrow() {
            if (value == null) {
                throw new RuntimeException("Key not found: " + key);
            }
            return value;
        }
    }

    private final Context context;

    public MetadataReader(final Context context) {
        this.context = context;
    }

    public MetadataResult<String> getStringMetadata(final String key) {
        try {
            final Bundle metadata = getMetadataFromContext();
            final String maybeValue = metadata.getString(key);
            return new MetadataResult<>(key, maybeValue);
        } catch (NameNotFoundException e) {
            return new MetadataResult<>(key, null);
        }
    }

    public MetadataResult<Boolean> getBooleanMetadata(final String key) {
        try {
            final Bundle metadata = getMetadataFromContext();
            final Boolean maybeValue = metadata.getBoolean(key);
            return new MetadataResult<>(key, maybeValue);
        } catch (NameNotFoundException e) {
            return new MetadataResult<>(key, null);
        }
    }

    private Bundle getMetadataFromContext() throws NameNotFoundException {
        final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
            context.getPackageName(),
            PackageManager.GET_META_DATA
        );
        return appInfo.metaData;
    }
}
