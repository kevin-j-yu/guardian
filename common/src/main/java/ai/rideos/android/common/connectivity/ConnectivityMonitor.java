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
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.view.View;
import io.reactivex.disposables.CompositeDisposable;

public class ConnectivityMonitor {
    private final ConnectivityInteractor connectivityInteractor;
    private final View banner;
    private final SchedulerProvider schedulerProvider;
    private CompositeDisposable compositeDisposable;

    public ConnectivityMonitor(final View banner) {
        this(
            new SocketConnectivityInteractor(),
            banner,
            new DefaultSchedulerProvider()
        );
    }

    public ConnectivityMonitor(final ConnectivityInteractor connectivityInteractor,
                               final View banner,
                               final SchedulerProvider schedulerProvider) {
        this.connectivityInteractor = connectivityInteractor;
        this.banner = banner;
        this.schedulerProvider = schedulerProvider;
    }

    public void start() {
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(
            connectivityInteractor.observeNetworkStatus()
                .observeOn(schedulerProvider.mainThread())
                .subscribe(status -> {
                    if (status == Status.CONNECTED) {
                        banner.setVisibility(View.GONE);
                    } else {
                        banner.setVisibility(View.VISIBLE);
                    }
                })
        );
    }

    public void stop() {
        compositeDisposable.dispose();
    }
}
