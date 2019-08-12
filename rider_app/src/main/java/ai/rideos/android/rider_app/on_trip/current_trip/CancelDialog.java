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
package ai.rideos.android.rider_app.on_trip.current_trip;

import ai.rideos.android.rider_app.R;
import android.content.Context;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

public class CancelDialog {
    public static AlertDialog showDialog(final Context context,
                                         @StringRes final int messageResourceId,
                                         final Runnable cancelListener) {
        return new Builder(context, R.style.DefaultAlertDialogTheme)
            .setTitle(R.string.cancel_on_trip_dialog_title)
            .setMessage(messageResourceId)
            .setPositiveButton(R.string.cancel_positive_button, (dialog, i) -> cancelListener.run())
            .setNegativeButton(R.string.cancel_negative_button, null)
            .show();
    }
}
