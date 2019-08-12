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
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperations.GetFleetsRequest;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperations.GetFleetsResponse;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperationsServiceGrpc;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperationsServiceGrpc.RideHailOperationsServiceFutureStub;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultFleetInteractor
    extends GrpcServerInteractor<RideHailOperationsServiceFutureStub>
    implements FleetInteractor {

    public DefaultFleetInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        super(RideHailOperationsServiceGrpc::newFutureStub, channelSupplier, user, new DefaultSchedulerProvider());
    }

    @Override
    public Observable<List<FleetInfo>> getFleets() {
        return fetchAuthorizedStubAndExecute(stub -> stub.getFleets(GetFleetsRequest.getDefaultInstance()))
            .map(GetFleetsResponse::getFleetList)
            .map(fleetList -> fleetList.stream()
                .map(fleet -> new FleetInfo(fleet.getId()))
                .collect(Collectors.toList())
            );
    }

    public void destroy() {
        shutDown();
    }
}
