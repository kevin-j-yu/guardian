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
package ai.rideos.android.driver_app.online.driving.confirming_arrival;

import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class DefaultConfirmingArrivalViewModel implements ConfirmingArrivalViewModel {
    private static final int RETRY_COUNT = 2;

    private final GeocodeInteractor geocodeInteractor;
    private final SchedulerProvider schedulerProvider;
    private final LatLng destination;

    public DefaultConfirmingArrivalViewModel(final GeocodeInteractor geocodeInteractor, final LatLng destination) {
        this(
            geocodeInteractor,
            destination,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultConfirmingArrivalViewModel(final GeocodeInteractor geocodeInteractor,
                                             final LatLng destination,
                                             final SchedulerProvider schedulerProvider) {
        this.geocodeInteractor = geocodeInteractor;
        this.schedulerProvider = schedulerProvider;
        this.destination = destination;
    }

    @Override
    public Observable<String> getArrivalDetailText() {
        return geocodeInteractor.getBestReverseGeocodeResult(destination)
            .observeOn(schedulerProvider.computation())
            .retry(RETRY_COUNT)
            .doOnError(error -> Timber.e(error, "Failed to geocode location"))
            .onErrorReturn(Result::failure)
            .map(result -> {
                if (result.isFailure()) {
                    return "";
                }
                return result.get().getDisplayName();
            });
    }

    @Override
    public void destroy() {
        // Do nothing
    }

    // This view doesn't require any markers or routes, so these methods should just return empty maps/lists.
    // TODO can consider zooming on destination and showing the current location as a car marker
    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(true, CenterPin.hidden()));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return Observable.empty();
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return Observable.just(Collections.emptyMap());
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return Observable.just(Collections.emptyList());
    }
}
