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
package ai.rideos.android.common.app.launch.permissions;

import ai.rideos.android.common.app.launch.LaunchStep.LaunchStepListener;
import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.Manifest.permission;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.Collections;
import java.util.List;

public class DefaultPermissionsViewModel implements PermissionsViewModel {
    private final BehaviorSubject<Notification> permissionsRequired
        = BehaviorSubject.createDefault(Notification.create());
    private final SchedulerProvider schedulerProvider;
    private final LaunchStepListener launchStepListener;

    public DefaultPermissionsViewModel(final LaunchStepListener launchStepListener) {
        this(launchStepListener, new DefaultSchedulerProvider());
    }

    public DefaultPermissionsViewModel(final LaunchStepListener launchStepListener,
                                       final SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
        this.launchStepListener = launchStepListener;
    }

    @Override
    public void permissionsEnabled(final boolean areEnabled) {
        if (areEnabled) {
            launchStepListener.done();
        } else {
            permissionsRequired.onNext(Notification.create());
        }
    }

    @Override
    public Observable<List<String>> getPermissionsToCheck() {
        return permissionsRequired
            .map(notification -> Collections.singletonList(permission.ACCESS_FINE_LOCATION));
    }
}
