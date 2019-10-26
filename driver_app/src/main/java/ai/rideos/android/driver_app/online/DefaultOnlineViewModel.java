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
import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.SetOperations;
import ai.rideos.android.driver_app.online.idle.GoOfflineListener;
import ai.rideos.android.interactors.DriverPlanInteractor;
import ai.rideos.android.model.DriverAlert;
import ai.rideos.android.model.DriverAlert.DriverAlertType;
import ai.rideos.android.model.OnlineViewState;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.core.util.Pair;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import timber.log.Timber;

public class DefaultOnlineViewModel implements OnlineViewModel {
    private static final int DEFAULT_POLL_INTERVAL_MILLIS = 2000;
    private static final int DEFAULT_RETRY_COUNT = 2;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // send any value to this subject to initiate syncing the vehicle state
    private final PublishSubject<Boolean> forceSyncSubject = PublishSubject.create();
    private final BehaviorSubject<VehiclePlan> currentPlan;
    private final BehaviorSubject<Boolean> shouldShowTripDetailsSubject = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<LocationAndHeading> currentLocation = BehaviorSubject.create();

    private final GoOfflineListener listener;
    private final DriverPlanInteractor planInteractor;
    private final User user;
    private final SchedulerProvider schedulerProvider;
    private final int retryCount;

    public DefaultOnlineViewModel(final GoOfflineListener listener,
                                  final DriverPlanInteractor planInteractor,
                                  final ExternalVehicleRouteSynchronizer vehicleRouteSynchronizer,
                                  final DeviceLocator deviceLocator,
                                  final User user) {
        this(
            listener,
            planInteractor,
            vehicleRouteSynchronizer,
            deviceLocator,
            user,
            new DefaultSchedulerProvider(),
            DEFAULT_POLL_INTERVAL_MILLIS,
            DEFAULT_RETRY_COUNT
        );
    }

    public DefaultOnlineViewModel(final GoOfflineListener listener,
                                  final DriverPlanInteractor planInteractor,
                                  final ExternalVehicleRouteSynchronizer vehicleRouteSynchronizer,
                                  final DeviceLocator deviceLocator,
                                  final User user,
                                  final SchedulerProvider schedulerProvider,
                                  final int pollIntervalMillis,
                                  final int retryCount) {
        this.listener = listener;
        this.planInteractor = planInteractor;
        this.user = user;
        this.schedulerProvider = schedulerProvider;
        this.retryCount = retryCount;

        currentPlan = BehaviorSubject.createDefault(new VehiclePlan(Collections.emptyList()));

        compositeDisposable.addAll(
            deviceLocator.observeCurrentLocation(pollIntervalMillis)
                .subscribe(currentLocation::onNext),
            subscribeToPlanUpdates(pollIntervalMillis),
            currentPlan.toFlowable(BackpressureStrategy.LATEST)
                .flatMapSingle(plan -> currentLocation.firstOrError().map(location -> Pair.create(plan, location)))
                .flatMapCompletable(planAndLocation ->
                    vehicleRouteSynchronizer.synchronizeForPlan(planAndLocation.first, planAndLocation.second)
                )
                .subscribe()
        );
    }

    @Override
    public void pickedUpPassenger() {
        forceSyncSubject.onNext(true);
    }

    @Override
    public void finishedDriving() {
        forceSyncSubject.onNext(true);
    }

    @Override
    public Observable<OnlineViewState> getOnlineViewState() {
        return Observable.combineLatest(
            currentPlan,
            shouldShowTripDetailsSubject,
            Pair::create
        )
            .observeOn(schedulerProvider.computation())
            .map(planAndShouldShowDetails -> {
                final VehiclePlan plan = planAndShouldShowDetails.first;
                final boolean shouldShowTripDetails = planAndShouldShowDetails.second;
                if (shouldShowTripDetails) {
                    return OnlineViewState.tripDetails(plan);
                }
                if (plan.getWaypoints().isEmpty()) {
                    return OnlineViewState.idle();
                }
                final Waypoint currentWaypoint = plan.getWaypoints().get(0);
                switch (currentWaypoint.getAction().getActionType()) {
                    case DRIVE_TO_PICKUP:
                        return OnlineViewState.drivingToPickup(currentWaypoint);
                    case DRIVE_TO_DROP_OFF:
                        return OnlineViewState.drivingToDropOff(currentWaypoint);
                    case LOAD_RESOURCE:
                        return OnlineViewState.waitingForPassenger(currentWaypoint);
                }
                throw new IllegalStateException("Unknown step action " + currentWaypoint.getAction().getActionType().name());
            })
            // If the display is equivalent (same display type AND current waypoint), don't update
            .distinctUntilChanged();
    }

