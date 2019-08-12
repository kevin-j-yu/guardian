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
package ai.rideos.android.rider_app.on_trip;

import ai.rideos.android.rider_app.R;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;

public class OnTripTransitions {
    public static Transition changeDetail(final ViewGroup detailContainer, final View detailView) {
        return new TransitionSet()
            .addTransition(new ChangeBounds()
                .addTarget(detailContainer)
                .addTarget("detail_view")
                .addTarget(R.id.on_trip_state_title)
                .addTarget(R.id.pickup_address_text)
                .addTarget(R.id.drop_off_address_text)
                .addTarget(R.id.cancel_button)
            );
    }
}
