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
package ai.rideos.android.common.view.layout;

import ai.rideos.android.common.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

/**
 * LoadableDividerView defines a horizontal rule divider that can be swapped for a horizontal progress bar in-place.
 */
public class LoadableDividerView extends FrameLayout {
    private View horizontalRule;
    private ProgressBar progressBar;
    
    public LoadableDividerView(final Context context) {
        super(context);
        init(context);
    }

    public LoadableDividerView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadableDividerView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        inflate(context, R.layout.loadable_divider, this);
        horizontalRule = findViewById(R.id.horizontal_rule);
        progressBar = findViewById(R.id.progress_bar);
        stopLoading();
    }

    public void startLoading() {
        horizontalRule.setVisibility(INVISIBLE);
        progressBar.setVisibility(VISIBLE);
    }

    public void stopLoading() {
        progressBar.setVisibility(INVISIBLE);
        horizontalRule.setVisibility(VISIBLE);
    }
}
