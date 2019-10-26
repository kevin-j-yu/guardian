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
package ai.rideos.android.driver_app.alerts;

import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.ModalFragmentViewController;
import ai.rideos.android.driver_app.R;
import ai.rideos.android.driver_app.alerts.NewRideRequestAlertFragment.NewRideRequestAlertArgs;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.Locale;

public class NewRideRequestAlertFragment
    extends ModalFragmentViewController<NewRideRequestAlertArgs, DismissAlertListener> {
    public static class NewRideRequestAlertArgs implements Serializable {
        private final int numPassengers;

        public NewRideRequestAlertArgs(final int numPassengers) {
            this.numPassengers = numPassengers;
        }
    }

    @Override
    public ControllerTypes<NewRideRequestAlertArgs, DismissAlertListener> getTypes() {
        return new ControllerTypes<>(NewRideRequestAlertArgs.class, DismissAlertListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.new_request_alert, container, false);
        final TextView passengerCountText = view.findViewById(R.id.new_ride_request_passenger_count);
        passengerCountText.setText(String.format(Locale.getDefault(), "%d", getArgs().numPassengers));

        final Button dismissButton = view.findViewById(R.id.dismiss_alert_button);

        // Dismiss after 5 seconds
        final Handler handler = new Handler();
        final Runnable onDelay = this::dismiss;
        handler.postDelayed(onDelay, 5000);

        dismissButton.setOnClickListener(click -> {
            handler.removeCallbacks(onDelay);
            dismiss();
            getListener().didDismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
