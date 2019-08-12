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
package ai.rideos.android.common.app.launch.login;

import ai.rideos.android.common.app.launch.LaunchStep.LaunchStepListener;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.Notification;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import com.auth0.android.result.Credentials;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class DefaultLoginViewModel implements LoginViewModel {
    private final LaunchStepListener launchStepListener;
    private final SchedulerProvider schedulerProvider;
    private final User user;
    private final BehaviorSubject<Notification> loginSubject = BehaviorSubject.createDefault(Notification.create());

    public DefaultLoginViewModel(final User user, final LaunchStepListener launchStepListener) {
        this(
            user,
            launchStepListener,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultLoginViewModel(final User user,
                                 final LaunchStepListener launchStepListener,
                                 final SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
        this.user = user;
        this.launchStepListener = launchStepListener;
    }

    @Override
    public void loginComplete(final Credentials credentials) {
        user.updateCredentials(credentials);
        launchStepListener.done();
    }

    @Override
    public void loginFailure(final Exception exception) {
        Timber.e(exception, "Failed to login");
        loginSubject.onNext(Notification.create());
    }

    @Override
    public Observable<Notification> shouldLaunchLogin() {
        if (user.isLoggedIn()) {
            launchStepListener.done();
            return Observable.empty();
        }
        return loginSubject;
    }

}
