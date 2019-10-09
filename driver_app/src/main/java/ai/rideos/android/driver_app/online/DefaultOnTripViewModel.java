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
package ai.rideos.android.driver_app.online;

import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.model.TripResourceInfo;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import androidx.annotation.StringRes;
import io.reactivex.Observable;
import timber.log.Timber;

public class DefaultOnTripViewModel implements OnTripViewModel {
    private static final int RETRY_COUNT = 2;
    private final SchedulerProvider schedulerProvider;

    private final GeocodeInteractor geocodeInteractor;
    private final Waypoint nextWaypoint;
    private final ResourceProvider resourceProvider;
    @StringRes
    private final int passengerDetailTemplate;

    protected DefaultOnTripViewModel(final GeocodeInteractor geocodeInteractor,
                                     final Waypoint nextWaypoint,
                                     final ResourceProvider resourceProvider,
                                     @StringRes final int passengerDetailTemplate,
                                     final SchedulerProvider schedulerProvider) {
        this.geocodeInteractor = geocodeInteractor;
        this.nextWaypoint = nextWaypoint;
        this.resourceProvider = resourceProvider;
        this.passengerDetailTemplate = passengerDetailTemplate;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public String getPassengerDetailText() {
        final String passengersToPickupText;
        final TripResourceInfo tripResourceInfo = nextWaypoint.getAction().getTripResourceInfo();
        if (tripResourceInfo.getNumPassengers() > 1) {
            final int numberOfRidersExcludingRequester = tripResourceInfo.getNumPassengers() - 1;
            passengersToPickupText = String.format(
                "%s + %s",
                tripResourceInfo.getNameOfTripRequester(),
                numberOfRidersExcludingRequester
            );
        } else {
            passengersToPickupText = tripResourceInfo.getNameOfTripRequester();
        }

        return resourceProvider.getString(passengerDetailTemplate, passengersToPickupText);
    }

    @Override
    public Observable<String> getDestinationAddress() {
        return geocodeInteractor.getBestReverseGeocodeResult(nextWaypoint.getAction().getDestination())
            .observeOn(schedulerProvider.computation())
            .retry(RETRY_COUNT)
            .doOnError(error -> Timber.e(error, "Failed to geocode location"))
            .onErrorReturn(Result::failure)
            .map(result -> {
                if (result.isFailure()) {
                    return "";
                }
                return result.get().getDisplayName();
            });
    }
}
