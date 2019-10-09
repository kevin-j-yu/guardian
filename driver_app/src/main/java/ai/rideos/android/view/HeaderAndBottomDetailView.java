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

import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.view.ViewMarginProvider;
import ai.rideos.android.common.view.ViewMargins;
import ai.rideos.android.driver_app.R;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;

public class HeaderAndBottomDetailView extends ConstraintLayout implements ViewMarginProvider {
    public static HeaderAndBottomDetailView inflate(final LayoutInflater layoutInflater,
                                                    final ViewGroup container,
                                                    final Activity activity,
                                                    @LayoutRes final int headerLayout,
                                                    @LayoutRes final int bottomDetailLayout) {
        final HeaderAndBottomDetailView headerAndBottomDetailView = (HeaderAndBottomDetailView) layoutInflater.inflate(
            R.layout.header_and_bottom_detail,
            container,
            false
        );

        final ViewGroup headerContainer = headerAndBottomDetailView.findViewById(R.id.header_container);
        final View headerView = layoutInflater.inflate(headerLayout, headerContainer, false);
        headerContainer.addView(headerView);

        final View menuButton = headerView.findViewById(R.id.top_button);
        if (activity instanceof OpenMenuListener) {
            menuButton.setOnClickListener(click -> ((OpenMenuListener) activity).openMenu());
        }

        final ViewGroup bottomContainer = headerAndBottomDetailView.findViewById(R.id.bottom_detail_container);
        final View bottomView = layoutInflater.inflate(bottomDetailLayout, bottomContainer, false);
        bottomContainer.addView(bottomView);

        return headerAndBottomDetailView;
    }

    public HeaderAndBottomDetailView(final Context context) {
        super(context);
    }

    public HeaderAndBottomDetailView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public HeaderAndBottomDetailView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void calculateViewMargins(final Consumer<ViewMargins> marginsConsumer) {
        final ViewGroup headerContainer = findViewById(R.id.header_container);
        final ViewGroup bottomDetailContainer = findViewById(R.id.bottom_detail_container);

        headerContainer.measure(0, 0);
        bottomDetailContainer.measure(0, 0);

        marginsConsumer.accept(
            ViewMargins.newBuilder()
                .setTop(headerContainer.getMeasuredHeight())
                .setBottom(bottomDetailContainer.getMeasuredHeight())
                .build()
        );
    }
}
