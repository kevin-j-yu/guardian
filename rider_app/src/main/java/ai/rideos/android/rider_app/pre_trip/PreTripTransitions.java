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
package ai.rideos.android.rider_app.pre_trip;

import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/**
 * This class contains common transitions used during the pre-trip experience.
 */
public class PreTripTransitions {
    public static Transition changeDetailTransition(final ViewGroup detailContainer, final View detailView) {
        final TransitionSet transitionSet;
        if (detailContainer.getChildCount() == 0) {
            transitionSet = new TransitionSet()
                .addTransition(new Slide(Gravity.BOTTOM).addTarget(detailView));
        } else {
            transitionSet = new TransitionSet()
                .addTransition(new Fade()
                    .addTarget("detail_title")
                )
                .addTransition(new ChangeBounds()
                    .addTarget(detailContainer)
                    .addTarget("detail_view")
                    .addTarget("action_button")
                );
        }
        return transitionSet;
    }

    public static Transition removeLocationSearchTransition() {
        return new TransitionSet()
            .addTransition(new Slide(Gravity.TOP)
                .addTarget("search_card")
            )
            .addTransition(new Slide(Gravity.BOTTOM)
                .addTarget("search_recycler")
            );
    }
}
