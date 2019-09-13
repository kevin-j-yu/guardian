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
package ai.rideos.android.common.app.progress;

import ai.rideos.android.common.view.errors.ErrorDialog;
import ai.rideos.android.common.view.layout.LoadableDividerView;
import ai.rideos.android.common.viewmodel.progress.ProgressSubject.ProgressState;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.StringRes;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;

public class ProgressConnector {
    private final List<Runnable> onIdleActions;
    private final List<Runnable> onLoadingActions;
    private final List<Runnable> onSuccessActions;
    private final List<Runnable> onFailureActions;

    private ProgressConnector(final List<Runnable> onIdleActions,
                              final List<Runnable> onLoadingActions,
                              final List<Runnable> onSuccessActions,
                              final List<Runnable> onFailureActions) {
        this.onIdleActions = onIdleActions;
        this.onLoadingActions = onLoadingActions;
        this.onSuccessActions = onSuccessActions;
        this.onFailureActions = onFailureActions;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Runnable> idleActionsToBuild = new ArrayList<>();
        private final List<Runnable> loadingActionsToBuild = new ArrayList<>();
        private final List<Runnable> successActionsToBuild = new ArrayList<>();
        private final List<Runnable> failureActionsToBuild = new ArrayList<>();

        public ProgressConnector build() {
            return new ProgressConnector(
                idleActionsToBuild,
                loadingActionsToBuild,
                successActionsToBuild,
                failureActionsToBuild
            );
        }

        public Builder showLoadableDividerWhenLoading(final LoadableDividerView loadableDividerView) {
            idleActionsToBuild.add(loadableDividerView::stopLoading);
            loadingActionsToBuild.add(loadableDividerView::startLoading);
            successActionsToBuild.add(loadableDividerView::stopLoading);
            failureActionsToBuild.add(loadableDividerView::stopLoading);
            return this;
        }

        public Builder disableButtonWhenLoading(final Button button) {
            idleActionsToBuild.add(() -> button.setEnabled(true));
            loadingActionsToBuild.add(() -> button.setEnabled(false));
            successActionsToBuild.add(() -> button.setEnabled(true));
            failureActionsToBuild.add(() -> button.setEnabled(true));
            return this;
        }

        public Builder disableButtonWhenLoadingOrSuccessful(final Button button) {
            idleActionsToBuild.add(() -> button.setEnabled(true));
            loadingActionsToBuild.add(() -> button.setEnabled(false));
            successActionsToBuild.add(() -> button.setEnabled(false));
            failureActionsToBuild.add(() -> button.setEnabled(true));
            return this;
        }

        public Builder toggleSwitchWhenLoading(final Switch switchToggle) {
            final boolean initialState = switchToggle.isChecked();
            idleActionsToBuild.add(() -> switchToggle.setEnabled(true));
            loadingActionsToBuild.add(() -> switchToggle.setEnabled(false));
            successActionsToBuild.add(() -> switchToggle.setEnabled(false));
            failureActionsToBuild.add(() -> {
                switchToggle.setChecked(initialState);
                switchToggle.setEnabled(true);
            });
            return this;
        }

        public Builder showTextWhenLoading(final TextView textView, @StringRes final int loadingText) {
            idleActionsToBuild.add(() -> textView.setVisibility(View.GONE));
            loadingActionsToBuild.add(() -> {
                textView.setVisibility(View.VISIBLE);
                textView.setText(loadingText);
            });
            successActionsToBuild.add(() -> textView.setVisibility(View.GONE));
            failureActionsToBuild.add(() -> textView.setVisibility(View.GONE));
            return this;
        }

        public Builder alertOnFailure(final Context context, @StringRes final int failureMessage) {
            failureActionsToBuild.add(() -> new ErrorDialog(context).show(context.getString(failureMessage)));
            return this;
        }

        public Builder doOnSuccess(final Runnable onSuccess) {
            successActionsToBuild.add(onSuccess);
            return this;
        }
    }

    public Disposable connect(final Observable<ProgressState> progressState) {
        return progressState.subscribe(state -> {
            switch (state) {
                case IDLE:
                    onIdleActions.forEach(Runnable::run);
                    break;
                case LOADING:
                    onLoadingActions.forEach(Runnable::run);
                    break;
                case SUCCEEDED:
                    onSuccessActions.forEach(Runnable::run);
                    break;
                case FAILED:
                    onFailureActions.forEach(Runnable::run);
                    break;
            }
        });
    }
}
