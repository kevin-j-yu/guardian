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

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.DeviceRegistryInteractor;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class PushNotificationManagerTest {
    private PushNotificationManager pushNotificationManager;
    private User user;
    private DeviceRegistryInteractor deviceRegistryInteractor;
    private FirebaseInstanceId firebaseInstance;

    @Before
    public void setUp() {
        user = Mockito.mock(User.class);
        deviceRegistryInteractor = Mockito.mock(DeviceRegistryInteractor.class);
        firebaseInstance = Mockito.mock(FirebaseInstanceId.class);
        pushNotificationManager = new PushNotificationManager(
            user,
            deviceRegistryInteractor,
            DeviceRegistryInteractor::registerRiderDevice,
            () -> firebaseInstance,
            new TrampolineSchedulerProvider(),
            0
        );
    }

    @Test
    public void testDeviceIsNotSyncedIfUserIsNotLoggedIn() {
        Mockito.when(user.getId()).thenReturn("");

        pushNotificationManager.requestTokenAndSync().test()
            .assertError(IllegalArgumentException.class);

        Mockito.verifyNoMoreInteractions(firebaseInstance, deviceRegistryInteractor);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeviceIsNotSyncedIfFirebaseCannotReturnDeviceToken() {
        Mockito.when(user.getId()).thenReturn("user-1");
        final ArgumentCaptor<OnCompleteListener> onCompleteCaptor = ArgumentCaptor.forClass(OnCompleteListener.class);
        final Task<InstanceIdResult> mockTask = Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnCompleteListener(onCompleteCaptor.capture())).thenReturn(mockTask);
        Mockito.when(mockTask.isSuccessful()).thenReturn(false);

        Mockito.when(firebaseInstance.getInstanceId()).thenReturn(mockTask);

        TestObserver<Void> testObserver = pushNotificationManager.requestTokenAndSync().test();
        onCompleteCaptor.getValue().onComplete(mockTask);

        testObserver.assertError(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeviceIsSyncedWhenFirebaseReturnsToken() {
        Mockito.when(user.getId()).thenReturn("user-1");
        final ArgumentCaptor<OnCompleteListener> onCompleteCaptor = ArgumentCaptor.forClass(OnCompleteListener.class);
        final Task<InstanceIdResult> mockTask = Mockito.mock(Task.class);

        Mockito.when(mockTask.addOnCompleteListener(onCompleteCaptor.capture())).thenReturn(mockTask);
        Mockito.when(mockTask.isSuccessful()).thenReturn(true);

        final InstanceIdResult mockResult = Mockito.mock(InstanceIdResult.class);
        Mockito.when(mockTask.getResult()).thenReturn(mockResult);
        Mockito.when(mockResult.getToken()).thenReturn("token");

        Mockito.when(firebaseInstance.getInstanceId()).thenReturn(mockTask);

        Mockito.when(deviceRegistryInteractor.registerRiderDevice(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Completable.complete());

        TestObserver<Void> testObserver = pushNotificationManager.requestTokenAndSync().test();
        onCompleteCaptor.getValue().onComplete(mockTask);

        testObserver.assertNoErrors().assertComplete();
        Mockito.verify(deviceRegistryInteractor).registerRiderDevice("user-1", "token");
    }
}
