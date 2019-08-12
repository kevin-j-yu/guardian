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
package ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off;

import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.viewmodel.state_machine.StateMachine;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.PreTripLocation;
import ai.rideos.android.model.SelectPickupDropOffDisplayState;
import ai.rideos.android.model.SelectPickupDropOffDisplayState.SetPickupDropOffStep;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

public class DefaultSelectPickupDropOffViewModel implements SelectPickupDropOffViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final StateMachine<LocationState> locationStateMachine;
    private final BehaviorSubject<SetPickupDropOffStep> stepSubject;
    private final SetPickupDropOffListener listener;
    private final SchedulerProvider schedulerProvider;

    public DefaultSelectPickupDropOffViewModel(final SetPickupDropOffListener listener) {
        this(listener, new DefaultSchedulerProvider());
    }

    public DefaultSelectPickupDropOffViewModel(final SetPickupDropOffListener listener,
                                               final SchedulerProvider schedulerProvider) {
        this.listener = listener;
        this.schedulerProvider = schedulerProvider;
        stepSubject = BehaviorSubject.createDefault(SetPickupDropOffStep.SEARCHING_FOR_PICKUP_DROP_OFF);
        locationStateMachine = new StateMachine<>(schedulerProvider);

        compositeDisposable.addAll(
            locationStateMachine.start(),
            locationStateMachine.observeCurrentState()
                .observeOn(schedulerProvider.computation())
                .subscribe(locationState -> {
                    if (locationState.completed()) {
                        listener.selectPickupDropOff(locationState.pickup, locationState.dropOff);
                    } else {
                        // If one or the other is not set
                        stepSubject.onNext(SetPickupDropOffStep.SEARCHING_FOR_PICKUP_DROP_OFF);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }

    @Override
    public void initialize(@Nullable final PreTripLocation initialPickup,
                           @Nullable final PreTripLocation initialDropOff,
                           final LocationSearchFocusType initialFocus) {
        locationStateMachine.initialize(new LocationState(initialPickup, initialDropOff, initialFocus, false));
    }

    @Override
    public void selectPickup(final NamedTaskLocation pickup) {
        locationStateMachine.transition(currentState -> new LocationState(
            new PreTripLocation(new DesiredAndAssignedLocation(pickup), false),
            currentState.dropOff,
            true
        ));
    }

    @Override
    public void selectDropOff(final NamedTaskLocation dropOff) {
        locationStateMachine.transition(currentState -> new LocationState(
            currentState.pickup,
            new PreTripLocation(new DesiredAndAssignedLocation(dropOff), false),
            true
        ));
    }

    @Override
    public void confirmPickup(final DesiredAndAssignedLocation pickup) {
        locationStateMachine.transition(currentState -> new LocationState(
            new PreTripLocation(pickup, true),
            currentState.dropOff,
            true
        ));
    }

    @Override
    public void confirmDropOff(final DesiredAndAssignedLocation dropOff) {
        locationStateMachine.transition(currentState -> new LocationState(
            currentState.pickup,
            new PreTripLocation(dropOff, true),
            true
        ));
    }

    @Override
    public void setPickupOnMap() {
        stepSubject.onNext(SetPickupDropOffStep.SETTING_PICKUP_ON_MAP);
    }

    @Override
    public void setDropOffOnMap() {
        stepSubject.onNext(SetPickupDropOffStep.SETTING_DROP_OFF_ON_MAP);
    }

    @Override
    public void doneSearching() {
        // if doneSearching is called, then just flip the "changedByUser" flag to true to advance to the next screen.
        locationStateMachine.transition(currentState -> new LocationState(
            currentState.pickup,
            currentState.dropOff,
            true
        ));
    }

    @Override
    public void back() {
        // If back is called while setting the pickup/drop-off on the map, go back to location search. Otherwise, call
        // the parent's back method.
        final SetPickupDropOffStep currentStep = stepSubject.getValue();
        if (currentStep == SetPickupDropOffStep.SETTING_DROP_OFF_ON_MAP || currentStep == SetPickupDropOffStep.SETTING_PICKUP_ON_MAP) {
            stepSubject.onNext(SetPickupDropOffStep.SEARCHING_FOR_PICKUP_DROP_OFF);
        } else {
            listener.back();
        }
    }

    @Override
    public void navigateUp() {
        // Up and back both just go to the previous screen in this flow.
        back();
    }

    @Override
    public Observable<SelectPickupDropOffDisplayState> getDisplayState() {
        return stepSubject.observeOn(schedulerProvider.computation())
            .distinctUntilChanged()
            .map(step -> {
                final LocationState locationState = locationStateMachine.getCurrentState();
                return new SelectPickupDropOffDisplayState(
                    step,
                    locationState.pickup == null
                        ? null
                        : locationState.pickup.getDesiredAndAssignedLocation().getDesiredLocation(),
                    locationState.dropOff == null
                        ? null
                        : locationState.dropOff.getDesiredAndAssignedLocation().getDesiredLocation(),
                    locationState.focus
                );
            });
    }

    private static class LocationState {
        private final PreTripLocation pickup;
        private final PreTripLocation dropOff;
        private final LocationSearchFocusType focus;
        // If there is already a pickup/drop-off selected by the user (like if they go back from the confirmation steps)
        // we do not want to automatically advance because both are selected. So, we can use this extra flag that is
        // true when the user has interacted in some way. For example, if the pickup and drop off are already selected,
        // the user can tap "done" to advance to the next screen. If the pickup is set, but the drop-off is not, then
        // they can select a drop-off and advance to the next screen.
        private final boolean changedByUser;

        // Using this constructor, the focus is decided by whether the pickup/drop-off locations are set.
        LocationState(final PreTripLocation pickup,
                      final PreTripLocation dropOff,
                      final boolean changedByUser) {
            this.pickup = pickup;
            this.dropOff = dropOff;
            this.changedByUser = changedByUser;
            if (pickup == null && dropOff != null) {
                focus = LocationSearchFocusType.PICKUP;
            } else {
                focus = LocationSearchFocusType.DROP_OFF;
            }
        }

        // Using this constructor, the focus is forcefully set.
        LocationState(final PreTripLocation pickup,
                      final PreTripLocation dropOff,
                      final LocationSearchFocusType focus,
                      final boolean changedByUser) {
            this.pickup = pickup;
            this.dropOff = dropOff;
            this.changedByUser = changedByUser;
            this.focus = focus;
        }

        // Completed if the pickup is set, the drop off is set, and there has been some interaction by the user
        boolean completed() {
            return pickup != null && dropOff != null && changedByUser;
        }
    }
}
