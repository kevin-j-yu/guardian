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

import static org.mockito.Mockito.when;

import ai.rideos.android.common.app.map.MapStateReceiver.MapCenterListener;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.viewmodel.map.MapStateProvider;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MapRelayTest {
    private static final CameraUpdate CAMERA_UPDATE = CameraUpdate.centerAndZoom(new LatLng(0, 1), 1.0f);
    private MapRelay mapRelayUnderTest;

    @Before
    public void setUp() {
        mapRelayUnderTest = new MapRelay(new TrampolineSchedulerProvider());
    }

    @Test
    public void testMapIsInitiallyCentered() {
        mapRelayUnderTest.shouldAllowReCentering().test()
            .assertValueAt(0, false);
    }

    @Test
    public void testRequestCameraUpdateIsImmediatelyPerformedWhenMapIsCentered() {
        mapRelayUnderTest.reCenterMap();
        mapRelayUnderTest.moveCamera(CAMERA_UPDATE, false);
        mapRelayUnderTest.getCameraUpdatesToPerform().test()
            .assertValueAt(0, CAMERA_UPDATE);
    }

    @Test
    public void testRequestCameraUpdateIsIgnoredWhenMapIsNotCentered() {
        mapRelayUnderTest.mapWasDragged();
        mapRelayUnderTest.moveCamera(CAMERA_UPDATE, false);
        mapRelayUnderTest.getCameraUpdatesToPerform().test()
            .assertEmpty();
    }

    @Test
    public void testRequestCameraUpdateWithForcePerformsUpdate() {
        mapRelayUnderTest.mapWasDragged();
        mapRelayUnderTest.moveCamera(CAMERA_UPDATE, true);
        mapRelayUnderTest.getCameraUpdatesToPerform().test()
            .assertValueAt(0, CAMERA_UPDATE);
    }

    @Test
    public void testGetCameraUpdatesToPerformRecordsLatestCameraUpdateWhenMapNotCentered() {
        mapRelayUnderTest.mapWasDragged();
        mapRelayUnderTest.moveCamera(CameraUpdate.noUpdate(), false);
        mapRelayUnderTest.moveCamera(CAMERA_UPDATE, false);
        mapRelayUnderTest.getCameraUpdatesToPerform().test().assertEmpty();
        mapRelayUnderTest.reCenterMap();
        mapRelayUnderTest.getCameraUpdatesToPerform().test()
            .assertValueAt(0, CAMERA_UPDATE);
    }

    @Test
    public void testConnectToProviderConnectsAllNecessaryState() {
        final MapStateProvider stateProvider = Mockito.mock(MapStateProvider.class);
        final MapCenterListener mapCenterListener = Mockito.mock(MapCenterListener.class);

        final MapSettings settings = new MapSettings(false, CenterPin.hidden());
        when(stateProvider.getMapSettings()).thenReturn(Observable.just(settings));

        final CameraUpdate cameraUpdate = CameraUpdate.centerAndZoom(new LatLng(1, 2), 15.0f);
        when(stateProvider.getCameraUpdates()).thenReturn(Observable.just(cameraUpdate));

        final Map<String, DrawableMarker> markers = new HashMap<>();
        markers.put("marker1", new DrawableMarker(new LatLng(1, 2), 0f, 0, Anchor.BOTTOM));
        when(stateProvider.getMarkers()).thenReturn(Observable.just(markers));

        final List<DrawablePath> paths = Collections.singletonList(new DrawablePath(Collections.emptyList(), 15f, 0));
        when(stateProvider.getPaths()).thenReturn(Observable.just(paths));

        mapRelayUnderTest.connectToProvider(stateProvider, mapCenterListener);
        mapRelayUnderTest.getMapSettings().test()
            .assertValueCount(1)
            .assertValueAt(0, settings);
        mapRelayUnderTest.getCameraUpdatesToPerform().test()
            .assertValueCount(1)
            .assertValueAt(0, cameraUpdate);
        mapRelayUnderTest.getMarkers().test()
            .assertValueCount(1)
            .assertValueAt(0, markers);
        mapRelayUnderTest.getPaths().test()
            .assertValueCount(1)
            .assertValueAt(0, paths);
        mapRelayUnderTest.getMapCenterListener().test()
            .assertValueCount(1)
            .assertValueAt(0, mapCenterListener);
    }
}
