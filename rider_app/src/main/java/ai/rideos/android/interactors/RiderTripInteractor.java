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

import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.model.ContactInfo;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.Optional;

public interface RiderTripInteractor {
    Observable<String> createTripForPassenger(final String passengerId,
                                              final ContactInfo contactInfo,
                                              final String fleetId,
                                              final int numPassengers,
                                              final TaskLocation pickupLocation,
                                              final TaskLocation dropOffLocation);

    Observable<String> createTripForPassengerAndVehicle(final String passengerId,
                                                        final ContactInfo contactInfo,
                                                        final String vehicleId,
                                                        final String fleetId,
                                                        final int numPassengers,
                                                        final TaskLocation pickupLocation,
                                                        final TaskLocation dropOffLocation);

    /**
     * Get the current trip given a passenger, if it exists.
     * @param passengerId - passenger to find trip for
     * @return Optional task id. This value is empty if a trip doesn't exist (or is over) and the trip id otherwise
     */
    Observable<Optional<String>> getCurrentTripForPassenger(final String passengerId);

    Completable cancelTrip(final String passengerId, final String tripId);

    /**
     * Modify the pickup location for a trip and emit the new task id or an error if it could not be updated
     * @param tripId - current trip id
     * @param newPickupLocation - updated pickup location
     * @return new task id if successful
     */
    Observable<String> editPickup(final String tripId, final TaskLocation newPickupLocation);

    void shutDown();
}
