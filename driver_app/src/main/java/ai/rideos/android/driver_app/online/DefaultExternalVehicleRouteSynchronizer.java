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
package ai.rideos.android.driver_app.online;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.RouteInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.VehicleDisplayRouteLeg;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.core.util.Pair;
import io.reactivex.Completable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import timber.log.Timber;

public class DefaultExternalVehicleRouteSynchronizer implements ExternalVehicleRouteSynchronizer {
    private final DriverVehicleInteractor vehicleInteractor;
    private final RouteInteractor routeInteractor;
    private final User user;
    private final SchedulerProvider schedulerProvider;

    public DefaultExternalVehicleRouteSynchronizer(final DriverVehicleInteractor vehicleInteractor,
                                                   final RouteInteractor routeInteractor,
                                                   final User user) {
        this(vehicleInteractor, routeInteractor, user, new DefaultSchedulerProvider());
    }

    public DefaultExternalVehicleRouteSynchronizer(final DriverVehicleInteractor vehicleInteractor,
                                                   final RouteInteractor routeInteractor,
                                                   final User user,
                                                   final SchedulerProvider schedulerProvider) {
        this.vehicleInteractor = vehicleInteractor;
        this.routeInteractor = routeInteractor;
        this.user = user;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Completable synchronizeForPlan(final VehiclePlan plan, final LocationAndHeading currentLocation) {
        final List<Waypoint> filteredSteps = filterRoutableSteps(plan);
        if (filteredSteps.isEmpty()) {
            // Just update the location
            return vehicleInteractor.updateVehicleLocation(user.getId(), currentLocation)
                .doOnError(e -> Timber.e(e, "Could not synchronize vehicle route with backend"))
                .onErrorComplete();
        }
        return routeInteractor.getRouteForWaypoints(getRoutableWaypoints(filteredSteps, currentLocation))
            .observeOn(schedulerProvider.computation())
            .map(routes -> collectRoutesByStep(routes, filteredSteps))
            .map(routesByStep -> toDisplayRoutes(plan, routesByStep))
            .flatMapCompletable(routeLegs -> Completable.mergeArray(
                vehicleInteractor.updateVehicleLocation(user.getId(), currentLocation),
                vehicleInteractor.updateVehicleRoute(user.getId(), routeLegs)
            ))
            .doOnError(e -> Timber.e(e, "Could not synchronize vehicle route with backend"))
            .onErrorComplete();
    }

    private static Map<Pair<String, String>, RouteInfoModel> collectRoutesByStep(
        final List<RouteInfoModel> routes,
        final List<Waypoint> routableWaypoints
    ) {
        return IntStream.range(0, routableWaypoints.size())
            .boxed()
            .collect(Collectors.toMap(i -> toWaypointKey(routableWaypoints.get(i)), routes::get));
    }

    private static List<VehicleDisplayRouteLeg> toDisplayRoutes(
        final VehiclePlan plan,
        final Map<Pair<String, String>, RouteInfoModel> routesByStep
    ) {
        final List<VehicleDisplayRouteLeg> routes = new ArrayList<>(routesByStep.size());
        for (int i = 0; i < plan.getWaypoints().size(); i++) {
            final Waypoint currentWaypoint = plan.getWaypoints().get(i);
            if (!isRoutable(currentWaypoint)) {
                continue;
            }
            final Pair<String, String> waypointKey = toWaypointKey(currentWaypoint);
            if (i == 0) {
                routes.add(new VehicleDisplayRouteLeg(null, waypointKey, routesByStep.get(waypointKey)));
            } else {
                final Waypoint previousWaypoint = plan.getWaypoints().get(i - 1);
                routes.add(new VehicleDisplayRouteLeg(
                    Pair.create(
                        previousWaypoint.getTaskId(),
                        // previous step should be the last step in the previous waypoint
                        previousWaypoint.getStepIds().get(previousWaypoint.getStepIds().size() - 1)
                    ),
                    waypointKey,
                    routesByStep.get(waypointKey)
                ));
            }
        }
        return routes;
    }

    private static List<Waypoint> filterRoutableSteps(final VehiclePlan plan) {
        return plan.getWaypoints().stream()
            .filter(DefaultExternalVehicleRouteSynchronizer::isRoutable)
            .collect(Collectors.toList());
    }

    private static boolean isRoutable(final Waypoint waypoint) {
        return waypoint.getAction().getActionType() == ActionType.DRIVE_TO_DROP_OFF
            || waypoint.getAction().getActionType() == ActionType.DRIVE_TO_PICKUP;
    }

    private static List<LatLng> getRoutableWaypoints(final List<Waypoint> waypoints,
                                                     final LocationAndHeading currentLocation) {
        final List<LatLng> routableWaypoints = new ArrayList<>(waypoints.size() + 1);
        routableWaypoints.add(currentLocation.getLatLng());
        routableWaypoints.addAll(
            waypoints.stream()
                .map(waypoint -> waypoint.getAction().getDestination())
                .collect(Collectors.toList())
        );
        return routableWaypoints;
    }

    /**
     * This key identifies a Waypoint based on the current assumption that each waypoint has a unique task and first
     * step.
     */
    private static Pair<String, String> toWaypointKey(final Waypoint waypoint) {
        return Pair.create(waypoint.getTaskId(), waypoint.getStepIds().get(0));
    }
}
