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
package ai.rideos.android.driver_app.online.driving;

import static ai.rideos.android.common.viewmodel.state_machine.StateTransitions.transitionIf;

import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.model.DrivingViewState;
import ai.rideos.android.model.DrivingViewState.DrivingStep;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class DefaultDrivingViewModel implements DrivingViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final StateMachine<DrivingViewState> stateMachine;

    private final FinishedDrivingListener listener;
    private final DrivingStep initialStep;

    public DefaultDrivingViewModel(final FinishedDrivingListener listener) {
        this(
            listener,
            DrivingStep.DRIVE_PENDING,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultDrivingViewModel(final FinishedDrivingListener listener,
                                   final DrivingStep initialStep,
                                   final SchedulerProvider schedulerProvider) {
        this.listener = listener;
        this.initialStep = initialStep;
        stateMachine = new StateMachine<>(schedulerProvider);

        compositeDisposable.add(stateMachine.start());
    }

    @Override
    public void initialize(final Waypoint waypointToComplete) {
        stateMachine.initialize(new DrivingViewState(initialStep, waypointToComplete));
    }

    @Override
    public void startNavigation() {
        stateMachine.transition(transitionIf(
            state -> state.getDrivingStep() == DrivingStep.DRIVE_PENDING,
            state -> new DrivingViewState(DrivingStep.NAVIGATING, state.getWaypointToComplete())
        ));
    }

    @Override
    public void finishedNavigation() {
        stateMachine.transition(transitionIf(
            state -> state.getDrivingStep() == DrivingStep.NAVIGATING,
            state -> new DrivingViewState(DrivingStep.CONFIRMING_ARRIVAL, state.getWaypointToComplete())
        ));
    }

    @Override
    public void finishedNavigationWithError(final Throwable throwable) {
        stateMachine.transition(transitionIf(
            state -> state.getDrivingStep() == DrivingStep.NAVIGATING,
            // Go right to confirm arrival, so that the driver can continue with the trip despite navigation issues.
            state -> new DrivingViewState(DrivingStep.CONFIRMING_ARRIVAL, state.getWaypointToComplete())
        ));
    }

    @Override
    public void confirmArrival() {
        stateMachine.transition(transitionIf(
            state -> state.getDrivingStep() == DrivingStep.CONFIRMING_ARRIVAL,
            state -> {
                listener.finishedDriving(state.getWaypointToComplete());
                return state;
            }
        ));
    }

    @Override
    public Observable<DrivingViewState> getDrivingViewState() {
        return stateMachine.observeCurrentState().distinctUntilChanged();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }
}
