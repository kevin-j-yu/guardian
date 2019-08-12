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

import static org.junit.Assert.assertEquals;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.model.map.LatLngBounds;
import ai.rideos.api.route.v1.RouteProto.RouteResponse;
import ai.rideos.api.route.v1.RouteProto.RouteResponse.Path;
import ai.rideos.api.timestamp.v1.TimestampProto.Duration;
import java.util.Arrays;
import org.junit.Test;

public class PathsTest {
    @Test
    public void testGetBoundsForPath() {
        final LatLngBounds expectedBounds = new LatLngBounds(
            new LatLng(0, 0),
            new LatLng(2, 2)
        );

        final LatLngBounds actualBounds = Paths.getBoundsForPath(Arrays.asList(
            new LatLng(
                expectedBounds.getSouthwestCorner().getLatitude(),
                expectedBounds.getSouthwestCorner().getLongitude()
            ),
            new LatLng(
                expectedBounds.getNortheastCorner().getLatitude(),
                expectedBounds.getNortheastCorner().getLongitude()
            )
        ));

        assertEquals(expectedBounds.getSouthwestCorner(), actualBounds.getSouthwestCorner());
        assertEquals(expectedBounds.getNortheastCorner(), actualBounds.getNortheastCorner());
    }

    @Test
    public void testGetRouteInfoFromRideOsRoute() {
        final LatLng origin = new LatLng(0, 0);
        final LatLng destination = new LatLng(1, 1);
        final long durationMilli = 4000;
        final double distanceMeters = 1000;
        final RouteResponse response = RouteResponse.newBuilder()
            .setPath(
                Path.newBuilder()
                    .addGeometry(Locations.toRideOsPosition(origin))
                    .addGeometry(Locations.toRideOsPosition(destination))
                    .setTravelTime(Duration.newBuilder().setMilliseconds(durationMilli))
                    .setDistanceMeters(distanceMeters)
            )
            .build();

        final RouteInfoModel expectedModel = new RouteInfoModel(
            Arrays.asList(origin, destination),
            durationMilli,
            distanceMeters
        );

        assertEquals(expectedModel, Paths.getRouteInfoFromRideOsRoute(response));
    }
}
