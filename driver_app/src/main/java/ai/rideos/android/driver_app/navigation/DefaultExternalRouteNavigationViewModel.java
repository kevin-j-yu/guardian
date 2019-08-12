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
package ai.rideos.android.driver_app.navigation;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import androidx.core.util.Pair;
import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import timber.log.Timber;

public class DefaultExternalRouteNavigationViewModel implements ExternalRouteNavigationViewModel {
    private static final int RETRY_COUNT = 3;
    private static final int DEFAULT_MAX_COORDINATES = 500;
    private static final float DEFAULT_SIMPLIFICATION_TOLERANCE = 0.002f;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final BehaviorSubject<LatLng> destinationSubject = BehaviorSubject.create();
    private final BehaviorSubject<Optional<LocationAndHeading>> originSubject = BehaviorSubject.create();

    private final RouteInteractor routeInteractor;
    private final DeviceLocator deviceLocator;
    private final SchedulerProvider schedulerProvider;

    private final int maxCoordinatesPerRoute;
    private final float simplificationTolerance;
    private final Simplify<LatLng> coordinateSimplifier;

    public DefaultExternalRouteNavigationViewModel(final RouteInteractor routeInteractor,
                                                   final DeviceLocator deviceLocator) {
        this(
            routeInteractor,
            deviceLocator,
            DEFAULT_MAX_COORDINATES,
            DEFAULT_SIMPLIFICATION_TOLERANCE,
            new Simplify<>(new LatLng[0], new LatLngPointExtractor()),
            new DefaultSchedulerProvider()
        );
    }

    public DefaultExternalRouteNavigationViewModel(final RouteInteractor routeInteractor,
                                                   final DeviceLocator deviceLocator,
                                                   final int maxCoordinatesPerRoute,
                                                   final float simplificationTolerance,
                                                   final Simplify<LatLng> coordinateSimplifier,
                                                   final SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
        this.routeInteractor = routeInteractor;
        this.deviceLocator = deviceLocator;
        this.maxCoordinatesPerRoute = maxCoordinatesPerRoute;
        this.simplificationTolerance = simplificationTolerance;
        this.coordinateSimplifier = coordinateSimplifier;
    }

    @Override
    public Observable<Result<List<LatLng>>> getRoute() {
        // Update when destination changes or forceUpdate is called. Don't update when current location changes
        return Observable.combineLatest(
            originSubject.startWith(Optional.empty()).observeOn(schedulerProvider.io()),
            destinationSubject.observeOn(schedulerProvider.io()),
            Pair::create
        )
            .observeOn(schedulerProvider.computation())
            // If the origin is given, then use it for the route. If not, find the last known location and use it
            // as the origin.
            .flatMap(originDestination -> {
                if (originDestination.first.isPresent()) {
                    return Observable.just(Pair.create(originDestination.first.get(), originDestination.second));
                } else {
                    return deviceLocator.getLastKnownLocation()
                        .map(locationAndHeading -> Pair.create(locationAndHeading, originDestination.second))
                        .toObservable();
                }
            })
            .flatMap(originDestination -> routeInteractor
                .getRoute(originDestination.first, new LocationAndHeading(originDestination.second, 0))
                .retry(RETRY_COUNT)
                .doOnError(e -> Timber.e(e, "Failed to find route to destination"))
                .map(RouteInfoModel::getRoute)
            )
            // simplify the route so it doesn't go over mapbox limits
            .map(this::simplifyRoute)
            .map(Result::success)
            .onErrorReturn(Result::failure);
    }

    private List<LatLng> simplifyRoute(final List<LatLng> originalRoute) {
        if (originalRoute.size() <= maxCoordinatesPerRoute) {
            return originalRoute;
        }
        final LatLng[] simplifiedRoute = coordinateSimplifier.simplify(
            originalRoute.toArray(new LatLng[0]),
            simplificationTolerance,
            false
        );
        return Arrays.asList(simplifiedRoute);
    }

    @Override
    public void setDestination(final LatLng destination) {
        destinationSubject.onNext(destination);
    }

    @Override
    public void didGoOffRoute(final LocationAndHeading fromLocation) {
        originSubject.onNext(Optional.of(fromLocation));
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }

    // Create point extractor to interface with simplification library's internal point representation
    private static class LatLngPointExtractor implements PointExtractor<LatLng> {
        // Used to improve simplification algorithm by not dealing with small decimals
        private static final int COORDINATE_MULTIPLE_CONSTANT = 1000000;

        @Override
        public double getX(LatLng latLng) {
            return latLng.getLatitude() * COORDINATE_MULTIPLE_CONSTANT;
        }

        @Override
        public double getY(LatLng latLng) {
            return latLng.getLongitude() * COORDINATE_MULTIPLE_CONSTANT;
        }
    }
}
