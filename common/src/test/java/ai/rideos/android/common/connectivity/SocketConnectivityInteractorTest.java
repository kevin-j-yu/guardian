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
package ai.rideos.android.common.connectivity;

import ai.rideos.android.common.connectivity.ConnectivityInteractor.Status;
import ai.rideos.android.common.connectivity.SocketConnectivityInteractor.Settings;
import ai.rideos.android.common.reactive.SchedulerProviders.TestSchedulerProvider;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SocketConnectivityInteractorTest {
    private static final SocketAddress ADDR = new InetSocketAddress("8.8.8.8", 53);
    private static final int INTERVAL_MS = 1000;

    private TestScheduler testScheduler;
    private AtomicReference<Socket> socketReference;
    private SocketConnectivityInteractor interactorUnderTest;

    @Before
    public void setUp() {
        testScheduler = new TestScheduler();
        socketReference = new AtomicReference<>();
        interactorUnderTest = new SocketConnectivityInteractor(
            ADDR,
            new Settings(INTERVAL_MS, 0, 0),
            new TestSchedulerProvider(testScheduler),
            () -> socketReference.get()
        );
    }

    @Test
    public void testCanGetConnectionWhenSocketInitiallyConnected() {
        final Socket mockSocket = Mockito.mock(Socket.class);
        socketReference.set(mockSocket);

        final TestObserver<Status> testObserver = interactorUnderTest.observeNetworkStatus().test();
        testScheduler.triggerActions();
        testObserver.assertValueAt(0, Status.CONNECTED);
    }

    @Test
    public void testCanGetConnectionWhenSocketInitiallyDisconnected() throws IOException {
        final Socket mockSocket = Mockito.mock(Socket.class);
        Mockito.doAnswer(invocation -> {
            throw new IOException();
        })
            .when(mockSocket).close();
        socketReference.set(mockSocket);

        final TestObserver<Status> testObserver = interactorUnderTest.observeNetworkStatus().test();
        testScheduler.triggerActions();
        testObserver.assertValueAt(0, Status.DISCONNECTED);
    }

    @Test
    public void testConnectionUpdatesOnInterval() throws IOException {
        final Socket mockSocket1 = Mockito.mock(Socket.class);
        Mockito.doAnswer(invocation -> {
            throw new IOException();
        })
            .when(mockSocket1).close();
        socketReference.set(mockSocket1);

        final Socket mockSocket2 = Mockito.mock(Socket.class);

        final TestObserver<Status> testObserver = interactorUnderTest.observeNetworkStatus().test();
        testScheduler.triggerActions();
        testObserver.assertValueAt(0, Status.DISCONNECTED);

        socketReference.set(mockSocket2);
        testScheduler.advanceTimeBy(INTERVAL_MS, TimeUnit.MILLISECONDS);
        testObserver.assertValueAt(1, Status.CONNECTED);
    }
}
