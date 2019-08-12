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

import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SocketConnectivityInteractor implements ConnectivityInteractor {
    private static final Settings DEFAULT_SETTINGS = new Settings(2000, 1000, 3);

    public static class Settings {
        private final int checkIntervalMs;
        private final int connectivityTimeoutMs;
        private final int retryCount;

        public Settings(final int checkIntervalMs, final int connectivityTimeoutMs, final int retryCount) {
            this.checkIntervalMs = checkIntervalMs;
            this.connectivityTimeoutMs = connectivityTimeoutMs;
            this.retryCount = retryCount;
        }
    }

    private final SocketAddress socketAddress;
    private final Settings settings;
    private final SchedulerProvider schedulerProvider;
    private final Supplier<Socket> socketSupplier;

    public SocketConnectivityInteractor() {
        // By default check Google's public DNS
        this(new InetSocketAddress("8.8.8.8", 53), DEFAULT_SETTINGS, new DefaultSchedulerProvider(), Socket::new);
    }

    public SocketConnectivityInteractor(final SocketAddress socketAddress,
                                        final Settings settings,
                                        final SchedulerProvider schedulerProvider,
                                        final Supplier<Socket> socketSupplier) {
        this.socketAddress = socketAddress;
        this.schedulerProvider = schedulerProvider;
        this.settings = settings;
        this.socketSupplier = socketSupplier;
    }

    @Override
    public Observable<Status> observeNetworkStatus() {
        return Flowable.interval(settings.checkIntervalMs, TimeUnit.MILLISECONDS, schedulerProvider.io())
            .startWith(0L)
            .onBackpressureDrop()
            .flatMap(i -> testConnection().toFlowable())
            .toObservable();
    }

    private Single<Status> testConnection() {
        return Single.<Status>create(emitter -> {
            final Socket socket = socketSupplier.get();
            socket.connect(socketAddress, settings.connectivityTimeoutMs);
            socket.close();
            emitter.onSuccess(Status.CONNECTED);
        })
            .subscribeOn(schedulerProvider.io())
            .retry(settings.retryCount)
            .onErrorReturnItem(Status.DISCONNECTED);
    }
}
