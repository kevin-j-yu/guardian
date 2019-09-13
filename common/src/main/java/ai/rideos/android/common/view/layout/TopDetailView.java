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
import ai.rideos.android.common.view.ViewMarginProvider;
import ai.rideos.android.common.view.ViewMargins;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;

public class TopDetailView extends ConstraintLayout implements ViewMarginProvider {

    public TopDetailView(final Context context) {
        super(context);
    }

    public TopDetailView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TopDetailView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static TopDetailView inflateViewWithDetail(final LayoutInflater layoutInflater,
                                                      final ViewGroup container,
                                                      @LayoutRes final int detailLayout) {
        final TopDetailView topDetailView = (TopDetailView) layoutInflater.inflate(
            R.layout.top_detail,
            container,
            false
        );
        final ViewGroup topContentContainer = topDetailView.findViewById(R.id.top_content);
        final View topContent = layoutInflater.inflate(detailLayout, topContentContainer, false);
        topContentContainer.addView(topContent);
        return topDetailView;
    }

    @Override
    public void calculateViewMargins(final Consumer<ViewMargins> marginsConsumer) {
        final ViewGroup topDetail = findViewById(R.id.top_content);

        topDetail.measure(0, 0);

        marginsConsumer.accept(
            ViewMargins.newBuilder().setTop(topDetail.getMeasuredHeight()).build()
        );
    }
}
