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
package ai.rideos.android.rider_app.on_trip;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.common.viewmodel.state_machine.StateTransitions;
import ai.rideos.android.interactors.RiderTripInteractor;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.OnTripDisplayState;
import ai.rideos.android.model.OnTripDisplayState.Display;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class DefaultOnTripViewModel implements OnTripViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final StateMachine<OnTripDisplayState> stateMachine;
    private final RiderTripInteractor tripInteractor;
    private final OnTripListener onTripListener;
    private final SchedulerProvider schedulerProvider;

    public DefaultOnTripViewModel(final User user,
                                  final RiderTripInteractor tripInteractor,
                                  final OnTripListener listener) {
        this(user, tripInteractor, listener, new DefaultSchedulerProvider());
    }

    public DefaultOnTripViewModel(final User user,
                                  final RiderTripInteractor tripInteractor,
                                  final OnTripListener listener,
                                  final SchedulerProvider schedulerProvider) {
        this.tripInteractor = tripInteractor;
        this.onTripListener = listener;
        this.schedulerProvider = schedulerProvider;

        stateMachine = new StateMachine<>(schedulerProvider);
        compositeDisposable.addAll(
            stateMachine.start(),
            subscribeToCancellations(user.getId()),
            subscribeToTripEdits()
        );
    }

    @Override
    public void initialize(final String tripId) {
        stateMachine.initialize(new OnTripDisplayState(Display.CURRENT_TRIP, tripId));
    }

    @Override
    public void confirmPickup(final DesiredAndAssignedLocation newPickup) {
        stateMachine.transition(StateTransitions.transitionIf(
            state -> state.getDisplay() == Display.EDITING_PICKUP,
            state -> new OnTripDisplayState(Display.CONFIRMING_EDIT_PICKUP, state.getTripId(), newPickup.getAssignedLocation())
        ));
    }

    @Override
    public void confirmDropOff(final DesiredAndAssignedLocation location) {
        // Not supported
        Timber.e("Changing drop-off while on-trip is not yet supported");
    }

    @Override
    public Observable<OnTripDisplayState> getDisplayState() {
        return stateMachine.observeCurrentState().distinctUntilChanged();
    }

    @Override
    public void cancelTrip() {
        stateMachine.transition(StateTransitions.transitionIf(
            state -> state.getDisplay() == Display.CURRENT_TRIP,
            state -> new OnTripDisplayState(Display.CONFIRMING_CANCEL, state.getTripId())
        ));
    }

    @Override
    public void changePickup() {
        stateMachine.transition(StateTransitions.transitionIf(
            state -> state.getDisplay() == Display.CURRENT_TRIP,
            state -> new OnTripDisplayState(Display.EDITING_PICKUP, state.getTripId())
        ));
    }

    @Override
    public void tripFinished() {
        onTripListener.tripFinished();
    }

    private Disposable subscribeToCancellations(final String passengerId) {
        return stateMachine.observeCurrentState()
            .observeOn(schedulerProvider.computation())
            .distinctUntilChanged()
            .filter(state -> state.getDisplay() == Display.CONFIRMING_CANCEL)
            .flatMapSingle(state -> tripInteractor.cancelTrip(passengerId, state.getTripId())
                // map completable to Single<Result> so that there are no errors in the pipeline
                .doOnError(throwable -> Timber.e(throwable, "Failed to cancel task"))
                .toSingleDefault(Result.success(true))
                .onErrorReturn(Result::failure)
            )
            // After a cancellation occurs, go back to polling the current trip
            // If the cancellation was successful, it will show the cancellation dialog
            // If not, it will continue showing the current state
            .subscribe(result -> stateMachine.transition(StateTransitions.transitionIf(
                state -> state.getDisplay() == Display.CONFIRMING_CANCEL,
                state -> new OnTripDisplayState(Display.CURRENT_TRIP, state.getTripId())
            )));
    }

    private Disposable subscribeToTripEdits() {
        return stateMachine.observeCurrentState()
            .observeOn(schedulerProvider.computation())
            .distinctUntilChanged()
            .filter(state -> state.getDisplay() == Display.CONFIRMING_EDIT_PICKUP)
            .flatMapSingle(state -> tripInteractor
                .editPickup(state.getTripId(), state.getPickupLocation().get().getLocation())
                .doOnError(throwable -> Timber.e(throwable, "Failed to edit task"))
                // We can ignore the new task id, because it is picked up by the MainViewModel.
                // We just care if there's an error here so we can allow the user to retry.
                .map(taskId -> Result.success(true))
                .onErrorReturn(Result::failure)
                .firstOrError()
            )
            .subscribe(result -> {
                // On a successful result, just wait for the current task to be updated from the main view model
                if (result.isFailure()) {
                    stateMachine.transition(StateTransitions.transitionIf(
                        state -> state.getDisplay() == Display.CONFIRMING_EDIT_PICKUP,
                        state -> new OnTripDisplayState(Display.CURRENT_TRIP, state.getTripId())
                    ));
                }
            });
    }

    @Override
    public void back() {
        stateMachine.transition(StateTransitions.transitionIf(
            state -> state.getDisplay() == Display.EDITING_PICKUP,
            state -> new OnTripDisplayState(Display.CURRENT_TRIP, state.getTripId())
        ));
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        tripInteractor.shutDown();
    }

    @Override
    public void navigateUp() {
        stateMachine.transition(StateTransitions.transitionIf(
            state -> state.getDisplay() == Display.EDITING_PICKUP,
            state -> new OnTripDisplayState(Display.CURRENT_TRIP, state.getTripId())
        ));
    }
}
