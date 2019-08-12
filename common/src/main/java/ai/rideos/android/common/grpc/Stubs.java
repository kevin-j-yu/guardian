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

import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;

public class Stubs {
    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    public static <T extends AbstractStub<T>> T withAuthorization(final T stub, final String token) {
        final Metadata authHeaders = new Metadata();
        authHeaders.put(AUTHORIZATION_METADATA_KEY, AUTHORIZATION_PREFIX + token);
        return MetadataUtils.attachHeaders(stub, authHeaders);
    }
}
