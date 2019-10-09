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

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.driver_app.online.trip_details.TripDetail.ActionToPerform;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import timber.log.Timber;

public class DefaultTripDetailsViewModel implements TripDetailsViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final DriverVehicleInteractor vehicleInteractor;
    private final GeocodeInteractor geocodeInteractor;
    private final User user;
    private final VehiclePlan vehiclePlan;
    private final SchedulerProvider schedulerProvider;

    public DefaultTripDetailsViewModel(final DriverVehicleInteractor vehicleInteractor,
                                       final GeocodeInteractor geocodeInteractor,
                                       final User user,
                                       final VehiclePlan vehiclePlan) {
        this(
            vehicleInteractor,
            geocodeInteractor,
            user,
            vehiclePlan,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultTripDetailsViewModel(final DriverVehicleInteractor vehicleInteractor,
                                       final GeocodeInteractor geocodeInteractor,
                                       final User user,
                                       final VehiclePlan vehiclePlan,
                                       final SchedulerProvider schedulerProvider) {
        this.vehicleInteractor = vehicleInteractor;
        this.geocodeInteractor = geocodeInteractor;
        this.user = user;
        this.vehiclePlan = vehiclePlan;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<List<TripDetail>> getTripDetails() {
        final LinkedHashMap<String, Waypoint> tripsAndNextWaypoint = new LinkedHashMap<>();
        final Map<String, LatLng> tripPickupLocations = new HashMap<>();
        final Map<String, LatLng> tripDropOffLocations = new HashMap<>();
        for (final Waypoint waypoint : vehiclePlan.getWaypoints()) {
            if (!tripsAndNextWaypoint.containsKey(waypoint.getTaskId())) {
                tripsAndNextWaypoint.put(waypoint.getTaskId(), waypoint);
            }
            switch (waypoint.getAction().getActionType()) {
                case DRIVE_TO_PICKUP:
                case LOAD_RESOURCE:
                    tripPickupLocations.put(waypoint.getTaskId(), waypoint.getAction().getDestination());
                    break;
                case DRIVE_TO_DROP_OFF:
                    tripDropOffLocations.put(waypoint.getTaskId(), waypoint.getAction().getDestination());
            }
        }

        // Get geocoded results for all pickup/drop-off locations and use them to display trip details
        return Single.zip(
            geocodeLocationCollection(tripPickupLocations),
            geocodeLocationCollection(tripDropOffLocations),
            Pair::create
        )
            .observeOn(schedulerProvider.computation())
            .map(geocodedPickupsAndDropOffs -> {
                final Map<String, String> geocodedPickups = geocodedPickupsAndDropOffs.first;
                final Map<String, String> geocodedDropOffs = geocodedPickupsAndDropOffs.second;
                return tripsAndNextWaypoint.entrySet().stream()
                    .map(tripAndNextWaypoint -> {
                        final Waypoint nextWaypoint = tripAndNextWaypoint.getValue();
                        return new TripDetail(
                            nextWaypoint,
                            getActionForNextWaypoint(nextWaypoint),
                            getRiderDisplayString(nextWaypoint.getAction().getTripResourceInfo()),
                            nextWaypoint.getAction().getTripResourceInfo().getPhoneNumber().orElse(null),
                            geocodedPickups.getOrDefault(nextWaypoint.getTaskId(), null),
                            geocodedDropOffs.getOrDefault(nextWaypoint.getTaskId(), "")
                        );
                    })
                    .collect(Collectors.toList());
            })
            .toObservable();
    }

    private ActionToPerform getActionForNextWaypoint(final Waypoint waypoint) {
        switch (waypoint.getAction().getActionType()) {
            case DRIVE_TO_PICKUP:
                return ActionToPerform.REJECT_TRIP;
            case LOAD_RESOURCE:
                return ActionToPerform.CANCEL_TRIP;
            case DRIVE_TO_DROP_OFF:
                return ActionToPerform.END_TRIP;
        }
        throw new RuntimeException("Unknown waypoint type: " + waypoint.getAction().getActionType().name());
    }

    private Single<Map<String, String>> geocodeLocationCollection(final Map<String, LatLng> locationCollection) {
        return Single.merge(
            locationCollection.entrySet().stream()
                .map(entry -> geocodeInteractor.getBestReverseGeocodeResult(entry.getValue())
                    .firstOrError()
                    .onErrorReturn(Result::failure)
                    .map(result -> {
                        if (result.isSuccess()) {
                            return Pair.create(entry.getKey(), result.get().getDisplayName());
                        } else {
                            return Pair.create(entry.getKey(), "");
                        }
                    })
                )
                .collect(Collectors.toList())
        )
            .toList()
            .observeOn(schedulerProvider.computation())
            .map(locationsByTask -> locationsByTask.stream()
                .collect(Collectors.toMap(
                    taskAndLocation -> taskAndLocation.first,
                    taskAndLocation -> taskAndLocation.second
                ))
            );
    }

    private String getRiderDisplayString(final TripResourceInfo tripResourceInfo) {
        if (tripResourceInfo.getNumPassengers() > 1) {
            final int numberOfRidersExcludingRequester = tripResourceInfo.getNumPassengers() - 1;
            return String.format(
                "%s + %d",
                tripResourceInfo.getNameOfTripRequester(),
                numberOfRidersExcludingRequester
            );
        }
        return tripResourceInfo.getNameOfTripRequester();
    }

    @Override
    public void performActionOnTrip(final TripDetail tripDetail) {
        // TODO handle progress state
        switch (tripDetail.getActionToPerform()) {
            case REJECT_TRIP:
                compositeDisposable.add(
                    vehicleInteractor.rejectTrip(user.getId(), tripDetail.getNextWaypoint().getTaskId())
                        .subscribe(() -> {}, e -> Timber.e(e, "Failed to reject trip"))
                );
                break;
            case CANCEL_TRIP:
                compositeDisposable.add(
                    vehicleInteractor.cancelTrip(tripDetail.getNextWaypoint().getTaskId())
                        .subscribe(() -> {}, e -> Timber.e(e, "Failed to cancel trip"))
                );
                break;
            case END_TRIP:
                compositeDisposable.add(
                    vehicleInteractor.finishSteps(
                        user.getId(),
                        tripDetail.getNextWaypoint().getTaskId(),
                        tripDetail.getNextWaypoint().getStepIds()
                    )
                        .subscribe(() -> {}, e -> Timber.e(e, "Failed to end trip"))
                );
                break;
        }
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        vehicleInteractor.shutDown();
    }
}