    @Override
    public Observable<DriverAlert> getDriverAlerts() {
        final Observable<Pair<VehiclePlan, VehiclePlan>> planObservable = currentPlan.scan(
            Pair.create(null, new VehiclePlan(Collections.emptyList())),
            (seed, newPlan) -> Pair.create(seed.second, newPlan)
        );

        return planObservable
            .observeOn(schedulerProvider.computation())
            .flatMap(oldAndNewPlan -> {
                if (oldAndNewPlan.first == null || oldAndNewPlan.second == null) {
                    return Observable.empty();
                }
                final Map<String, TripResourceInfo> oldTrips = getTripsInPlan(oldAndNewPlan.first);
                final Map<String, TripResourceInfo> newTrips = getTripsInPlan(oldAndNewPlan.second);
                final SetOperations.DiffResult<String> tripDifferences = SetOperations.getDifferences(
                    oldTrips.keySet(),
                    newTrips.keySet()
                );
                // TODO - Until we can tell what happened to old trips (i.e. if they were completed, cancelled or
                //  replaced), then only alert when no old trips are removed
                if (tripDifferences.getOnlyOnLeft().isEmpty()) {
                    return Observable.fromIterable(
                        tripDifferences.getOnlyOnRight().stream()
                            .map(newTrip -> new DriverAlert(DriverAlertType.NEW_REQUEST, newTrips.get(newTrip)))
                            .collect(Collectors.toList())
                    );
                }
                return Observable.empty();
            });
    }

    private static Map<String, TripResourceInfo> getTripsInPlan(final VehiclePlan vehiclePlan) {
        final Map<String, TripResourceInfo> resourcesByTripId = new HashMap<>();
        for (final Waypoint waypoint : vehiclePlan.getWaypoints()) {
            if (!resourcesByTripId.containsKey(waypoint.getTaskId())) {
                resourcesByTripId.put(waypoint.getTaskId(), waypoint.getAction().getTripResourceInfo());
            }
        }
        return resourcesByTripId;
    }

    @Override
    public void didGoOffline() {
        listener.didGoOffline();
    }

    private Disposable subscribeToPlanUpdates(final int updateIntervalMillis) {
        // Poll current location to sync at a certain interval. If an update should be forced, a notification is
        // emitted to forceSyncSubject.
        return Flowable.combineLatest(
            Observable.interval(0, updateIntervalMillis, TimeUnit.MILLISECONDS, schedulerProvider.io())
                .toFlowable(BackpressureStrategy.DROP),
            forceSyncSubject.startWith(true).observeOn(schedulerProvider.computation())
                .toFlowable(BackpressureStrategy.DROP), // we don't care about missed syncs
            (timer, forceSync) -> Notification.create()
        )
            .observeOn(schedulerProvider.computation())
            // Drop location updates when getPlanForVehicle is backed up
            .onBackpressureDrop()
            // Sync the vehicle state using the latest state
            .flatMap(notification -> getPlanAndRetry())
            // Ignore failures (it will be retried after the polling interval regardless)
            .filter(Result::isSuccess)
            .map(Result::get)
            .subscribe(currentPlan::onNext);
    }

    private Flowable<Result<VehiclePlan>> getPlanAndRetry() {
        return planInteractor.getPlanForVehicle(user.getId())
            .observeOn(schedulerProvider.computation())
            // Retry a few times
            .retry(retryCount)
            .doOnError(e -> Timber.e(e, "Failed to get plan"))
            // Return success/failure based on error emitted
            .map(Result::success)
            .onErrorReturn(Result::failure)
            // We should only receive one plan at a time, so drop the rest if this ever happens
            .toFlowable(BackpressureStrategy.DROP);
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }

    @Override
    public void openTripDetails() {
        shouldShowTripDetailsSubject.onNext(true);
    }

    @Override
    public void closeTripDetails() {
        shouldShowTripDetailsSubject.onNext(false);
    }
}
