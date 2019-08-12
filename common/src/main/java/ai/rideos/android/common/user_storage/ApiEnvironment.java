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
 * Each ApiEnvironment value contains a stored name and an API endpoint. The stored name can be used in key-value
 * stores to save a user's preference. This should be unchanged, whereas the API endpoint for each value can be updated
 * as the URLs change.
 */
public enum ApiEnvironment {
    DEVELOPMENT("dev", "gapi.development.rideos.ai"),
    STAGING("staging", "gapi.staging.rideos.ai"),
    PRODUCTION("prod", "gapi.rideos.ai");

    private final String storedName;
    private final String endpoint;

    public static ApiEnvironment fromStoredNameOrThrow(final String name) {
        for (final ApiEnvironment env : ApiEnvironment.values()) {
            if (env.getStoredName().equals(name)) {
                return env;
            }
        }
        throw new RuntimeException("Invalid environment: " + name);
    }

    ApiEnvironment(final String storedName, final String endpoint) {
        this.storedName = storedName;
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getStoredName() {
        return storedName;
    }
}
