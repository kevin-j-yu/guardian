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
import ai.rideos.android.common.model.VehiclePosition;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.utils.Locations;
import ai.rideos.api.ride_hail_rider.v1.RideHailRider.GetVehiclesInVicinityRequest;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc;
import ai.rideos.api.ride_hail_rider.v1.RideHailRiderServiceGrpc.RideHailRiderServiceFutureStub;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultPreviewVehicleInteractor
    extends GrpcServerInteractor<RideHailRiderServiceFutureStub>
    implements PreviewVehicleInteractor {

    public DefaultPreviewVehicleInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        super(RideHailRiderServiceGrpc::newFutureStub, channelSupplier, user, new DefaultSchedulerProvider());
    }

    @Override
    public Observable<List<VehiclePosition>> getVehiclesInVicinity(final LatLng center, final String fleetId) {
        return fetchAuthorizedStubAndExecute(dispatchStub -> dispatchStub.getVehiclesInVicinity(
            GetVehiclesInVicinityRequest.newBuilder()
                .setQueryPosition(Locations.toRideOsPosition(center))
                .setFleetId(fleetId)
                .build()
        ))
            .map(response -> response.getVehicleList().stream()
                .map(vehiclePosition -> new VehiclePosition(
                    vehiclePosition.getId(),
                    Locations.fromRideOsPosition(vehiclePosition.getPosition()),
                    vehiclePosition.getHeading().getValue()
                ))
                .collect(Collectors.toList())
            );
    }
}
