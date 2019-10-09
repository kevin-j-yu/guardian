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
package ai.rideos.android.rider_app.pre_trip;

import static ai.rideos.android.common.viewmodel.state_machine.StateTransitions.transitionIf;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.RetryBehavior;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.common.viewmodel.state_machine.WorkflowBackStack;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.model.ContactInfo;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.PreTripLocation;
import ai.rideos.android.model.PreTripState;
import ai.rideos.android.model.PreTripState.Step;
import ai.rideos.android.model.VehicleSelectionOption;
import ai.rideos.android.model.VehicleSelectionOption.SelectionType;
import ai.rideos.android.settings.RiderStorageKeys;
import androidx.core.util.Pair;
import com.auth0.android.result.UserProfile;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class DefaultPreTripViewModel implements PreTripViewModel {
    private static final PreTripState INITIAL_STATE = new PreTripState(null, null, 0, null, Step.SELECTING_PICKUP_DROP_OFF);

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final StateMachine<PreTripState> stateMachine;
    private final WorkflowBackStack<PreTripState> backStack;
    private final PublishSubject<Notification> tripCreationFailures = PublishSubject.create();
    private final PreTripState initialState;
    private final RiderTripInteractor tripInteractor;
    private final PreTripListener listener;
    private final SchedulerProvider schedulerProvider;
    private final UserStorageReader userStorageReader;

    public DefaultPreTripViewModel(final PreTripListener listener,
                                   final RiderTripInteractor tripInteractor,
                                   final User user,
                                   final Observable<FleetInfo> observableFleet,
                                   final UserStorageReader userStorageReader) {
        this(
            listener,
            tripInteractor,
            user,
            observableFleet,
            userStorageReader,
            new DefaultSchedulerProvider(),
            INITIAL_STATE,
            RetryBehaviors.getDefault()
        );
    }

    public DefaultPreTripViewModel(final PreTripListener listener,
                                   final RiderTripInteractor tripInteractor,
                                   final User user,
                                   final Observable<FleetInfo> observableFleet,
                                   final UserStorageReader userStorageReader,
                                   final SchedulerProvider schedulerProvider,
                                   final PreTripState initialState,
                                   final RetryBehavior retryBehavior) {
        this.listener = listener;
        this.tripInteractor = tripInteractor;
        this.userStorageReader = userStorageReader;

        this.initialState = initialState;
        stateMachine = new StateMachine<>(schedulerProvider);
        backStack = new WorkflowBackStack<>(
            (state1, state2) -> state1.getStep() == state2.getStep(),
            state -> state.getStep() == Step.CONFIRMED
        );
        this.schedulerProvider = schedulerProvider;

        compositeDisposable.addAll(
            // state machine should be called on main thread to avoid race conditions
            stateMachine.start(),
            backStack.follow(stateMachine),
            stateMachine.observeCurrentState()
                // Switch off single state machine thread
                .observeOn(schedulerProvider.computation())
                .distinctUntilChanged()
                .filter(state -> state.getStep() == Step.CONFIRMED)
                .flatMapSingle(state ->
                    Single.zip(
                        observableFleet.firstOrError(),
                        observeContactInfo(user),
                        Pair::create
                    )
                        .flatMap(fleetAndContact -> createTrip(
                            user.getId(),
                            fleetAndContact.first.getId(),
                            fleetAndContact.second,
                            state,
                            tripInteractor,
                            retryBehavior
                        ))
                )
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        listener.onTripCreated(result.get());
                    } else {
                        tripCreationFailed();
                    }
                })
        );
    }

    @Override
    public void destroy() {
        tripInteractor.shutDown();
        compositeDisposable.dispose();
    }

    public void initialize() {
        backStack.clear();
        stateMachine.initialize(initialState);
    }

    @Override
    public void selectPickupDropOff(final PreTripLocation pickup, final PreTripLocation dropOff) {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.SELECTING_PICKUP_DROP_OFF,
            currentState -> {
                final Step nextStep;
                if (dropOff.wasConfirmedOnMap() && pickup.wasConfirmedOnMap()) {
                    nextStep = Step.CONFIRMING_TRIP;
                } else if (dropOff.wasConfirmedOnMap()) {
                    nextStep = Step.CONFIRMING_PICKUP;
                } else {
                    nextStep = Step.CONFIRMING_DROP_OFF;
                }
                return new PreTripState(
                    pickup,
                    dropOff,
                    currentState.getNumPassengers(),
                    currentState.getVehicleSelection().orElse(null),
                    nextStep
                );
            }
        ));
    }

    @Override
    public void confirmDropOff(final DesiredAndAssignedLocation location) {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.CONFIRMING_DROP_OFF,
            currentState -> {
                final Step nextStep;
                if (currentState.getPickup().wasConfirmedOnMap()) {
                    nextStep = Step.CONFIRMING_TRIP;
                } else {
                    nextStep = Step.CONFIRMING_PICKUP;
                }
                return new PreTripState(
                    currentState.getPickup(),
                    new PreTripLocation(location, true),
                    currentState.getNumPassengers(),
                    currentState.getVehicleSelection().orElse(null),
                    nextStep
                );
            }
        ));
    }

    @Override
    public void confirmPickup(final DesiredAndAssignedLocation location) {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.CONFIRMING_PICKUP,
            currentState -> new PreTripState(
                new PreTripLocation(location, true),
                currentState.getDropOff(),
                currentState.getNumPassengers(),
                currentState.getVehicleSelection().orElse(null),
                Step.CONFIRMING_TRIP
            )
        ));
    }

    @Override
    public void editPickup() {
        stateMachine.transition(currentState -> new PreTripState(
            currentState.getPickup(),
            currentState.getDropOff(),
            LocationSearchFocusType.PICKUP,
            currentState.getNumPassengers(),
            currentState.getVehicleSelection().orElse(null),
            Step.SELECTING_PICKUP_DROP_OFF
        ));
    }

    @Override
    public void editDropOff() {
        stateMachine.transition(currentState -> new PreTripState(
            currentState.getPickup(),
            currentState.getDropOff(),
            LocationSearchFocusType.DROP_OFF,
            currentState.getNumPassengers(),
            currentState.getVehicleSelection().orElse(null),
            Step.SELECTING_PICKUP_DROP_OFF
        ));
    }

    @Override
    public void confirmTrip(final int numPassengers) {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.CONFIRMING_TRIP,
            currentState -> {
                // If vehicle selection is not enabled, jump right to CONFIRMED.
                final boolean isVehicleSelectionEnabled = userStorageReader
                    .getBooleanPreference(RiderStorageKeys.MANUAL_VEHICLE_SELECTION);
                final Step nextStep = isVehicleSelectionEnabled ? Step.CONFIRMING_VEHICLE : Step.CONFIRMED;
                return new PreTripState(
                    currentState.getPickup(),
                    currentState.getDropOff(),
                    numPassengers,
                    currentState.getVehicleSelection().orElse(null),
                    nextStep
                );
            }
        ));
    }

    @Override
    public void confirmVehicle(final VehicleSelectionOption vehicle) {
        stateMachine.transition(transitionIf(
            state -> state.getStep() == Step.CONFIRMING_VEHICLE,
            currentState -> new PreTripState(
                currentState.getPickup(),
                currentState.getDropOff(),
                currentState.getNumPassengers(),
                vehicle,
                Step.CONFIRMED
            )
        ));
    }

    @Override
    public void back() {
        backStack.back(listener::back);
    }

    @Override
    public void navigateUp() {
        stateMachine.transition(currentState -> {
            // Up button only allowed for confirmation steps.
            if (currentState.getStep() == Step.CONFIRMED || currentState.getStep() == Step.SELECTING_PICKUP_DROP_OFF) {
                Timber.e("Cannot navigate up from CONFIRMED or SELECTING_PICKUP_DROP_OFF");
                return currentState;
            }

            // Focus on pickup if confirming pickup, otherwise drop-off
            final LocationSearchFocusType searchFocus = currentState.getStep() == Step.CONFIRMING_PICKUP
                ? LocationSearchFocusType.PICKUP : LocationSearchFocusType.DROP_OFF;
            return new PreTripState(
                currentState.getPickup(),
                currentState.getDropOff(),
                searchFocus,
                currentState.getNumPassengers(),
                currentState.getVehicleSelection().orElse(null),
                Step.SELECTING_PICKUP_DROP_OFF
            );
        });
    }

    @Override
    public void cancelTripRequest() {
        listener.cancelTripRequest();
    }

    @Override
    public Observable<PreTripState> getPreTripState() {
        return stateMachine.observeCurrentState();
    }

    @Override
    public Observable<Notification> getTripCreationFailures() {
        return tripCreationFailures;
    }

    private void tripCreationFailed() {
        tripCreationFailures.onNext(Notification.create());
        stateMachine.transition(currentState -> {
            // Rewind state machine back to confirming trip
            if (currentState.getStep() == Step.CONFIRMED) {
                return new PreTripState(
                    currentState.getPickup(),
                    currentState.getDropOff(),
                    currentState.getNumPassengers(),
                    currentState.getVehicleSelection().orElse(null),
                    Step.CONFIRMING_TRIP
                );
            }
            // If state has transitioned away from CONFIRMED step, don't make any changes. This is not an error.
            return currentState;
        });
    }

    private Single<Result<String>> createTrip(final String passengerId,
                                              final String fleetId,
                                              final ContactInfo contactInfo,
                                              final PreTripState preTripState,
                                              final RiderTripInteractor tripInteractor,
                                              final RetryBehavior retryBehavior) {
        final Observable<String> observableTask;
        if (preTripState.getVehicleSelection().isPresent()
            && preTripState.getVehicleSelection().get().getSelectionType() == SelectionType.MANUAL
            && preTripState.getVehicleSelection().get().getVehicleId().isPresent()) {
            observableTask = tripInteractor
                .createTripForPassengerAndVehicle(
                    passengerId,
                    contactInfo,
                    preTripState.getVehicleSelection().get().getVehicleId().get(),
                    fleetId,
                    preTripState.getNumPassengers(),
                    preTripState.getPickup().getDesiredAndAssignedLocation().getAssignedLocation().getLocation(),
                    preTripState.getDropOff().getDesiredAndAssignedLocation().getAssignedLocation().getLocation()
                );
        } else {
            // Automatic vehicle selection
            observableTask = tripInteractor
                .createTripForPassenger(
                    passengerId,
                    contactInfo,
                    fleetId,
                    preTripState.getNumPassengers(),
                    preTripState.getPickup().getDesiredAndAssignedLocation().getAssignedLocation().getLocation(),
                    preTripState.getDropOff().getDesiredAndAssignedLocation().getAssignedLocation().getLocation()
                );
        }
        return observableTask
            .observeOn(schedulerProvider.computation())
            // retry a few times
            .retryWhen(retryBehavior)
            // return successes
            .map(Result::success)
            .doOnError(throwable -> Timber.e(throwable, "Failed to create task"))
            // return failure with exception if too many retries
            .onErrorReturn(Result::failure)
            .firstOrError();
    }

    private Single<ContactInfo> observeContactInfo(final User user) {
        return Single.zip(
            observeUserName(user),
            Single.just(userStorageReader.getStringPreference(StorageKeys.PHONE_NUMBER)),
            ContactInfo::new
        );
    }

    private Single<String> observeUserName(final User user) {
        final String userName = userStorageReader.getStringPreference(StorageKeys.PREFERRED_NAME);
        if (!userName.isEmpty()) {
            return Single.just(userName);
        }
        return user.fetchUserProfile()
            .map(UserProfile::getEmail)
            .onErrorReturnItem("");
    }
}
