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
package ai.rideos.android.interactors;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.GrpcServerInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.android.model.Stop;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.FindPredefinedStopRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.StopSearchParameters;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc.RideHailRiderServiceFutureStub;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import java.util.function.Supplier;

public class DefaultStopInteractor
    extends GrpcServerInteractor<RideHailRiderServiceFutureStub>
    implements StopInteractor {

    public DefaultStopInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        super(RideHailRiderServiceGrpc::newFutureStub, channelSupplier, user, new DefaultSchedulerProvider());
    }

    @Override
    public Observable<Stop> getBestStop(final String fleetId, final LatLng location) {
        return fetchAuthorizedStubAndExecute(stub -> stub.findPredefinedStop(
            FindPredefinedStopRequest.newBuilder()
                .setFleetId(fleetId)
                .setSearchParameters(
                    StopSearchParameters.newBuilder().setQueryPosition(Locations.toRideOsPosition(location))
                )
                .build()
        ))
            .map(response -> new Stop(
                Locations.fromRideOsPosition(response.getPredefinedStop(0).getPosition()),
                response.getPredefinedStop(0).getId()
            ));
    }
}
