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

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.model.DrivingViewState.DrivingStep;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan.Action;
import ai.rideos.android.model.VehiclePlan.Action.ActionType;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultDrivingViewModelTest {
    private static final LatLng DESTINATION = new LatLng(0, 1);
    private static final Waypoint MOCK_WAYPOINT = new Waypoint(
        "task-1",
        Collections.singletonList("step-1"),
        new Action(DESTINATION, ActionType.DRIVE_TO_PICKUP, new TripResourceInfo(1, "Rider"))
    );

    private DefaultDrivingViewModel viewModelUnderTest;
    private FinishedDrivingListener drivingListener;

    @Test
    public void testCanObserveInitialStep() {
        assertStateTransition(DrivingStep.DRIVE_PENDING, () -> {}, DrivingStep.DRIVE_PENDING);
    }

    @Test
    public void testDrivePendingTransitionsToNavigating() {
        assertStateTransition(DrivingStep.DRIVE_PENDING, () -> viewModelUnderTest.startNavigation(), DrivingStep.NAVIGATING);
    }

    @Test
    public void testNavigatingTransitionsToConfirmingArrival() {
        assertStateTransition(DrivingStep.NAVIGATING, () -> viewModelUnderTest.finishedNavigation(), DrivingStep.CONFIRMING_ARRIVAL);
    }

    @Test
    public void testNavigationErrorTransitionsToConfirmingArrival() {
        assertStateTransition(
            DrivingStep.NAVIGATING,
            () -> viewModelUnderTest.finishedNavigationWithError(new IOException()),
            DrivingStep.CONFIRMING_ARRIVAL
        );
    }

    @Test
    public void testConfirmArrivalCallsFinishDrivingListener() {
        assertStateTransition(DrivingStep.CONFIRMING_ARRIVAL, () -> viewModelUnderTest.confirmArrival(), DrivingStep.CONFIRMING_ARRIVAL);
        Mockito.verify(drivingListener).finishedDriving(MOCK_WAYPOINT);
    }

    @Test
    public void testInvalidStateTransitionsReturnSameStep() {
        assertStateTransition(DrivingStep.NAVIGATING, () -> viewModelUnderTest.startNavigation(), DrivingStep.NAVIGATING);
        assertStateTransition(DrivingStep.CONFIRMING_ARRIVAL, () -> viewModelUnderTest.finishedNavigation(), DrivingStep.CONFIRMING_ARRIVAL);
        assertStateTransition(
            DrivingStep.CONFIRMING_ARRIVAL,
            () -> viewModelUnderTest.finishedNavigationWithError(new IOException()),
            DrivingStep.CONFIRMING_ARRIVAL
        );
    }

    private void assertStateTransition(final DrivingStep currentStep, final Runnable action, final DrivingStep nextStep) {
        setUpWithInitialStep(currentStep);
        action.run();
        viewModelUnderTest.getDrivingViewState().test()
            .assertValueCount(1)
            .assertValueAt(0, viewState -> viewState.getDrivingStep() == nextStep);
    }

    private void setUpWithInitialStep(final DrivingStep step) {
        drivingListener = Mockito.mock(FinishedDrivingListener.class);
        viewModelUnderTest = new DefaultDrivingViewModel(
            drivingListener,
            step,
            new TrampolineSchedulerProvider()
        );
        viewModelUnderTest.initialize(MOCK_WAYPOINT);
    }
}
