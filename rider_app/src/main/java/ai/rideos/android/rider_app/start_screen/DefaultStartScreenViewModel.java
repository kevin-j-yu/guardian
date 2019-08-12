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
package ai.rideos.android.rider_app.start_screen;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.VehiclePosition;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.interactors.PreviewVehicleInteractor;
import ai.rideos.android.rider_app.R;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import timber.log.Timber;

public class DefaultStartScreenViewModel implements StartScreenViewModel {
    private static final float ZOOM_LEVEL = 15;
    private static final int POLLING_INTERVAL_MILLI = 5000;

    private final BehaviorSubject<LatLng> currentMapCenterSubject = BehaviorSubject.create();
    private final Observable<LatLng> currentLocation;
    private final PreviewVehicleInteractor vehicleInteractor;
    private final Observable<FleetInfo> observableFleet;
    private final SchedulerProvider schedulerProvider;
    private final StartScreenListener listener;
    private final int pollingIntervalMilli;

    public DefaultStartScreenViewModel(final StartScreenListener listener,
                                       final DeviceLocator deviceLocation,
                                       final PreviewVehicleInteractor vehicleInteractor,
                                       final Observable<FleetInfo> observableFleet) {
        this(
            listener,
            deviceLocation,
            vehicleInteractor,
            observableFleet,
            new DefaultSchedulerProvider(),
            POLLING_INTERVAL_MILLI
        );
    }

    public DefaultStartScreenViewModel(final StartScreenListener listener,
                                       final DeviceLocator deviceLocation,
                                       final PreviewVehicleInteractor vehicleInteractor,
                                       final Observable<FleetInfo> observableFleet,
                                       final SchedulerProvider schedulerProvider,
                                       final int pollingIntervalMilli) {
        this.listener = listener;
        this.vehicleInteractor = vehicleInteractor;
        this.observableFleet = observableFleet;
        this.schedulerProvider = schedulerProvider;
        this.pollingIntervalMilli = pollingIntervalMilli;
        this.currentLocation = deviceLocation.observeCurrentLocation(pollingIntervalMilli)
            .map(LocationAndHeading::getLatLng);
    }

    @Override
    public void setCurrentMapCenter(final LatLng center) {
        currentMapCenterSubject.onNext(center);
    }

    @Override
    public void startDestinationSearch() {
        listener.startPreTripFlow();
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return Observable.just(new MapSettings(
            true,
            CenterPin.hidden()
        ));
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdates() {
        return currentLocation.observeOn(schedulerProvider.computation())
            .firstOrError()
            .map(location -> CameraUpdate.centerAndZoom(
                new LatLng(location.getLatitude(), location.getLongitude()),
                ZOOM_LEVEL
            ))
            .toObservable();
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        final Observable<List<VehiclePosition>> periodicVehicleObservable = Observable.combineLatest(
            Observable.interval(0, pollingIntervalMilli, TimeUnit.MILLISECONDS, schedulerProvider.io())
                .timeInterval(),
            currentMapCenterSubject,
            observableFleet,
            (time, mapCenter, fleet) -> Pair.create(mapCenter, fleet)
        )
            .observeOn(schedulerProvider.computation())
            .flatMap(mapCenterAndFleet ->
                vehicleInteractor.getVehiclesInVicinity(mapCenterAndFleet.first, mapCenterAndFleet.second.getId())
                    .observeOn(schedulerProvider.computation())
                    .doOnError(e -> Timber.e(e, "Error retrieving vehicles for start screen"))
                    .map(Result::success)
                    .onErrorReturn(Result::failure)
            )
            .filter(Result::isSuccess)
            .map(Result::get);

        return periodicVehicleObservable
            .map(DefaultStartScreenViewModel::getMarkersFromVehiclePositions);
    }

    private static Map<String, DrawableMarker> getMarkersFromVehiclePositions(
        final List<VehiclePosition> vehiclePositions
    ) {
        return vehiclePositions.stream()
            .collect(Collectors.toMap(
                VehiclePosition::getVehicleId,
                vehiclePosition -> new DrawableMarker(
                    vehiclePosition.getPosition(),
                    vehiclePosition.getHeading(),
                    R.mipmap.car,
                    Anchor.CENTER
                )
            ));
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return Observable.just(Collections.emptyList());
    }

    @Override
    public void destroy() {
        this.vehicleInteractor.shutDown();
    }
}
