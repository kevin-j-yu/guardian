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
package ai.rideos.android.driver_app.online.trip_details;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.driver_app.online.trip_details.TripDetailsFragment.TripDetailsArgs;
import ai.rideos.android.model.VehiclePlan;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class TripDetailsFragment extends FragmentViewController<TripDetailsArgs, TripDetailsListener> {
    private CompositeDisposable compositeDisposable;
    private TripDetailsViewModel viewModel;

    public static class TripDetailsArgs implements Serializable {
        private final VehiclePlan vehiclePlan;

        public TripDetailsArgs(final VehiclePlan vehiclePlan) {
            this.vehiclePlan = vehiclePlan;
        }
    }

    @Override
    public ControllerTypes<TripDetailsArgs, TripDetailsListener> getTypes() {
        return new ControllerTypes<>(TripDetailsArgs.class, TripDetailsListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new DefaultTripDetailsViewModel(
            DriverDependencyRegistry.driverDependencyFactory().getDriverVehicleInteractor(getContext()),
            User.get(getContext()),
            getArgs().vehiclePlan
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.trip_details, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        compositeDisposable = new CompositeDisposable();

        final Toolbar toolbar = view.findViewById(ai.rideos.android.common.R.id.title_bar);
        toolbar.setNavigationOnClickListener(click -> getListener().closeTripDetails());

        final RecyclerView recyclerView = view.findViewById(R.id.trip_details_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // specify an adapter (see also next example)
        final TripDetailsRecyclerAdapter recyclerAdapter = new TripDetailsRecyclerAdapter(
            getActivity(),
            viewModel::performActionOnTrip
        );
        recyclerView.setAdapter(recyclerAdapter);

        compositeDisposable.add(
            viewModel.getTripDetails().observeOn(AndroidSchedulers.mainThread())
                .subscribe(recyclerAdapter::setTripDetails)
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
        viewModel.destroy();
    }
}
