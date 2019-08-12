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

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.google.interactors.AndroidGeocodeInteractor;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class AndroidGeocodeInteractorTest {
    private static LatLng LAT_LNG = new LatLng(0, 1);

    private Geocoder geocoder;
    private AndroidGeocodeInteractor interactorUnderTest;

    @Before
    public void setUp() {
        geocoder = Mockito.mock(Geocoder.class);
        interactorUnderTest = new AndroidGeocodeInteractor(geocoder, new TrampolineSchedulerProvider());
    }

    @Test
    public void getLocationGivenNumberAndStreet() throws IOException {
        assertLocationGivenAddress(
            createAddress("221", "Main Street", "SF"),
            new NamedTaskLocation("221 Main Street", LAT_LNG)
        );
    }

    @Test
    public void getLocationGivenStreetAndCity() throws IOException {
        assertLocationGivenAddress(
            createAddress(null, "Main Street", "SF"),
            new NamedTaskLocation("Main Street, SF", LAT_LNG)
        );
    }

    @Test
    public void getLocationGivenCity() throws IOException {
        assertLocationGivenAddress(
            createAddress(null, null, "SF"),
            new NamedTaskLocation("SF", LAT_LNG)
        );
    }

    @Test
    public void testGetBestReverseGeocodeReturnsLocationWithStreetAndNumber() throws IOException {
        final Address badAddress = createAddress(null, null, "SF");
        final Address goodAddress = createAddress("221", "Main Street", "SF");
        Mockito.when(geocoder.getFromLocation(Matchers.eq(LAT_LNG.getLatitude()), Matchers.eq(LAT_LNG.getLongitude()), Matchers.anyInt()))
            .thenReturn(Arrays.asList(badAddress, goodAddress));
        final String expectedLocation = "221 Main Street";
        interactorUnderTest.getBestReverseGeocodeResult(LAT_LNG).test()
            .assertValueAt(
                0,
                result -> result.isSuccess() && result.get().getDisplayName().equals(expectedLocation)
            );
    }

    @Test
    public void testGetBestReverseGeocodeReturnsFailureWhenNoAddressesFound() throws IOException {
        Mockito.when(geocoder.getFromLocation(Matchers.eq(LAT_LNG.getLatitude()), Matchers.eq(LAT_LNG.getLongitude()), Matchers.anyInt()))
            .thenReturn(Collections.emptyList());
        interactorUnderTest.getBestReverseGeocodeResult(LAT_LNG).test()
            .assertValueAt(0, Result::isFailure);
    }

    @Test
    public void testGetBestReverseGeocodeReturnsFirstAddressWhenNoneHaveStreetAndNumber() throws IOException {
        final Address badAddress = createAddress(null, null, "SF");
        Mockito.when(geocoder.getFromLocation(Matchers.eq(LAT_LNG.getLatitude()), Matchers.eq(LAT_LNG.getLongitude()), Matchers.anyInt()))
            .thenReturn(Collections.singletonList(badAddress));
        final String expectedLocation = "SF";
        interactorUnderTest.getBestReverseGeocodeResult(LAT_LNG).test()
            .assertValueAt(
                0,
                result -> result.isSuccess() && result.get().getDisplayName().equals(expectedLocation)
            );
    }

    private void assertLocationGivenAddress(final Address address,
                                            final NamedTaskLocation expectedLocation) throws IOException {
        Mockito.when(geocoder.getFromLocation(LAT_LNG.getLatitude(), LAT_LNG.getLongitude(), 1))
            .thenReturn(Collections.singletonList(address));
        interactorUnderTest.getReverseGeocodeResults(LAT_LNG, 1).test()
            .assertValueCount(1)
            .assertValueAt(0, list -> list.size() == 1 && list.get(0).equals(expectedLocation));
    }

    private static Address createAddress(final String number, final String street, final String city) {
        final Address address = Mockito.mock(Address.class);
        Mockito.when(address.getSubThoroughfare()).thenReturn(number);
        Mockito.when(address.getThoroughfare()).thenReturn(street);
        Mockito.when(address.getLocality()).thenReturn(city);
        Mockito.when(address.getLatitude()).thenReturn(LAT_LNG.getLatitude());
        Mockito.when(address.getLongitude()).thenReturn(LAT_LNG.getLongitude());
        return address;
    }
}
