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
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.api.ride_hail_notification.v1.Device.SetDriverDeviceInfoRequest;
import ai.rideos.api.ride_hail_notification.v1.Device.SetRiderDeviceInfoRequest;
import ai.rideos.api.ride_hail_notification.v1.DeviceRegistryServiceGrpc;
import ai.rideos.api.ride_hail_notification.v1.DeviceRegistryServiceGrpc.DeviceRegistryServiceFutureStub;
import ai.rideos.api.ride_hail_notification.v1.NotificationCommon.DeviceInfo;
import ai.rideos.api.ride_hail_notification.v1.NotificationCommon.DeviceInfo.AndroidDevice;
import io.grpc.ManagedChannel;
import io.reactivex.Completable;
import java.util.function.Supplier;

public class DefaultDeviceRegistryInteractor
    extends GrpcServerInteractor<DeviceRegistryServiceFutureStub>
    implements DeviceRegistryInteractor {

    public DefaultDeviceRegistryInteractor(final Supplier<ManagedChannel> channelSupplier, final User user) {
        super(DeviceRegistryServiceGrpc::newFutureStub, channelSupplier, user, new DefaultSchedulerProvider());
    }

    public Completable registerRiderDevice(final String riderId, final String token) {
        return fetchAuthorizedStubAndExecute(stub -> stub.setRiderDeviceInfo(
            SetRiderDeviceInfoRequest.newBuilder()
                .setDeviceInfo(
                    DeviceInfo.newBuilder()
                        .setAndroidDevice(AndroidDevice.newBuilder().setToken(token))
                )
                .setRiderId(riderId)
                .build()
        ))
            .ignoreElements();
    }

    public Completable registerDriverDevice(final String vehicleId, final String token) {
        return fetchAuthorizedStubAndExecute(stub -> stub.setDriverDeviceInfo(
            SetDriverDeviceInfoRequest.newBuilder()
                .setDeviceInfo(
                    DeviceInfo.newBuilder()
                        .setAndroidDevice(AndroidDevice.newBuilder().setToken(token))
                )
                .setVehicleId(vehicleId)
                .build()
        ))
            .ignoreElements();
    }
}
