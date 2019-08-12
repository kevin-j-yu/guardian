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
package ai.rideos.android.rider_app.pre_trip.confirm_vehicle;

import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.reactive.SchedulerProviders.TrampolineSchedulerProvider;
import ai.rideos.android.common.view.resources.ResourceProvider;
import ai.rideos.android.interactors.AvailableVehicleInteractor;
import ai.rideos.android.model.AvailableVehicle;
import ai.rideos.android.model.VehicleSelectionOption;
import io.reactivex.Observable;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultConfirmVehicleViewModelTest {
    private static final String AUTOMATIC_DISPLAY_NAME = "AUTOMATIC";
    private static final AvailableVehicle VEHICLE_1 = new AvailableVehicle("vehicle-1", "vehicle1");
    private static final AvailableVehicle VEHICLE_2 = new AvailableVehicle("vehicle-2c", "vehicle2");
    private static final String FLEET_ID = "fleet-1";

    private DefaultConfirmVehicleViewModel viewModelUnderTest;
    private AvailableVehicleInteractor availableVehicleInteractor;

    @Before
    public void setUp() {
        availableVehicleInteractor = Mockito.mock(AvailableVehicleInteractor.class);
        final ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getString(Mockito.anyInt())).thenReturn(AUTOMATIC_DISPLAY_NAME);

        viewModelUnderTest = new DefaultConfirmVehicleViewModel(
            availableVehicleInteractor,
            Observable.just(new FleetInfo(FLEET_ID)),
            resourceProvider,
            new TrampolineSchedulerProvider()
        );
    }

    @Test
    public void testGetVehicleSelectionOptionsRetrievesAvailableVehiclesInFleet() {
        Mockito.when(availableVehicleInteractor.getAvailableVehicles(FLEET_ID))
            .thenReturn(Observable.just(Arrays.asList(VEHICLE_1, VEHICLE_2)));

        viewModelUnderTest.getVehicleSelectionOptions().test()
            .assertValueCount(1)
            .assertValueAt(0, options -> options.getOptions().size() == 3
                && options.getOptions().get(0).getValue().equals(VehicleSelectionOption.automatic())
                && options.getOptions().get(1).getValue().equals(VehicleSelectionOption.manual(VEHICLE_1.getVehicleId()))
                && options.getOptions().get(2).getValue().equals(VehicleSelectionOption.manual(VEHICLE_2.getVehicleId()))
                && options.getSelectionIndex().isPresent()
                && options.getSelectionIndex().get() == 0
            );
    }
}
