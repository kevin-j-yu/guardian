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
package ai.rideos.android.rider_app.deeplink;

import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

public class KnownAppSchemes {
    private static final Map<String, String> playStoreUrisByScheme = ImmutableMap.<String, String>builder()
        .put("slack", "https://play.google.com/store/apps/details?id=com.Slack")
        .build();

    public static Optional<String> getPlayStoreLinkFromScheme(@Nullable final String scheme) {
        if (scheme == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playStoreUrisByScheme.getOrDefault(scheme, null));
    }
}
