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
package ai.rideos.android.rider_app.pre_trip.confirm_trip;

import ai.rideos.android.rider_app.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ConfirmSeatsDialogView extends ConstraintLayout {
    private LinearLayout seatSelection;
    private Button confirmSeatsButton;

    public ConfirmSeatsDialogView(final Context context) {
        super(context);
    }

    public ConfirmSeatsDialogView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmSeatsDialogView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.seatSelection = findViewById(R.id.seat_selection);
        this.confirmSeatsButton = findViewById(R.id.confirm_seats_button);
    }

    public LinearLayout getSeatSelection() {
        return seatSelection;
    }

    public Button getConfirmSeatsButton() {
        return confirmSeatsButton;
    }
}
