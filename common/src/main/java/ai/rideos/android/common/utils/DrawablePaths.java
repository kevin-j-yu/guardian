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
package ai.rideos.android.common.utils;

import ai.rideos.android.common.R;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.view.resources.ResourceProvider;
import java.util.List;

public class DrawablePaths {
    public static final float DEFAULT_PATH_WIDTH = 10.0f;

    public static DrawablePath getActivePath(final List<LatLng> route, final ResourceProvider resourceProvider) {
        return new DrawablePath(route, DEFAULT_PATH_WIDTH, resourceProvider.getColor(R.attr.rideos_route_color));
    }

    public static DrawablePath getInactivePath(final List<LatLng> route, final ResourceProvider resourceProvider) {
        return new DrawablePath(route, DEFAULT_PATH_WIDTH, resourceProvider.getColor(R.attr.rideos_inactive_route_color));
    }
}
