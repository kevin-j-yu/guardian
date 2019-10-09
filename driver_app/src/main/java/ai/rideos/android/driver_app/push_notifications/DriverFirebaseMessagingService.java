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
package ai.rideos.android.driver_app.push_notifications;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import com.google.firebase.messaging.FirebaseMessagingService;
import timber.log.Timber;

public class DriverFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        final User user = User.get(getApplicationContext());
        final String userId = user.getId();

        if (userId.isEmpty()) {
            // User is logged out. Wait for the app to start up and log in
            return;
        }

        DriverDependencyRegistry.driverDependencyFactory().getDeviceRegistryInteractor(this)
            .registerDriverDevice(userId, token)
            .blockingAwait();
        Timber.i("Successfully updated device for user %s and device %s", userId, token);
    }
}
