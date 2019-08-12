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
package ai.rideos.android.rider_app.pre_trip.select_pickup_drop_off.location_search;

import ai.rideos.android.adapters.LocationSearchRecyclerAdapter;
import ai.rideos.android.common.architecture.ControllerTypes;
import ai.rideos.android.common.architecture.FragmentViewController;
import ai.rideos.android.common.device.FusedLocationDeviceLocator;
import ai.rideos.android.common.device.InputMethodManagerKeyboardManager;
import ai.rideos.android.common.device.KeyboardManager;
import ai.rideos.android.common.view.resources.AndroidResourceProvider;
import ai.rideos.android.model.LocationSearchFocusType;
import ai.rideos.android.model.LocationSearchInitialState;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class LocationSearchFragment extends FragmentViewController<LocationSearchInitialState, LocationSearchListener> {
    private CompositeDisposable compositeDisposable;

    private LocationSearchViewModel viewModel;
    private LocationSearchRecyclerAdapter recyclerAdapter;
    private EditText pickupText;
    private EditText dropOffText;
    private Button doneButton;
    private View clearPickup;
    private View clearDropOff;
    private KeyboardManager keyboardManager;
    private boolean blockTextUpdates = false;

    @Override
    public ControllerTypes<LocationSearchInitialState, LocationSearchListener> getTypes() {
        return new ControllerTypes<>(LocationSearchInitialState.class, LocationSearchListener.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new DefaultLocationSearchViewModel(
            getListener(),
            RiderDependencyRegistry.mapDependencyFactory().getAutocompleteInteractor(getContext()),
            RiderDependencyRegistry.riderDependencyFactory().getHistoricalSearchInteractor(getContext()),
            new FusedLocationDeviceLocator(getContext()),
            AndroidResourceProvider.forContext(getContext()),
            getArgs()
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final TransitionSet transitionSet = new TransitionSet()
            .addTransition(new ChangeBounds()
                .addTarget("search_card")
            )
            .addTransition(new Slide(Gravity.BOTTOM)
                .addTarget("search_recycler")
            );
        setSharedElementEnterTransition(transitionSet);
        setExitTransition(transitionSet);
        return inflater.inflate(R.layout.location_search, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();
        final View view = getView();
        final LocationSearchListener listener = getListener();
        final RecyclerView recyclerView = view.findViewById(R.id.search_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // specify an adapter (see also next example)
        recyclerAdapter = new LocationSearchRecyclerAdapter(viewModel::makeSelection);
        recyclerView.setAdapter(recyclerAdapter);

        pickupText = view.findViewById(R.id.edit_pickup);
        pickupText.addTextChangedListener(createTextWatcher(viewModel::setPickupInput));
        pickupText.setOnFocusChangeListener(createTextFocusListener(LocationSearchFocusType.PICKUP));

        dropOffText = view.findViewById(R.id.edit_drop_off);
        dropOffText.addTextChangedListener(createTextWatcher(viewModel::setDropOffInput));
        dropOffText.setOnFocusChangeListener(createTextFocusListener(LocationSearchFocusType.DROP_OFF));

        final View upButton = view.findViewById(R.id.up_icon);
        upButton.setOnClickListener(click -> listener.navigateUp());

        clearPickup = view.findViewById(R.id.clear_pickup);
        clearPickup.setOnClickListener(click -> pickupText.setText(""));

        clearDropOff = view.findViewById(R.id.clear_drop_off);
        clearDropOff.setOnClickListener(click -> dropOffText.setText(""));

        doneButton = view.findViewById(R.id.done_button);
        disableDoneButton();

        subscribeToViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (keyboardManager != null) {
            keyboardManager.hideKeyboard();
        }
        compositeDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    private void subscribeToViewModel() {
        // Subscribe to observable events
        compositeDisposable.addAll(
            viewModel.getInitialState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setUpWithInitialState),
            viewModel.getSelectedPickup()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(selection -> setTextWithoutUpdate(pickupText, selection)),
            viewModel.getSelectedDropOff()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(selection -> setTextWithoutUpdate(dropOffText, selection)),
            viewModel.getLocationOptions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(options -> recyclerAdapter.setLocations(options)),
            viewModel.isDoneActionEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(enabled -> {
                    if (enabled) {
                        enableDoneButton();
                    } else {
                        disableDoneButton();
                    }
                }),
            viewModel.canClearPickup().observeOn(AndroidSchedulers.mainThread())
                .subscribe(canClear -> setVisibility(clearPickup, canClear)),
            viewModel.canClearDropOff().observeOn(AndroidSchedulers.mainThread())
                .subscribe(canClear -> setVisibility(clearDropOff, canClear))
        );
    }

    private static void setVisibility(final View view, final boolean isVisible) {
        if (isVisible) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void setUpWithInitialState(final LocationSearchInitialState initialState) {
        if (initialState.getInitialPickup().isPresent()) {
            setTextWithoutUpdate(pickupText, initialState.getInitialPickup().get().getDisplayName());
        }
        if (initialState.getInitialDropOff().isPresent()) {
            setTextWithoutUpdate(dropOffText, initialState.getInitialDropOff().get().getDisplayName());
        }

        if (initialState.getInitialFocus() == LocationSearchFocusType.PICKUP) {
            pickupText.requestFocus();
            keyboardManager = new InputMethodManagerKeyboardManager(getContext(), pickupText);
            pickupText.postDelayed(keyboardManager::showKeyboard, 0);
        } else if (initialState.getInitialFocus() == LocationSearchFocusType.DROP_OFF) {
            dropOffText.requestFocus();
            keyboardManager = new InputMethodManagerKeyboardManager(getContext(), dropOffText);
            dropOffText.postDelayed(keyboardManager::showKeyboard, 0);
        }
    }

    private void enableDoneButton() {
        doneButton.setVisibility(View.VISIBLE);
        doneButton.setEnabled(true);
        doneButton.setOnClickListener(v -> viewModel.done());
    }

    private void disableDoneButton() {
        doneButton.setVisibility(View.GONE);
        doneButton.setOnClickListener(v -> {});
    }

    private void setTextWithoutUpdate(final EditText editText, final String text) {
        blockTextUpdates = true;
        editText.setText(text);
        blockTextUpdates = false;
    }

    private TextWatcher createTextWatcher(final Consumer<String> onChange) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (!blockTextUpdates) {
                    onChange.accept(s.toString());
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {
            }
        };
    }

    private OnFocusChangeListener createTextFocusListener(final LocationSearchFocusType focusToSet) {
        return (v, hasFocus) -> {
            if (hasFocus) {
                viewModel.setFocus(focusToSet);
            }
        };
    }
}
