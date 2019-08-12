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

import ai.rideos.android.common.reactive.Notification;
import com.auth0.android.result.Credentials;
import io.reactivex.Observable;

public interface LoginViewModel {
    void loginComplete(final Credentials credentials);

    void loginFailure(final Exception exception);

    /**
     * Emit a notification when the login experience should be launched. If the user is already logged in, this should
     * not emit anything.
     */
    Observable<Notification> shouldLaunchLogin();
}
