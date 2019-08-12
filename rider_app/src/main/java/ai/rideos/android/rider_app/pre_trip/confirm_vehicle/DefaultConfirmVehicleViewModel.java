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

import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.model.SingleSelectOptions.Option;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.interactors.AvailableVehicleInteractor;
import ai.rideos.android.model.AvailableVehicle;
import ai.rideos.android.model.VehicleSelectionOption;
import ai.rideos.android.rider_app.R;
import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import timber.log.Timber;

public class DefaultConfirmVehicleViewModel implements ConfirmVehicleViewModel {
    private static final int DEFAULT_RETRY_COUNT = 3;

    private final AvailableVehicleInteractor vehicleInteractor;
    private final Observable<FleetInfo> observableFleet;
    private final ResourceProvider resourceProvider;
    private final SchedulerProvider schedulerProvider;

    public DefaultConfirmVehicleViewModel(final AvailableVehicleInteractor vehicleInteractor,
                                          final Observable<FleetInfo> observableFleet,
                                          final ResourceProvider resourceProvider) {
        this(
            vehicleInteractor,
            observableFleet,
            resourceProvider,
            new SchedulerProviders.DefaultSchedulerProvider()
        );
    }

    public DefaultConfirmVehicleViewModel(final AvailableVehicleInteractor vehicleInteractor,
                                          final Observable<FleetInfo> observableFleet,
                                          final ResourceProvider resourceProvider,
                                          final SchedulerProvider schedulerProvider) {
        this.vehicleInteractor = vehicleInteractor;
        this.observableFleet = observableFleet;
        this.resourceProvider = resourceProvider;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<SingleSelectOptions<VehicleSelectionOption>> getVehicleSelectionOptions() {
        return observableFleet.observeOn(schedulerProvider.computation())
            .flatMap(fleetInfo -> vehicleInteractor
                .getAvailableVehicles(fleetInfo.getId())
                .map(vehicles -> vehicles
                    .stream()
                    .sorted(Comparator.comparing(AvailableVehicle::getDisplayName))
                    .collect(Collectors.toList())
                )
            )
            .retryWhen(RetryBehaviors.getDefault())
            .doOnError(e -> Timber.e("Could not get vehicle options"))
            // On an error, just return no options. By default, we will populate the "automatic" option.
            .onErrorReturnItem(Collections.emptyList())
            .map(availableVehicles -> {
                final List<Option<VehicleSelectionOption>> options = new ArrayList<>(availableVehicles.size() + 1);
                options.add(new Option<>(
                    resourceProvider.getString(R.string.automatic_vehicle_display_name),
                    VehicleSelectionOption.automatic()
                ));
                options.addAll(
                    availableVehicles.stream()
                        .map(vehicle -> new Option<>(
                            vehicle.getDisplayName(),
                            VehicleSelectionOption.manual(vehicle.getVehicleId())
                        ))
                        .collect(Collectors.toList())
                );
                // Select automatic by default
                return SingleSelectOptions.withSelection(options, 0);
            });
    }

    @Override
    public void destroy() {
        vehicleInteractor.shutDown();
    }
}
