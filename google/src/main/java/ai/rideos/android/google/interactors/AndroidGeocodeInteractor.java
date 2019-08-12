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
package ai.rideos.android.google.interactors;

import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import io.reactivex.Observable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AndroidGeocodeInteractor uses the default Android Geocoder object to perform reverse geocodings. It is important to
 * note that this library cannot be used for location autocompletion because the forward geocoding is only intended
 * for singular results.
 */
public class AndroidGeocodeInteractor implements GeocodeInteractor {
    private static final int MAX_RESULTS_TO_SCAN = 10;
    private final Geocoder geocoder;
    private final SchedulerProvider schedulerProvider;

    public AndroidGeocodeInteractor(final Context context) {
        this(new Geocoder(context), new DefaultSchedulerProvider());
    }

    public AndroidGeocodeInteractor(final Geocoder geocoder, final SchedulerProvider schedulerProvider) {
        this.geocoder = geocoder;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<List<NamedTaskLocation>> getReverseGeocodeResults(final LatLng latLng, final int maxResults) {
        return Observable.fromCallable(() -> geocoder.getFromLocation(
            latLng.getLatitude(),
            latLng.getLongitude(),
            maxResults
        ))
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.computation())
            .map(addressList -> addressList.stream()
                .map(address -> new NamedTaskLocation(
                    getDisplayNameFromAddress(address),
                    new LatLng(address.getLatitude(), address.getLongitude())
                ))
                .collect(Collectors.toList())
            );
    }

    @Override
    public Observable<Result<NamedTaskLocation>> getBestReverseGeocodeResult(final LatLng latLng) {
        return Observable.fromCallable(() -> geocoder.getFromLocation(
            latLng.getLatitude(),
            latLng.getLongitude(),
            MAX_RESULTS_TO_SCAN
        ))
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.computation())
            .map(fullAddressList -> {
                if (fullAddressList.size() == 0) {
                    return Result.failure(new ReverseGeocodeException("Failed to find any geocode results"));
                }
                final List<Address> addressesWithAllDisplayInfo = fullAddressList.stream()
                    .filter(AndroidGeocodeInteractor::hasAddressNumberAndStreet)
                    .collect(Collectors.toList());
                // Use an address with good display information, or else just return the first address in the list
                final Address addressToReturn = addressesWithAllDisplayInfo.size() > 0
                    ? addressesWithAllDisplayInfo.get(0)
                    : fullAddressList.get(0);
                return Result.success(
                    new NamedTaskLocation(
                        getDisplayNameFromAddress(addressToReturn),
                        new LatLng(addressToReturn.getLatitude(), addressToReturn.getLongitude())
                    )
                );
            });
    }

    private static String getDisplayNameFromAddress(final Address address) {
        final String number = address.getSubThoroughfare();
        final String street = address.getThoroughfare();
        final String city = address.getLocality();

        if (number != null && street != null) {
            return String.format("%s %s", number, street);
        } else if (street != null && city != null) {
            return String.format("%s, %s", street, city);
        } else if (city != null) {
            return String.format("%s", city);
        } else {
            return "Unknown Location";
        }
    }

    private static boolean hasAddressNumberAndStreet(final Address address) {
        return address.getSubThoroughfare() != null && address.getThoroughfare() != null;
    }

    public static class ReverseGeocodeException extends Exception {
        ReverseGeocodeException(final String message) {
            super(message);
        }
    }
}
