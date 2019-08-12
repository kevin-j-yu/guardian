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
package ai.rideos.android.common.interactors;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.grpc.Stubs;
import ai.rideos.android.common.reactive.SchedulerProvider;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * GrpcServerInteractor handles several common functions for interactors that require calling to a gRPC server
 * including authenticating calls and transforming futures to observables.
 * @param <T> Stub type
 */
public abstract class GrpcServerInteractor<T extends AbstractStub<T>> {
    // Supplies a new stub given a channel
    public interface StubSupplier<T> {
        T getStub(final ManagedChannel channel);
    }

    // Calls a method on a stub that returns a Future
    public interface FutureStubMethod<T, R> {
        Future<R> call(final T stub);
    }

    private final StubSupplier<T> stubSupplier;
    private final ManagedChannel channel;
    private final User user;
    private final SchedulerProvider schedulerProvider;

    protected GrpcServerInteractor(final StubSupplier<T> stubSupplier,
                                   final Supplier<ManagedChannel> channelSupplier,
                                   final User user,
                                   final SchedulerProvider schedulerProvider) {
        this.stubSupplier = stubSupplier;
        this.channel = channelSupplier.get();
        this.user = user;
        this.schedulerProvider = schedulerProvider;
    }

    public void shutDown() {
        channel.shutdownNow();
    }

    protected SchedulerProvider getSchedulerProvider() {
        return schedulerProvider;
    }

    protected Single<T> fetchAuthorizedStub() {
        return user.fetchUserToken()
            .map(userToken -> Stubs.withAuthorization(
                stubSupplier.getStub(channel),
                userToken
            ))
            // Stub will be fetched on io thread
            .subscribeOn(schedulerProvider.io());
    }

    protected <R> Observable<R> fetchAuthorizedStubAndExecute(final FutureStubMethod<T, R> grpcMethod) {
        return fetchAuthorizedStub()
            // grpc method will be run on io thread because it is blocking
            .flatMapObservable(stub -> Observable.fromFuture(grpcMethod.call(stub), schedulerProvider.io()));
    }
}
