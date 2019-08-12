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
package ai.rideos.android.view;

import ai.rideos.android.driver_app.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

/**
 * ActionDetailView is a useful view for displaying a title text, detail text, and action button.
 */
public class ActionDetailView extends CardView {
    private TextView titleView;
    private TextView detailView;
    private Button actionButton;

    public ActionDetailView(@NonNull final Context context) {
        this(context, null);
    }

    public ActionDetailView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ActionDetailView(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        titleView = findViewById(R.id.action_title_text);
        detailView = findViewById(R.id.action_detail_text);
        actionButton = findViewById(R.id.action_button);
    }

    public TextView getTitleView() {
        return titleView;
    }

    public TextView getDetailView() {
        return detailView;
    }

    public Button getActionButton() {
        return actionButton;
    }
}
