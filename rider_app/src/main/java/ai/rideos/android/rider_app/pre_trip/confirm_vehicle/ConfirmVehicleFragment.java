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
package ai.rideos.android.rider_app.pre_trip.confirm_vehicle;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.view.adapters.SingleSelectArrayAdapter;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.model.VehicleSelectionOption;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class ConfirmVehicleFragment extends FragmentViewController<EmptyArg, ConfirmVehicleListener> {
    private ConfirmVehicleViewModel viewModel;
    private CompositeDisposable compositeDisposable;

    @Override
    public ControllerTypes<EmptyArg, ConfirmVehicleListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, ConfirmVehicleListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultConfirmVehicleViewModel(
            RiderDependencyRegistry.riderDependencyFactory().getAvailableVehicleInteractor(getContext()),
            ResolvedFleet.get().observeFleetInfo(),
            AndroidResourceProvider.forContext(getContext())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final ConfirmVehicleListener listener = getListener();
        return BottomDetailAndButtonView.inflateWithUpButton(inflater, container, listener::navigateUp, R.layout.confirm_vehicle);
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        final Spinner spinner = view.findViewById(R.id.vehicle_spinner);
        final Button selectButton = view.findViewById(R.id.select_vehicle_button);

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            viewModel.getVehicleSelectionOptions().observeOn(AndroidSchedulers.mainThread())
                .subscribe(options -> setVehicleSelectionOptions(options, spinner, selectButton))
        );
    }

    private void setVehicleSelectionOptions(final SingleSelectOptions<VehicleSelectionOption> vehicleOptions,
                                            final Spinner vehicleSelectionSpinnner,
                                            final Button selectVehicleButton) {
        final SingleSelectArrayAdapter<VehicleSelectionOption> adapter
            = new SingleSelectArrayAdapter<>(getContext(), vehicleOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleSelectionSpinnner.setAdapter(adapter);

        if (vehicleOptions.getSelectionIndex().isPresent()) {
            vehicleSelectionSpinnner.setSelection(vehicleOptions.getSelectionIndex().get());
        }

        final ConfirmVehicleListener listener = getListener();
        selectVehicleButton.setOnClickListener(click -> {
            final int selectedIndex = vehicleSelectionSpinnner.getSelectedItemPosition();
            listener.confirmVehicle(adapter.getOptionAtPosition(selectedIndex).getValue());
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }
}
