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

import ai.rideos.android.common.view.Presenter;
import ai.rideos.android.rider_app.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class ConfirmSeatsDialogPresenter implements Presenter<ConfirmSeatsDialogView> {
    public interface ConfirmSeatsListener {
        void confirmSeats(final int numSeats);
    }

    private final Context context;
    private final AppCompatDialog dialog;
    private final ConfirmSeatsListener confirmSeatsListener;

    private ConfirmSeatsDialogView dialogView;

    public ConfirmSeatsDialogPresenter(final Context context,
                                       final ConfirmSeatsListener confirmSeatsListener) {
        this.context = context;
        this.confirmSeatsListener = confirmSeatsListener;
        dialog = new BottomSheetDialog(context);
    }

    @Override
    public void attach(final ConfirmSeatsDialogView dialogView) {
        this.dialogView = dialogView;
        dialog.setContentView(dialogView);
    }

    @Override
    public void detach() {
        if (dialog.isShowing()) {
            dialog.hide();
        }
    }

    public void setSeatSelectionBounds(final int min, final int max) {
        dialogView.getSeatSelection().removeAllViews();
        final List<Button> seatButtons = new ArrayList<>(max - min + 1);
        for (int seatCount = min; seatCount <= max; seatCount++) {
            final int index = seatCount - min;
            final View buttonWrapper = LayoutInflater.from(context)
                .inflate(R.layout.seat_button, dialogView.getSeatSelection(), false);

            final Button innerButton = buttonWrapper.findViewById(R.id.circle_button);
            innerButton.setText(Integer.toString(seatCount));
            innerButton.setOnClickListener(click -> selectButton(seatButtons, index));

            seatButtons.add(innerButton);
            dialogView.getSeatSelection().addView(buttonWrapper);
        }
        selectButton(seatButtons, 0);

        dialogView.getConfirmSeatsButton().setOnClickListener(click -> confirmSeats(seatButtons, min));
    }

    public void show() {
        dialog.show();
    }

    private static void selectButton(final List<Button> seatButtons, final int index) {
        for (int i = 0; i < seatButtons.size(); i++) {
            if (i == index) {
                seatButtons.get(i).setSelected(true);
            } else {
                seatButtons.get(i).setSelected(false);
            }
        }
    }

    private void confirmSeats(final List<Button> seatButtons, final int minCount) {
        for (int i = 0; i < seatButtons.size(); i++) {
            if (seatButtons.get(i).isSelected()) {
                confirmSeatsListener.confirmSeats(minCount + i);
                dialog.hide();
            }
        }
    }
}
