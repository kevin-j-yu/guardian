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
package ai.rideos.android.common.interactors.mapbox;

import ai.rideos.android.common.app.CommonMetadataKeys;
import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.content.Context;
import androidx.annotation.NonNull;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MapboxDirectionsInteractor can be used to call into Mapbox's APIs for map matching and directions. There is no need
 * generalizing this, since it needs to use Mapbox's internal objects.
 */
public class MapboxApiInteractor {
    private final Supplier<MapboxMapMatching.Builder> authenticatedMatchingBuilder;
    private final Supplier<NavigationRoute.Builder> authenticatedNavBuilder;
    private final SchedulerProvider schedulerProvider;

    public MapboxApiInteractor(final Context context) {
        // TODO
        this(context, new MetadataReader(context).getStringMetadata(CommonMetadataKeys.MAPBOX_TOKEN_KEY).getOrThrow());
    }

    public MapboxApiInteractor(final Context context,
                               final String mapBoxApiToken) {
        this(
            () -> MapboxMapMatching.builder().accessToken(mapBoxApiToken),
            () -> NavigationRoute.builder(context).accessToken(mapBoxApiToken),
            new DefaultSchedulerProvider()
        );
    }

    /**
     * Mapbox doesn't supply a "client" object, so authentication needs to happen every time on the builder. For the sake
     * of dependency injection, suppliers of the builders that are pre-authenticated are given to this constructor.
     */
    public MapboxApiInteractor(final Supplier<MapboxMapMatching.Builder> authenticatedMatchingBuilder,
                               final Supplier<NavigationRoute.Builder> authenticatedNavBuilder,
                               final SchedulerProvider schedulerProvider) {
        this.authenticatedMatchingBuilder = authenticatedMatchingBuilder;
        this.authenticatedNavBuilder = authenticatedNavBuilder;
        this.schedulerProvider = schedulerProvider;
    }

    public Observable<DirectionsRoute> matchCoordinatesToDirections(final List<Point> coordinates) {
        final MapboxMapMatching matching = authenticatedMatchingBuilder.get()
            .coordinates(coordinates)
            .waypointIndices(0, coordinates.size() - 1)
            .steps(true)
            .voiceInstructions(true)
            .bannerInstructions(true)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .build();

        return Observable.<DirectionsRoute>create(emitter -> matching.enqueueCall(new Callback<MapMatchingResponse>() {
            @Override
            public void onResponse(@NonNull final Call<MapMatchingResponse> call,
                                   @NonNull final Response<MapMatchingResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().matchings().isEmpty()) {
                    final DirectionsRoute route = response.body().matchings().get(0).toDirectionRoute();
                    emitter.onNext(route);
                } else {
                    emitter.onError(new MapboxException(
                        "Received match response but was unsuccessful: " + response.toString()
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull final Call<MapMatchingResponse> call, @NonNull final Throwable t) {
                emitter.onError(t);
            }
        }))
            .subscribeOn(schedulerProvider.io());
    }

    public Observable<DirectionsRoute> getDirectionsToDestination(final Point origin, final Point destination) {
        return getDirectionsToDestination(origin, destination, Collections.emptyList());
    }

    public Observable<DirectionsRoute> getDirectionsToDestination(final Point origin,
                                                                  final Point destination,
                                                                  final List<Point> waypoints) {
        final NavigationRoute.Builder routeBuilder = authenticatedNavBuilder.get()
            .origin(origin)
            .destination(destination);

        for (final Point waypoint : waypoints) {
            routeBuilder.addWaypoint(waypoint);
        }
        final NavigationRoute route = routeBuilder.build();

        return Observable.<DirectionsRoute>create(emitter -> route.getRoute(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(@NonNull final Call<DirectionsResponse> call,
                                   @NonNull final Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().routes().isEmpty()) {
                    emitter.onNext(response.body().routes().get(0));
                } else {
                    emitter.onError(new MapboxException(
                        "Received route response but was unsuccessful: " + response.toString()
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull final Call<DirectionsResponse> call, @NonNull final Throwable t) {
                emitter.onError(t);
            }
        }))
            .subscribeOn(schedulerProvider.io());
    }

    public static class MapboxException extends Exception {
        public MapboxException(final String message) {
            super(message);
        }
    }
}
