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

import ai.rideos.android.common.model.LatLng;
import com.google.maps.android.PolyUtil;
import java.util.List;
import java.util.stream.Collectors;

public class Polylines {
    public interface PolylineDecoder {
        List<LatLng> decode(final String polyline);
    }

    public interface PolylineEncoder {
        String encode(final List<LatLng> path);
    }

    public static class GMSPolylineDecoder implements Polylines.PolylineDecoder {
        @Override
        public List<LatLng> decode(final String polyline) {
            return PolyUtil.decode(polyline).stream()
                .map(Locations::fromGoogleLatLng)
                .collect(Collectors.toList());
        }
    }

    public static class GMSPolylineEncoder implements Polylines.PolylineEncoder {
        @Override
        public String encode(final List<LatLng> path) {
            return PolyUtil.encode(
                path.stream()
                    .map(Locations::toGoogleLatLng)
                    .collect(Collectors.toList())
            );
        }
    }
}
