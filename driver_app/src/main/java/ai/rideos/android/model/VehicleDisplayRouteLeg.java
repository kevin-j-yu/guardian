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

import ai.rideos.android.common.model.RouteInfoModel;
import androidx.core.util.Pair;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class VehicleDisplayRouteLeg {
    @Nullable
    private final Pair<String, String> previousTripAndStep;
    private final Pair<String, String> routableTripAndStep;
    private final RouteInfoModel routeInfoModel;

    public VehicleDisplayRouteLeg(@Nullable final Pair<String, String> previousTripAndStep,
                                  final Pair<String, String> routableTripAndStep,
                                  final RouteInfoModel routeInfoModel) {
        this.previousTripAndStep = previousTripAndStep;
        this.routableTripAndStep = routableTripAndStep;
        this.routeInfoModel = routeInfoModel;
    }

    public Optional<Pair<String, String>> getPreviousTripAndStep() {
        return Optional.ofNullable(previousTripAndStep);
    }

    public Pair<String, String> getRoutableTripAndStep() {
        return routableTripAndStep;
    }

    public RouteInfoModel getRoute() {
        return routeInfoModel;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof VehicleDisplayRouteLeg)) {
            return false;
        }
        final VehicleDisplayRouteLeg otherModel = (VehicleDisplayRouteLeg) other;
        return Objects.equals(previousTripAndStep, otherModel.previousTripAndStep)
            && routableTripAndStep.equals(otherModel.routableTripAndStep)
            && routeInfoModel.equals(otherModel.routeInfoModel);
    }
}
