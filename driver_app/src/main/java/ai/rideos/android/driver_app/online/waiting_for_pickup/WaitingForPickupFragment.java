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
package ai.rideos.android.driver_app.online.waiting_for_pickup;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.online.waiting_for_pickup.WaitingForPickupFragment.WaitingForPickupArgs;
import ai.rideos.android.model.VehiclePlan.Waypoint;
import ai.rideos.android.view.ActionDetailView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import java.io.Serializable;

public class WaitingForPickupFragment extends FragmentViewController<WaitingForPickupArgs, WaitingForPickupListener> {
    public static class WaitingForPickupArgs implements Serializable {
        private final Waypoint waypointToComplete;

        public WaitingForPickupArgs(final Waypoint waypointToComplete) {
            this.waypointToComplete = waypointToComplete;
        }
    }

    @Override
    public ControllerTypes<WaitingForPickupArgs, WaitingForPickupListener> getTypes() {
        return new ControllerTypes<>(WaitingForPickupArgs.class, WaitingForPickupListener.class);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.action_detail);
        final ActionDetailView actionView = view.findViewById(R.id.action_detail_container);

        final int numPassengers = getArgs().waypointToComplete.getAction().getTripResourceInfo().getNumPassengers();
        final String titleText = getResources().getQuantityString(R.plurals.waiting_for_passengers_title_text, numPassengers);
        final String detailText = getResources().getQuantityString(
            R.plurals.waiting_for_passengers_detail_text,
            numPassengers,
            numPassengers
        );
        actionView.getTitleView().setText(titleText);
        actionView.getDetailView().setText(detailText);

        actionView.getActionButton().setText(R.string.waiting_for_pickup_confirmation_button);
        actionView.getActionButton()
            .setOnClickListener(click -> getListener().pickedUpPassenger(getArgs().waypointToComplete));
        return view;
    }
}
