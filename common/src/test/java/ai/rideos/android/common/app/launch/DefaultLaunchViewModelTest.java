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
package ai.rideos.android.common.app.launch;

import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultLaunchViewModelTest {
    private static final int NUM_STEPS = 3;
    private DefaultLaunchViewModel viewModelUnderTest;
    private Runnable doneListener;

    @Before
    public void setUp() {
        doneListener = Mockito.mock(Runnable.class);
        viewModelUnderTest = new DefaultLaunchViewModel(doneListener, NUM_STEPS, new TrampolineSchedulerProvider());
    }

    @Test
    public void testInitialStepIsZero() {
        viewModelUnderTest.getLaunchStepToDisplay().test()
            .assertValueAt(0, 0);
    }

    @Test
    public void testCallingFinishLaunchStepAdvancesStep() {
        viewModelUnderTest.finishLaunchStep(0);
        viewModelUnderTest.getLaunchStepToDisplay().test()
            .assertValueAt(0, 1);
    }

    @Test
    public void testFinishingInvalidStepDoesNothing() {
        viewModelUnderTest.finishLaunchStep(1);
        viewModelUnderTest.getLaunchStepToDisplay().test()
            .assertValueAt(0, 0);
    }

    @Test
    public void testFinishingLastStepCallsDoneListener() {
        viewModelUnderTest.finishLaunchStep(0);
        viewModelUnderTest.finishLaunchStep(1);
        viewModelUnderTest.finishLaunchStep(2);

        // displayable launch step should not advance past 2
        viewModelUnderTest.getLaunchStepToDisplay().test()
            .assertValueAt(0, 2);

        Mockito.verify(doneListener).run();
    }
}
