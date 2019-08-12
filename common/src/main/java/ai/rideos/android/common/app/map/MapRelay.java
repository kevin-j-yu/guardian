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
package ai.rideos.android.common.app.map;

import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.view.ViewMargins;
import ai.rideos.android.common.viewmodel.map.MapStateProvider;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.subjects.BehaviorSubject;
import java.util.List;
import java.util.Map;

/**
 * The MapRelay relays state from a MapStateProvider to a MapFragment by implementing both the MapViewModel and the
 * MapStateReceiver. The state provider sends information through the MapStateReceiver which is added to behavior subjects.
 * When the MapFragment is loaded, it can connect through the MapViewModel to receive state and update the map view.
 */
public class MapRelay implements MapViewModel, MapStateReceiver {
    private static volatile MapRelay INSTANCE;

    private final BehaviorSubject<Boolean> isMapCenteredSubject = BehaviorSubject.createDefault(true);
    private final BehaviorSubject<CameraUpdate> latestCameraUpdateSubject = BehaviorSubject.create();
    private final BehaviorSubject<MapSettings> mapSettingsSubject = BehaviorSubject.create();
    private final BehaviorSubject<Map<String, DrawableMarker>> markerSubject = BehaviorSubject.create();
    private final BehaviorSubject<List<DrawablePath>> pathSubject = BehaviorSubject.create();
    private final BehaviorSubject<ViewMargins> mapMarginSubject = BehaviorSubject.create();
    private final BehaviorSubject<MapCenterListener> mapCenterListenerSubject = BehaviorSubject.create();

    private final SchedulerProvider schedulerProvider;

    private MapRelay() {
        this(new DefaultSchedulerProvider());
    }

    @VisibleForTesting
    MapRelay(final SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
    }

    public static MapRelay get() {
        if (INSTANCE == null) {
            synchronized (MapRelay.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MapRelay();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Connect a map state provider to the map relay and returns a disposable for all subscribed values
     * @param mapStateProvider - state provider, like a view model
     * @return disposable with all elements that have been subscribed
     */
    public Disposable connectToProvider(final MapStateProvider mapStateProvider) {
        return connectToProvider(mapStateProvider, MapCenterListener.NOOP);
    }

    /**
     * Connect a map state provider and a map center listener to the map relay and returns a disposable for all
     * subscribed values
     * @param mapStateProvider - state provider, like a view model
     * @param mapCenterListener - listener that is called when the map view center changes
     * @return disposable with all elements that have been subscribed
     */
    public Disposable connectToProvider(final MapStateProvider mapStateProvider,
                                        final MapCenterListener mapCenterListener) {
        final CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.addAll(
            mapStateProvider.getMapSettings()
                .subscribe(this::setMapSettings),
            // Forcefully move camera when a provider first connects to the map view
            mapStateProvider.getCameraUpdates()
                .firstElement()
                .subscribe(update -> moveCamera(update, true)),
            // Non-forcefully move camera for 2nd update and after
            mapStateProvider.getCameraUpdates()
                .skip(1)
                .subscribe(update -> moveCamera(update, false)),
            mapStateProvider.getMarkers()
                .subscribe(this::showMarkers),
            mapStateProvider.getPaths()
                .subscribe(this::showPaths)
        );
        if (mapCenterListener != null) {
            this.setMapCenterListener(mapCenterListener);
        }

        // Between map providers, hide the center pin. This helps when the padding of the map changes and the center
        // pin moves around the map
        compositeDisposable.add(Disposables.fromAction(() ->
            setMapSettings(new MapSettings(false, CenterPin.hidden()))
        ));

        return compositeDisposable;
    }

    // MapViewModel - exposed to map fragment

    @Override
    public void mapWasDragged() {
        isMapCenteredSubject.onNext(false);
    }

    @Override
    public void reCenterMap() {
        isMapCenteredSubject.onNext(true);
    }

    @Override
    public Observable<CameraUpdate> getCameraUpdatesToPerform() {
        return Observable.combineLatest(
            latestCameraUpdateSubject.observeOn(schedulerProvider.computation()),
            isMapCenteredSubject.observeOn(schedulerProvider.computation()),
            Pair::create
        )
            // Only return camera updates when map is centered
            .filter(updateAndCentered -> updateAndCentered.second)
            .map(updateAndCentered -> updateAndCentered.first);
    }

    @Override
    public Observable<Boolean> shouldAllowReCentering() {
        return isMapCenteredSubject.map(isCentered -> !isCentered);
    }

    @Override
    public Observable<MapSettings> getMapSettings() {
        return mapSettingsSubject;
    }

    @Override
    public Observable<Map<String, DrawableMarker>> getMarkers() {
        return markerSubject;
    }

    @Override
    public Observable<List<DrawablePath>> getPaths() {
        return pathSubject;
    }

    @Override
    public Observable<ViewMargins> getMapMargins() {
        return mapMarginSubject;
    }

    @Override
    public Observable<MapCenterListener> getMapCenterListener() {
        return mapCenterListenerSubject;
    }

    // MapStateReceiver - exposed to map state provider

    @Override
    public void setMapSettings(final MapSettings mapSettings) {
        mapSettingsSubject.onNext(mapSettings);
    }

    @Override
    public void moveCamera(final CameraUpdate cameraUpdate, final boolean force) {
        latestCameraUpdateSubject.onNext(cameraUpdate);
        if (force) {
            reCenterMap();
        }
    }

    @Override
    public void showMarkers(final Map<String, DrawableMarker> markers) {
        markerSubject.onNext(markers);
    }

    @Override
    public void showPaths(final List<DrawablePath> paths) {
        pathSubject.onNext(paths);
    }

    @Override
    public void setMapCenterListener(final MapCenterListener listener) {
        mapCenterListenerSubject.onNext(listener);
    }

    @Override
    public void setMapMargins(final ViewMargins mapMargins) {
        mapMarginSubject.onNext(mapMargins);
    }
}
