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

import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class DefaultLaunchViewModel implements LaunchViewModel {
    private final Runnable launchDoneListener;
    private final BehaviorSubject<Integer> stepSubject;
    private final int maxStep;
    private final SchedulerProvider schedulerProvider;

    public DefaultLaunchViewModel(final Runnable launchDoneListener, final int numSteps) {
        this(launchDoneListener, numSteps, new DefaultSchedulerProvider());
    }

    public DefaultLaunchViewModel(final Runnable launchDoneListener,
                                  final int numSteps,
                                  final SchedulerProvider schedulerProvider) {
        this.launchDoneListener = launchDoneListener;
        maxStep = numSteps - 1;
        stepSubject = BehaviorSubject.createDefault(0);
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public void finishLaunchStep(final int stepNumber) {
        final int currentStep = stepSubject.getValue();
        if (currentStep != stepNumber) {
            Timber.e("Cannot finish step %d because the current step is %d", stepNumber, currentStep);
        } else if (currentStep == maxStep) {
            launchDoneListener.run();
        } else {
            stepSubject.onNext(currentStep + 1);
        }
    }

    @Override
    public Observable<Integer> getLaunchStepToDisplay() {
        return stepSubject;
    }
}
