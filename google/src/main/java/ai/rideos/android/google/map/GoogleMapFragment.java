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
package ai.rideos.android.google.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.map.MapStateReceiver.MapCenterListener;
import ai.rideos.android.common.app.map.MapViewModel;
import ai.rideos.android.common.model.map.CameraUpdate;
import ai.rideos.android.common.model.map.CenterPin;
import ai.rideos.android.common.model.map.DrawableMarker;
import ai.rideos.android.common.model.map.DrawableMarker.Anchor;
import ai.rideos.android.common.model.map.DrawablePath;
import ai.rideos.android.common.model.map.DrawablePath.Style;
import ai.rideos.android.common.model.map.MapSettings;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.common.utils.SetOperations;
import ai.rideos.android.common.utils.SetOperations.DiffResult;
import ai.rideos.android.common.view.DensityConverter;
import ai.rideos.android.common.view.ViewMargins;
import ai.rideos.android.google.R;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class GoogleMapFragment extends Fragment implements OnMapReadyCallback {
    private static final int BOUNDS_PADDING = 90;
    private static final int ANIMATION_SPEED_MILLIS = 250;
    private static final int RE_CENTER_PADDING_DP = 20;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private MapViewModel mapViewModel;
    private GoogleMap googleMap;
    private View reCenterButton;
    private ImageView centerPin;
    private final Map<String, Marker> currentMarkers = new HashMap<>();
    private List<Polyline> currentPaths = new ArrayList<>();
    private Context context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.google_map_fragment, container, false);
        centerPin = view.findViewById(R.id.center_pin);

        mapViewModel = MapRelay.get();
        context = view.getContext();
        reCenterButton = view.findViewById(R.id.re_center_button);
        reCenterButton.setVisibility(View.GONE);
        reCenterButton.setOnClickListener(click -> mapViewModel.reCenterMap());
        final SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
            .findFragmentById(R.id.support_map_fragment);
        mapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }

    private void subscribeToMapViewModel() {
        compositeDisposable.addAll(
            mapViewModel.getCameraUpdatesToPerform().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::performCameraUpdate),
            mapViewModel.shouldAllowReCentering().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setRecenterButtonVisibility),
            mapViewModel.getMapSettings().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setMapSettings),
            mapViewModel.getMarkers().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showMarkers),
            mapViewModel.getPaths().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showPaths),
            mapViewModel.getMapMargins().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setMapMargins),
            mapViewModel.getMapCenterListener().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setMapCenterListener)
        );
    }

    private void performCameraUpdate(final CameraUpdate cameraUpdate) {
        switch (cameraUpdate.getUpdateType()) {
            case FIT_LAT_LNG_BOUNDS:
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    new LatLngBounds(
                        Locations.toGoogleLatLng(cameraUpdate.getNewBounds().getSouthwestCorner()),
                        Locations.toGoogleLatLng(cameraUpdate.getNewBounds().getNortheastCorner())
                    ),
                    BOUNDS_PADDING
                ), ANIMATION_SPEED_MILLIS, null);
                break;
            case CENTER_AND_ZOOM:
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    Locations.toGoogleLatLng(cameraUpdate.getNewCenter()),
                    cameraUpdate.getNewZoom()
                ), ANIMATION_SPEED_MILLIS, null);
                break;
            case NO_UPDATE:  // Do nothing
        }
    }

    private void setRecenterButtonVisibility(final boolean isVisible) {
        if (isVisible) {
            reCenterButton.setVisibility(View.VISIBLE);
        } else {
            reCenterButton.setVisibility(View.GONE);
        }
    }

    private void setMapSettings(final MapSettings mapSettings) {
        setCenterPin(mapSettings.getCenterPin());
        setCurrentLocationEnabled(mapSettings.shouldShowUserLocation());
    }

    private void setCenterPin(final CenterPin centerPin) {
        if (centerPin.shouldShow()) {
            displayCenterPin(centerPin.getDrawablePin());
        } else {
            hideCenterPin();
        }
    }

    private void setCurrentLocationEnabled(final boolean enabled) {
        if (enabled && !googleMap.isMyLocationEnabled()) {
            setMyLocationEnabledIfPermitted(true);
        } else if (!enabled && googleMap.isMyLocationEnabled()) {
            setMyLocationEnabledIfPermitted(false);
        }
    }

    private void setMyLocationEnabledIfPermitted(final boolean enabled) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                1
            );
        } else {
            googleMap.setMyLocationEnabled(enabled);
        }
    }

    private void displayCenterPin(final int drawable) {
        centerPin.setVisibility(View.VISIBLE);
        centerPin.setBackgroundResource(drawable);
    }

    private void hideCenterPin() {
        centerPin.setVisibility(View.INVISIBLE);
        centerPin.setBackgroundResource(0); // remove
    }

    private void showMarkers(final Map<String, DrawableMarker> newMarkers) {
        final DiffResult<String> diffResult = SetOperations.getDifferences(
            newMarkers.keySet(),
            currentMarkers.keySet()
        );

        for (final String newMarkerKey : diffResult.getOnlyOnLeft()) {
            final DrawableMarker markerToAdd = newMarkers.get(newMarkerKey);
            final Marker newMarker = googleMap.addMarker(new MarkerOptions()
                .anchor(0.5f, getVerticalAnchor(markerToAdd))
                .position(Locations.toGoogleLatLng(markerToAdd.getPosition()))
                .rotation(markerToAdd.getRotation())
                .icon(getMarkerIcon(markerToAdd.getDrawableIcon()))
            );
            currentMarkers.put(newMarkerKey, newMarker);
        }

        for (final String currentMarkerKey : diffResult.getIntersecting()) {
            final DrawableMarker markerToUpdate = newMarkers.get(currentMarkerKey);
            final Marker drawnMarker = currentMarkers.get(currentMarkerKey);
            drawnMarker.setPosition(Locations.toGoogleLatLng(markerToUpdate.getPosition()));
            drawnMarker.setRotation(markerToUpdate.getRotation());
            // Cannot update color
        }

        for (final String deletedMarkerKey : diffResult.getOnlyOnRight()) {
            currentMarkers.get(deletedMarkerKey).remove();
            currentMarkers.remove(deletedMarkerKey);
        }
    }

    private float getVerticalAnchor(final DrawableMarker marker) {
        if (marker.getAnchor() == Anchor.BOTTOM) {
            return 1.0f;
        } else {
            return 0.5f;
        }
    }

    private static BitmapDescriptor getMarkerIcon(final int drawableIcon) {
        return BitmapDescriptorFactory.fromResource(drawableIcon);
    }

    private void showPaths(final List<DrawablePath> paths) {
        for (final Polyline drawnPath : currentPaths) {
            drawnPath.remove();
        }

        currentPaths = new ArrayList<>(paths.size());

        for (final DrawablePath newPath : paths) {
            final Polyline addedPath = googleMap.addPolyline(new PolylineOptions()
                .addAll(
                    newPath.getCoordinates().stream()
                        .map(Locations::toGoogleLatLng)
                        .collect(Collectors.toList())
                )
                .width(newPath.getWidth())
                .color(newPath.getColor())
                .pattern(getPatternForStyle(newPath.getStyle()))
            );
            currentPaths.add(addedPath);
        }
    }

    private static List<PatternItem> getPatternForStyle(final Style style) {
        if (style == Style.DOTTED) {
            return Arrays.asList(new Dot(), new Gap(11));
        } else {
            // null == standard solid line pattern
            return null;
        }
    }

    private void setMapCenterListener(final MapCenterListener listener) {
        googleMap.setOnCameraIdleListener(() -> listener.mapCenterDidMove(
            Locations.fromGoogleLatLng(googleMap.getCameraPosition().target)
        ));
        googleMap.setOnCameraMoveStartedListener((reason) -> {
            listener.mapCenterStartedMoving();
            if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
                mapViewModel.mapWasDragged();
            }
        });
    }

    private void setMapMargins(final ViewMargins mapMargins) {
        googleMap.setPadding(mapMargins.getLeft(), mapMargins.getTop(), mapMargins.getRight(), mapMargins.getBottom());
        setLayoutParamsForView(
            centerPin,
            calculateCenteredMargins(mapMargins)
        );

        final int reCenterPaddingPx = DensityConverter.fromContext(getContext())
            .convertDpToPixels(RE_CENTER_PADDING_DP);
        setLayoutParamsForView(
            reCenterButton,
            ViewMargins.newBuilder()
                .setRight(mapMargins.getRight() + reCenterPaddingPx)
                .setBottom(mapMargins.getBottom() + reCenterPaddingPx)
                .build()
        );
    }

    /**
     * When centering an object in the map with insets, simply adding the top/bottom/left/right margins will not work.
     * This is because, for example, setting the top margin as some amount and bottom margin as some amount does not
     * actually center the object vertically. Instead, we have to find the average of the top/bottom and left/right
     * insets and use that as the margin.
     */
    private ViewMargins calculateCenteredMargins(final ViewMargins mapMargins) {
        final int topOffset = (mapMargins.getTop() - mapMargins.getBottom()) / 2;
        final int leftOffset = (mapMargins.getLeft() - mapMargins.getRight()) / 2;
        ViewMargins.Builder builder = ViewMargins.newBuilder();
        builder = topOffset > 0 ? builder.setTop(topOffset) : builder.setBottom(-topOffset);
        builder = leftOffset > 0 ? builder.setLeft(leftOffset) : builder.setRight(-leftOffset);
        return builder.build();
    }

    private static void setLayoutParamsForView(final View view, final ViewMargins mapMargins) {
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        lp.setMargins(mapMargins.getLeft(), mapMargins.getTop(), mapMargins.getRight(), mapMargins.getBottom());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        try {
            // Customise map styling via JSON file
            MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night);
            boolean success = googleMap.setMapStyle(style);
            if (!success) {
                Log.e("Error", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("Error", "Can't find style. Error: ", e);
        }
        final UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setTiltGesturesEnabled(false);
        uiSettings.setCompassEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        // https://issuetracker.google.com/issues/35829548
        googleMap.setIndoorEnabled(false);
        subscribeToMapViewModel();
    }
}
