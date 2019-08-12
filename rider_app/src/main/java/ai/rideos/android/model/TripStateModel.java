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
package ai.rideos.android.model;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.LocationAndHeading;
import ai.rideos.android.common.model.RouteInfoModel;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TripStateModel {
    public enum Stage {
        WAITING_FOR_ASSIGNMENT,
        DRIVING_TO_PICKUP,
        WAITING_FOR_PICKUP,
        DRIVING_TO_DROP_OFF,
        COMPLETED,
        CANCELLED,
        UNKNOWN
    }

    public static class CancellationReason {
        public enum Source {
            DRIVER,
            RIDER,
            INTERNAL
        }

        private final Source source;
        private final String description;

        public CancellationReason(final Source source, final String description) {
            this.source = source;
            this.description = description;
        }

        public Source getSource() {
            return source;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof CancellationReason)) {
                return false;
            }
            final CancellationReason otherModel = (CancellationReason) other;
            return source == otherModel.source && Objects.equals(description, otherModel.description);
        }
    }

    private final Stage stage;
    @Nullable
    private final RouteInfoModel vehicleRoute;
    @Nullable
    private final VehicleInfo vehicleInfo;
    @Nullable
    private final LocationAndHeading vehiclePosition;
    private final LatLng passengerPickupLocation;
    private final LatLng passengerDropOffLocation;
    private final List<LatLng> waypoints;
    @Nullable
    private final CancellationReason cancellationReason;

    public TripStateModel(final Stage stage,
                          @Nullable final RouteInfoModel vehicleRoute,
                          @Nullable final VehicleInfo vehicleInfo,
                          @Nullable final LocationAndHeading vehiclePosition,
                          final LatLng passengerPickupLocation,
                          final LatLng passengerDropOffLocation,
                          final List<LatLng> waypoints,
                          @Nullable final CancellationReason cancellationReason) {
        this.stage = stage;
        this.vehicleRoute = vehicleRoute;
        this.vehicleInfo = vehicleInfo;
        this.vehiclePosition = vehiclePosition;
        this.passengerPickupLocation = passengerPickupLocation;
        this.passengerDropOffLocation = passengerDropOffLocation;
        this.waypoints = waypoints;
        this.cancellationReason = cancellationReason;
    }

    public Stage getStage() {
        return stage;
    }

    // Vehicle route is optional, for example, in the case that the task is cancelled
    public Optional<RouteInfoModel> getVehicleRouteInfo() {
        return Optional.ofNullable(vehicleRoute);
    }

    public LatLng getPassengerPickupLocation() {
        return passengerPickupLocation;
    }

    public LatLng getPassengerDropOffLocation() {
        return passengerDropOffLocation;
    }

    public Optional<VehicleInfo> getVehicleInfo() {
        return Optional.ofNullable(vehicleInfo);
    }

    public Optional<LocationAndHeading> getVehiclePosition() {
        return Optional.ofNullable(vehiclePosition);
    }

    public List<LatLng> getWaypoints() {
        return waypoints;
    }

    public Optional<CancellationReason> getCancellationReason() {
        return Optional.ofNullable(cancellationReason);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TripStateModel)) {
            return false;
        }
        final TripStateModel otherModel = (TripStateModel) other;
        return stage == otherModel.stage
            && Objects.equals(vehicleRoute, otherModel.vehicleRoute)
            && Objects.equals(vehicleInfo, otherModel.vehicleInfo)
            && Objects.equals(vehiclePosition, otherModel.vehiclePosition)
            && passengerPickupLocation.equals(otherModel.passengerPickupLocation)
            && passengerDropOffLocation.equals(otherModel.passengerDropOffLocation)
            && waypoints.equals(otherModel.waypoints)
            && Objects.equals(cancellationReason, otherModel.cancellationReason);
    }
}
