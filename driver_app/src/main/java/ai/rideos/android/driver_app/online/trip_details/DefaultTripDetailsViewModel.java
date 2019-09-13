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
import ai.rideos.android.driver_app.online.trip_details.TripDetail.ActionToPerform;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import timber.log.Timber;

public class DefaultTripDetailsViewModel implements TripDetailsViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final DriverVehicleInteractor vehicleInteractor;
    private final User user;
    private final VehiclePlan vehiclePlan;

    public DefaultTripDetailsViewModel(final DriverVehicleInteractor vehicleInteractor,
                                       final User user,
                                       final VehiclePlan vehiclePlan) {
        this.vehicleInteractor = vehicleInteractor;
        this.user = user;
        this.vehiclePlan = vehiclePlan;
    }

    @Override
    public Observable<List<TripDetail>> getTripDetails() {
        final LinkedHashMap<String, Waypoint> tripsAndNextWaypoint = new LinkedHashMap<>();
        for (final Waypoint waypoint : vehiclePlan.getWaypoints()) {
            if (!tripsAndNextWaypoint.containsKey(waypoint.getTaskId())) {
                tripsAndNextWaypoint.put(waypoint.getTaskId(), waypoint);
            }
        }

        final List<TripDetail> tripDetails = tripsAndNextWaypoint.entrySet().stream()
            .map(tripAndNextWaypoint -> {
                final Waypoint nextWaypoint = tripAndNextWaypoint.getValue();
                final ActionToPerform actionToPerform = nextWaypoint.getAction().getActionType() == ActionType.DRIVE_TO_DROP_OFF
                    ? ActionToPerform.END_TRIP
                    : ActionToPerform.REJECT_TRIP;
                return new TripDetail(
                    nextWaypoint,
                    actionToPerform,
                    nextWaypoint.getAction().getTripResourceInfo().getNameOfTripRequester(), // TODO use rider count
                    null // TODO get passenger phone from backend
                );
            })
            .collect(Collectors.toList());
        return Observable.just(tripDetails);
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
