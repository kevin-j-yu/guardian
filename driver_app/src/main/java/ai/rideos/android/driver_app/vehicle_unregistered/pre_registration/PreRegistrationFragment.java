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
package ai.rideos.android.driver_app.vehicle_unregistered.pre_registration;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.driver_app.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;

public class PreRegistrationFragment extends FragmentViewController<EmptyArg, PreRegistrationListener> {
    @Override
    public ControllerTypes<EmptyArg, PreRegistrationListener> getTypes() {
        return new ControllerTypes<>(EmptyArg.class, PreRegistrationListener.class);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.pre_registration);
        final Button startRegistrationButton = view.findViewById(R.id.start_registration_button);
        startRegistrationButton.setOnClickListener(click -> getListener().startRegistration());
        return view;
    }
}
