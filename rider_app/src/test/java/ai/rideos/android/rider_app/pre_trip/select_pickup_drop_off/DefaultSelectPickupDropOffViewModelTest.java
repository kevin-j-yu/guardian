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
package ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off;

import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.model.DesiredAndAssignedLocation;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.PreTripLocation;
import ai.rideos.android.model.SelectPickupDropOffDisplayState;
import ai.rideos.android.model.SelectPickupDropOffDisplayState.SetPickupDropOffStep;
import io.reactivex.observers.TestObserver;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultSelectPickupDropOffViewModelTest {
    private static final NamedTaskLocation PICKUP = new NamedTaskLocation(
        "pickup",
        new LatLng(0, 1)
    );
    private static final PreTripLocation CONFIRMED_PICKUP = new PreTripLocation(new DesiredAndAssignedLocation(PICKUP), true);
    private static final PreTripLocation UNCONFIRMED_PICKUP = new PreTripLocation(new DesiredAndAssignedLocation(PICKUP), false);

    private static final NamedTaskLocation DROP_OFF = new NamedTaskLocation(
        "drop-off",
        new LatLng(0, 2)
    );
    private static final PreTripLocation CONFIRMED_DROP_OFF = new PreTripLocation(new DesiredAndAssignedLocation(DROP_OFF), true);
    private static final PreTripLocation UNCONFIRMED_DROP_OFF = new PreTripLocation(new DesiredAndAssignedLocation(DROP_OFF), false);

    private DefaultSelectPickupDropOffViewModel viewModelUnderTest;
    private SetPickupDropOffListener listener;

    private void setUpWithInitialState(final PreTripLocation pickup, final PreTripLocation dropOff) {
        listener = Mockito.mock(SetPickupDropOffListener.class);
        viewModelUnderTest = new DefaultSelectPickupDropOffViewModel(
            listener,
            new TrampolineSchedulerProvider()
        );
        viewModelUnderTest.initialize(pickup, dropOff, LocationSearchFocusType.DROP_OFF);
    }

    @Test
    public void testInitialDisplayStateDefaultsToLocationSearch() {
        setUpWithInitialState(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);
        viewModelUnderTest.getDisplayState().test()
            .assertValueAt(0, new SelectPickupDropOffDisplayState(
                SetPickupDropOffStep.SEARCHING_FOR_PICKUP_DROP_OFF,
                PICKUP,
                DROP_OFF,
                LocationSearchFocusType.DROP_OFF
            ));
    }

    @Test
    public void testSelectingPickupDropOffWhileSearchingFinishesSelection() {
        setUpWithInitialState(null, null);
        final TestObserver<SelectPickupDropOffDisplayState> testObserver = viewModelUnderTest.getDisplayState().test();

        viewModelUnderTest.selectPickup(PICKUP);
        viewModelUnderTest.selectDropOff(DROP_OFF);

        Mockito.verify(listener).selectPickupDropOff(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);
        // Selecting pickup/drop-off should not update the display
        testObserver.assertValueCount(1);
    }

    @Test
    public void testCallingDoneSearchingFinishesSelectionWhenPickupAndDropOffInitiallySet() {
        setUpWithInitialState(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);
        viewModelUnderTest.doneSearching();
        Mockito.verify(listener).selectPickupDropOff(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);
    }

    @Test
    public void testSelectingPickupAfterPickupAndDropOffInitiallySetFinishesSelection() {
        setUpWithInitialState(UNCONFIRMED_PICKUP, UNCONFIRMED_DROP_OFF);
        final NamedTaskLocation updatedPickup = new NamedTaskLocation("pickup2", new LatLng(1, 1));
        viewModelUnderTest.selectPickup(updatedPickup);

        Mockito.verify(listener).selectPickupDropOff(
            new PreTripLocation(new DesiredAndAssignedLocation(updatedPickup), false),
            UNCONFIRMED_DROP_OFF
        );
    }

    @Test
    public void testSetPickupOnMapGoesBackToSearchWhenDropOffNotSet() {
        setUpWithInitialState(UNCONFIRMED_PICKUP, null);
        final NamedTaskLocation updatedPickup = new NamedTaskLocation("pickup2", new LatLng(1, 1));
        final TestObserver<SelectPickupDropOffDisplayState> testObserver = viewModelUnderTest.getDisplayState().test();

        viewModelUnderTest.setPickupOnMap();
        testObserver.assertValueCount(2)
            .assertValueAt(1, new SelectPickupDropOffDisplayState(
                SetPickupDropOffStep.SETTING_PICKUP_ON_MAP,
                PICKUP,
                null,
                LocationSearchFocusType.DROP_OFF
            ));
        viewModelUnderTest.confirmPickup(new DesiredAndAssignedLocation(updatedPickup));
        testObserver.assertValueCount(3)
            .assertValueAt(2, new SelectPickupDropOffDisplayState(
                SetPickupDropOffStep.SEARCHING_FOR_PICKUP_DROP_OFF,
                updatedPickup,
                null,
                LocationSearchFocusType.DROP_OFF
            ));
    }

    @Test
    public void testSetDropOffOnMapGoesBackToSearchWhenPickupNotSet() {
        setUpWithInitialState(null, UNCONFIRMED_DROP_OFF);
        final NamedTaskLocation updatedDropOff = new NamedTaskLocation("drop-off2", new LatLng(1, 1));
        final TestObserver<SelectPickupDropOffDisplayState> testObserver = viewModelUnderTest.getDisplayState().test();

        viewModelUnderTest.setDropOffOnMap();
        testObserver.assertValueCount(2)
            .assertValueAt(1, new SelectPickupDropOffDisplayState(
                SetPickupDropOffStep.SETTING_DROP_OFF_ON_MAP,
                null,
                DROP_OFF,
                LocationSearchFocusType.PICKUP
            ));
        viewModelUnderTest.confirmDropOff(new DesiredAndAssignedLocation(updatedDropOff));
        testObserver.assertValueCount(3)
            .assertValueAt(2, new SelectPickupDropOffDisplayState(
                SetPickupDropOffStep.SEARCHING_FOR_PICKUP_DROP_OFF,
                null,
                updatedDropOff,
                LocationSearchFocusType.PICKUP
            ));
    }

    @Test
    public void testSetLocationOnMapFinishesSelectionIfBothLocationsSet() {
        setUpWithInitialState(UNCONFIRMED_PICKUP, null);
        final TestObserver<SelectPickupDropOffDisplayState> testObserver = viewModelUnderTest.getDisplayState().test();

        viewModelUnderTest.setDropOffOnMap();
        testObserver.assertValueCount(2)
            .assertValueAt(1, new SelectPickupDropOffDisplayState(
                SetPickupDropOffStep.SETTING_DROP_OFF_ON_MAP,
                PICKUP,
                null,
                LocationSearchFocusType.DROP_OFF
            ));
        viewModelUnderTest.confirmDropOff(CONFIRMED_DROP_OFF.getDesiredAndAssignedLocation());
        testObserver.assertValueCount(2);
        Mockito.verify(listener).selectPickupDropOff(UNCONFIRMED_PICKUP, CONFIRMED_DROP_OFF);
    }

}
