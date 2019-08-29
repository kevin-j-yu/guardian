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
package ai.rideos.android.rider_app.on_trip.current_trip.cancelled;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.model.TripStateModel.CancellationReason;
import ai.rideos.android.model.TripStateModel.CancellationReason.Source;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.on_trip.current_trip.cancelled.CancelledFragment.CancelledArgs;
import ai.rideos.android.viewmodel.ClearMapDetailsMapStateProvider;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class CancelledFragment extends FragmentViewController<CancelledArgs, CancelledListener> {
    public static class CancelledArgs implements Serializable {
        @Nullable
        private final CancellationReason cancellationReason;

        public CancelledArgs(@Nullable final CancellationReason cancellationReason) {
            this.cancellationReason = cancellationReason;
        }
    }

    private CompositeDisposable compositeDisposable;

    @Override
    public ControllerTypes<CancelledArgs, CancelledListener> getTypes() {
        return new ControllerTypes<>(CancelledArgs.class, CancelledListener.class);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final CancellationReason cancellationReason = getArgs().cancellationReason;
        if (cancellationReason == null || cancellationReason.getSource() == Source.RIDER) {
            displayCancellationSnackbar(container);
        } else {
            displayCancellationReasonDialog(cancellationReason);
        }
        return BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.empty_fragment);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(MapRelay.get().connectToProvider(new ClearMapDetailsMapStateProvider()));
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    private void displayCancellationSnackbar(final ViewGroup viewGroup) {
        final Snackbar snackBar = Snackbar.make(
            viewGroup,
            getString(R.string.cancelled_start_screen_message),
            Snackbar.LENGTH_LONG
        );
        snackBar.setAction(R.string.dismiss_start_screen_message, v -> snackBar.dismiss());
        snackBar.addCallback(new BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(final Snackbar transientBottomBar, final int event) {
                super.onDismissed(transientBottomBar, event);
                getListener().doneConfirmingCancellation();
            }
        });
        snackBar.show();
    }

    private void displayCancellationReasonDialog(final CancellationReason cancellationReason) {
        final String reasonMessage;
        if (cancellationReason.getDescription().isEmpty()) {
            reasonMessage = "";
        } else {
            reasonMessage = getString(R.string.cancellation_reason_prefix) + cancellationReason.getDescription();
        }
        new AlertDialog.Builder(getContext())
            .setTitle(getString(R.string.external_trip_cancel_title))
            .setMessage(
                getString(R.string.external_trip_cancel_explanation) + reasonMessage
            )
            .setPositiveButton(
                getString(R.string.external_trip_cancel_confirm_button),
                (dialog, i) -> getListener().doneConfirmingCancellation()
            )
            .setOnCancelListener(dialog -> getListener().doneConfirmingCancellation())
            .create()
            .show();
    }
}
