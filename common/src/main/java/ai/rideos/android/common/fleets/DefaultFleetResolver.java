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
package ai.rideos.android.common.fleets;

import ai.rideos.android.common.device.DeviceLocator;
import ai.rideos.android.common.interactors.FleetInteractor;
import ai.rideos.android.common.location.DistanceCalculator;
import ai.rideos.android.common.location.HaversineDistanceCalculator;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import androidx.core.util.Pair;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import timber.log.Timber;

public class DefaultFleetResolver implements FleetResolver {
    private static final int RETRY_COUNT = 3;

    private final FleetInteractor fleetInteractor;
    private final DeviceLocator deviceLocator;
    private final DistanceCalculator distanceCalculator;
    private final SchedulerProvider schedulerProvider;

    public DefaultFleetResolver(final FleetInteractor fleetInteractor,
                                final DeviceLocator deviceLocator) {
        this(fleetInteractor, deviceLocator, new HaversineDistanceCalculator(), new DefaultSchedulerProvider());
    }

    public DefaultFleetResolver(final FleetInteractor fleetInteractor,
                                final DeviceLocator deviceLocator,
                                final DistanceCalculator distanceCalculator,
                                final SchedulerProvider schedulerProvider) {
        this.fleetInteractor = fleetInteractor;
        this.deviceLocator = deviceLocator;
        this.distanceCalculator = distanceCalculator;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<FleetInfo> resolveFleet(final Observable<String> storedFleetId) {
        return storedFleetId.observeOn(schedulerProvider.computation())
            .flatMap(fleetId -> {
                if (fleetId.equals(AUTOMATIC_FLEET_ID)) {
                    return resolveAutomatic();
                } else {
                    return getAvailableFleets()
                        .flatMap(fleets -> {
                            final OptionalInt fleetIndex = IntStream.range(0, fleets.size())
                                .filter(i -> fleets.get(i).getId().equals(fleetId))
                                .findFirst();
                            if (fleetIndex.isPresent()) {
                                return Observable.just(fleets.get(fleetIndex.getAsInt()));
                            }
                            return resolveAutomatic();
                        });
                }
            });
    }

    @Override
    public void shutDown() {
        fleetInteractor.destroy();
    }

    private Observable<List<FleetInfo>> getAvailableFleets() {
        return fleetInteractor.getFleets()
            .observeOn(schedulerProvider.computation())
            .retry(RETRY_COUNT)
            .doOnError(e -> Timber.e(e, "Failed to resolve fleets"))
            .onErrorReturnItem(Collections.singletonList(FleetInfo.DEFAULT_FLEET));
    }

    private Observable<FleetInfo> resolveAutomatic() {
        return Observable.combineLatest(
            getAvailableFleets(),
            deviceLocator.getLastKnownLocation().toObservable(),
            Pair::create
        )
            .map(fleetsAndLocation -> {
                final List<FleetInfo> fleets = fleetsAndLocation.first;
                final LatLng location = fleetsAndLocation.second.getLatLng();
                return findClosestFleet(fleets, location).orElse(FleetInfo.DEFAULT_FLEET);
            });
    }

    private Optional<FleetInfo> findClosestFleet(final List<FleetInfo> fleets, final LatLng currentLocation) {
        final List<FleetInfo> locatableFleets = fleets.stream()
            .filter(fleetInfo -> fleetInfo.getCenter().isPresent())
            .collect(Collectors.toList());

        final Comparator<FleetInfo> comparator = (fleet0, fleet1) -> Double.compare(
            distanceCalculator.getDistanceInMeters(currentLocation, fleet0.getCenter().get()),
            distanceCalculator.getDistanceInMeters(currentLocation, fleet1.getCenter().get())
        );

        // Try to find closest non-phantom fleets first
        final List<FleetInfo> nonPhantomFleets = locatableFleets.stream()
            .filter(fleet -> !fleet.isPhantom())
            .sorted(comparator)
            .collect(Collectors.toList());

        if (nonPhantomFleets.size() > 0) {
            return Optional.of(nonPhantomFleets.get(0));
        }

        // If all fleets are phantom fleets, return the closest one.
        final List<FleetInfo> phantomFleets = locatableFleets.stream()
            .filter(FleetInfo::isPhantom)
            .sorted(comparator)
            .collect(Collectors.toList());

        if (phantomFleets.size() > 0) {
            return Optional.of(phantomFleets.get(0));
        }
        return Optional.empty();
    }
}
