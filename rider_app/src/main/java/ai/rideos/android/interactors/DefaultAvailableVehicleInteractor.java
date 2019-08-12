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
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.model.AvailableVehicle;
import ai.rideos.api.commons.ride_hail_commons.RideHailCommons.Vehicle;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperations.GetVehiclesRequest;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperationsServiceGrpc;
import ai.rideos.api.ride_hail_operations.v1.RideHailOperationsServiceGrpc.RideHailOperationsServiceFutureStub;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultAvailableVehicleInteractor
    extends GrpcServerInteractor<RideHailOperationsServiceFutureStub>
    implements AvailableVehicleInteractor {

    public DefaultAvailableVehicleInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        super(RideHailOperationsServiceGrpc::newFutureStub, channelSupplier, user, new DefaultSchedulerProvider());
    }

    @Override
    public Observable<List<AvailableVehicle>> getAvailableVehicles(final String fleetId) {
        return fetchAuthorizedStubAndExecute(operationsStub -> operationsStub.getVehicles(
            GetVehiclesRequest.newBuilder()
                .setFleetId(fleetId)
                .build()
        ))
            .map(vehicles -> vehicles.getVehicleList().stream()
                .filter(vehicle -> vehicle.getState().getReadiness())
                .map(vehicle -> new AvailableVehicle(vehicle.getId(), getDisplayNameForVehicle(vehicle)))
                .collect(Collectors.toList())
            );
    }

    private static String getDisplayNameForVehicle(final Vehicle vehicle) {
        if (vehicle.getInfo().getLicensePlate().isEmpty()) {
            return vehicle.getId();
        }
        return vehicle.getInfo().getLicensePlate();
    }
}
