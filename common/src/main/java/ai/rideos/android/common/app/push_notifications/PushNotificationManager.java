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
package ai.rideos.android.common.app.push_notifications;

import ai.rideos.android.common.app.dependency.CommonDependencyRegistry;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.DeviceRegistryInteractor;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.google.firebase.iid.FirebaseInstanceId;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * The PushNotificationManager handles initializing a user's device token with a backend device registry using
 * Firebase messaging. Note that a device token can be updated in the background if Firebase needs to invalidate
 * previous tokens. In this case, a service extending FirebaseMessagingService should listen for new tokens and
 * sync them with the backend accordingly.
 */
public class PushNotificationManager {
    private static final int DEFAULT_RETRY_COUNT = 3;

    private final User user;
    private final DeviceRegistryInteractor deviceRegistryInteractor;
    private final RegistrationMethod registrationMethod;
    private final Supplier<FirebaseInstanceId> firebaseInstanceSupplier;
    private final SchedulerProvider schedulerProvider;
    private final int retryCount;

    @VisibleForTesting
    interface RegistrationMethod {
        Completable register(final DeviceRegistryInteractor deviceRegistryInteractor,
                             final String userId,
                             final String deviceToken);
    }

    private PushNotificationManager(final User user,
                                    final DeviceRegistryInteractor deviceRegistryInteractor,
                                    final RegistrationMethod registrationMethod) {
        this(
            user,
            deviceRegistryInteractor,
            registrationMethod,
            FirebaseInstanceId::getInstance,
            new DefaultSchedulerProvider(),
            DEFAULT_RETRY_COUNT
        );
    }

    @VisibleForTesting
    PushNotificationManager(final User user,
                            final DeviceRegistryInteractor deviceRegistryInteractor,
                            final RegistrationMethod registrationMethod,
                            final Supplier<FirebaseInstanceId> firebaseInstanceSupplier,
                            final SchedulerProvider schedulerProvider,
                            final int retryCount) {
        this.user = user;
        this.deviceRegistryInteractor = deviceRegistryInteractor;
        this.registrationMethod = registrationMethod;
        this.firebaseInstanceSupplier = firebaseInstanceSupplier;
        this.schedulerProvider = schedulerProvider;
        this.retryCount = retryCount;
    }

    public static PushNotificationManager forRider(final Context context) {
        final User user = User.get(context);
        return new PushNotificationManager(
            user,
            CommonDependencyRegistry.commonDependencyFactory().getDeviceRegistryInteractor(context),
            DeviceRegistryInteractor::registerRiderDevice
        );
    }

    public static PushNotificationManager forDriver(final Context context) {
        final User user = User.get(context);
        return new PushNotificationManager(
            user,
            CommonDependencyRegistry.commonDependencyFactory().getDeviceRegistryInteractor(context),
            DeviceRegistryInteractor::registerDriverDevice
        );
    }

    public Completable requestTokenAndSync() {
        final String userId = user.getId();

        if (userId.isEmpty()) {
            return Completable.error(new IllegalArgumentException("User is not logged in"));
        }

        return Single.<String>create(
            emitter -> firebaseInstanceSupplier.get().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        emitter.onSuccess(task.getResult().getToken());
                    } else {
                        emitter.onError(new IOException("Failed to get current device token"));
                    }
                })
                .addOnFailureListener(emitter::onError)
        )
            .subscribeOn(schedulerProvider.io())
            .flatMapCompletable(token -> registrationMethod.register(deviceRegistryInteractor, userId, token))
            .retry(retryCount);
    }
}
