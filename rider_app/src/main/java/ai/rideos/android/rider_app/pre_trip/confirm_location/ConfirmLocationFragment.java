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
package ai.rideos.android.rider_app.pre_trip.confirm_location;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.app.map.MapStateReceiver.MapCenterListener;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationFragment.ConfirmLocationArgs;
import ai.rideos.android.rider_app.pre_trip.confirm_location.ConfirmLocationViewModel.ReverseGeocodingStatus;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class ConfirmLocationFragment extends FragmentViewController<ConfirmLocationArgs, ConfirmLocationListener> {
    public enum ConfirmLocationType {
        PICKUP,
        DROP_OFF
    }

    public static class ConfirmLocationArgs implements Serializable {
        private final boolean enableEditing;
        private final ConfirmLocationType confirmLocationType;
        @Nullable
        private final NamedTaskLocation initialLocation;
        private final int titleText;
        private final int buttonText;

        public ConfirmLocationArgs(final boolean enableEditing,
                                   final ConfirmLocationType confirmLocationType,
                                   @Nullable final NamedTaskLocation initialLocation,
                                   @StringRes final int titleText,
                                   @StringRes final int buttonText) {
            this.enableEditing = enableEditing;
            this.confirmLocationType = confirmLocationType;
            this.initialLocation = initialLocation;
            this.titleText = titleText;
            this.buttonText = buttonText;
        }
    }

    private ConfirmLocationViewModel confirmLocationViewModel;
    private CompositeDisposable compositeDisposable;
    private Runnable onEdit;

    @Override
    public ControllerTypes<ConfirmLocationArgs, ConfirmLocationListener> getTypes() {
        return new ControllerTypes<>(ConfirmLocationArgs.class, ConfirmLocationListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ConfirmLocationListener listener = getListener();
        if (getArgs().confirmLocationType == ConfirmLocationType.PICKUP) {
            confirmLocationViewModel = ConfirmLocationViewModelBuilder.buildPickupViewModel(
                getContext(),
                listener::confirmPickup,
                getArgs().initialLocation
            );
            onEdit = listener::editPickup;
        } else {
            confirmLocationViewModel = ConfirmLocationViewModelBuilder.buildDropOffViewModel(
                getContext(),
                listener::confirmDropOff,
                getArgs().initialLocation
            );
            onEdit = listener::editDropOff;
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final ConfirmLocationListener listener = getListener();
        final View confirmView = BottomDetailAndButtonView.inflateWithUpButton(inflater, container, listener::navigateUp, R.layout.confirm_location);

        final TextView titleView = confirmView.findViewById(R.id.confirm_location_title);
        final Button confirmButton = confirmView.findViewById(R.id.confirm_location_button);
        titleView.setText(getArgs().titleText);
        confirmButton.setText(getArgs().buttonText);
        return confirmView;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();

        final Button confirmButton = view.findViewById(R.id.confirm_location_button);
        confirmButton.setOnClickListener(click -> confirmLocationViewModel.confirmLocation());

        final TextView locationField = view.findViewById(R.id.confirm_location_field);

        final AppCompatImageView editButton = view.findViewById(R.id.edit_button);
        if (getArgs().enableEditing) {
            editButton.setVisibility(View.VISIBLE);
            editButton.setOnClickListener(click -> onEdit.run());
        } else {
            editButton.setVisibility(View.GONE);
        }

        final LoadableDividerView loadableDivider = view.findViewById(R.id.loadable_divider);

        compositeDisposable = new CompositeDisposable();

        compositeDisposable.add(MapRelay.get().connectToProvider(
            confirmLocationViewModel,
            new MapCenterListener() {
                @Override
                public void mapCenterDidMove(final LatLng newLocation) {
                    confirmLocationViewModel.onCameraMoved(newLocation);
                }

                @Override
                public void mapCenterStartedMoving() {
                    confirmLocationViewModel.onCameraStartedMoving();
                }
            }
        ));

        compositeDisposable.addAll(
            confirmLocationViewModel.getReverseGeocodedLocation()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(locationField::setText),
            confirmLocationViewModel.getReverseGeocodingStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> displayStatus(loadableDivider, confirmButton, status))
        );
    }

    private void displayStatus(final LoadableDividerView loadableDivider,
                               final Button confirmButton,
                               final ReverseGeocodingStatus status) {
        switch (status) {
            case IDLE:
                loadableDivider.stopLoading();
                confirmButton.setEnabled(true);
                break;
            case IN_PROGRESS:
            case ERROR:
                // TODO improve error display
                loadableDivider.startLoading();
                confirmButton.setEnabled(false);
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        confirmLocationViewModel.destroy();
    }
}
