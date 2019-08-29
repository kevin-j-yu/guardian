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
package ai.rideos.android.rider_app.on_trip.current_trip.trip_completed;

import ai.rideos.android.common.app.map.MapRelay;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.view.layout.BottomDetailAndButtonView;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.on_trip.current_trip.trip_completed.TripCompletedFragment.TripCompletedArgs;
import ai.rideos.android.viewmodel.ClearMapDetailsMapStateProvider;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import java.io.Serializable;

public class TripCompletedFragment extends FragmentViewController<TripCompletedArgs, TripCompletedListener> {
    public static class TripCompletedArgs implements Serializable {
        private final NamedTaskLocation dropOffLocation;

        public TripCompletedArgs(final NamedTaskLocation dropOffLocation) {
            this.dropOffLocation = dropOffLocation;
        }
    }

    private CompositeDisposable compositeDisposable;

    @Override
    public ControllerTypes<TripCompletedArgs, TripCompletedListener> getTypes() {
        return new ControllerTypes<>(TripCompletedArgs.class, TripCompletedListener.class);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = BottomDetailAndButtonView.inflateWithMenuButton(inflater, container, getActivity(), R.layout.trip_completed);
        final TextView dropOffText = view.findViewById(R.id.drop_off_address_text);
        dropOffText.setText(getArgs().dropOffLocation.getDisplayName());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Button doneButton = getView().findViewById(R.id.done_button);
        final TripCompletedListener listener = getListener();
        doneButton.setOnClickListener(click -> listener.tripFinished());

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(MapRelay.get().connectToProvider(new ClearMapDetailsMapStateProvider()));
    }

    @Override
    public void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }
}
