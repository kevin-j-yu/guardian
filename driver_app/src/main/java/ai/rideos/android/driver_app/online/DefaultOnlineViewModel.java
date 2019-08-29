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
import ai.rideos.android.driver_app.online.idle.GoOfflineListener;
import ai.rideos.android.interactors.DriverPlanInteractor;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import ai.rideos.android.model.OnlineViewState;
import ai.rideos.android.model.OnlineViewState.DisplayType;
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
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class DefaultOnlineViewModel implements OnlineViewModel {
    private static final int DEFAULT_POLL_INTERVAL_MILLIS = 2000;
    private static final int DEFAULT_RETRY_COUNT = 2;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // send any value to this subject to initiate syncing the vehicle state
    private final PublishSubject<Boolean> forceSyncSubject = PublishSubject.create();
    private final BehaviorSubject<VehiclePlan> currentPlan;
    private final BehaviorSubject<LocationAndHeading> currentLocation = BehaviorSubject.create();

    private final GoOfflineListener listener;
    private final DriverVehicleInteractor vehicleInteractor;
    private final DriverPlanInteractor planInteractor;
    private final User user;
    private final SchedulerProvider schedulerProvider;
    private final int retryCount;

    public DefaultOnlineViewModel(final GoOfflineListener listener,
                                  final DriverVehicleInteractor vehicleInteractor,
                                  final DriverPlanInteractor planInteractor,
                                  final ExternalVehicleRouteSynchronizer vehicleRouteSynchronizer,
                                  final DeviceLocator deviceLocator,
                                  final User user) {
        this(
            listener,
            vehicleInteractor,
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
                                  final DriverVehicleInteractor vehicleInteractor,
                                  final DriverPlanInteractor planInteractor,
                                  final ExternalVehicleRouteSynchronizer vehicleRouteSynchronizer,
                                  final DeviceLocator deviceLocator,
                                  final User user,
                                  final SchedulerProvider schedulerProvider,
                                  final int pollIntervalMillis,
                                  final int retryCount) {
        this.listener = listener;
        this.vehicleInteractor = vehicleInteractor;
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
    public void pickedUpPassenger(final Waypoint waypointToComplete) {
        completeWaypoint(waypointToComplete);
    }

    @Override
    public void finishedDriving(final Waypoint waypointToComplete) {
        completeWaypoint(waypointToComplete);
    }

    private void completeWaypoint(final Waypoint waypointToComplete) {
        final VehiclePlan plan = currentPlan.getValue();
        if (plan == null || plan.getWaypoints().size() == 0) {
            return;
        }
        compositeDisposable.add(
            vehicleInteractor.finishSteps(
                user.getId(),
                waypointToComplete.getTaskId(),
                waypointToComplete.getStepIds()
            )
                .observeOn(schedulerProvider.computation())
                .retry(retryCount)
                // force state to update
                .subscribe(
                    () -> forceSyncSubject.onNext(true),
                    error -> Timber.e(error, "Failed to complete waypoint")
                )
        );
    }

    @Override
    public Observable<OnlineViewState> getOnlineViewState() {
        return currentPlan.observeOn(schedulerProvider.computation())
            .map(plan -> {
                if (plan.getWaypoints().isEmpty()) {
                    return new OnlineViewState(DisplayType.IDLE, null);
                }
                final Waypoint currentWaypoint = plan.getWaypoints().get(0);
                switch (currentWaypoint.getAction().getActionType()) {
                    case DRIVE_TO_PICKUP:
                        return new OnlineViewState(DisplayType.DRIVING_TO_PICKUP, currentWaypoint);
                    case DRIVE_TO_DROP_OFF:
                        return new OnlineViewState(DisplayType.DRIVING_TO_DROP_OFF, currentWaypoint);
                    case LOAD_RESOURCE:
                        return new OnlineViewState(DisplayType.WAITING_FOR_PASSENGER, currentWaypoint);
                }
                throw new IllegalStateException("Unknown step action " + currentWaypoint.getAction().getActionType().name());
            })
            // If the display is equivalent (same display type AND current waypoint), don't update
            .distinctUntilChanged();
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
        vehicleInteractor.shutDown();
    }
}
