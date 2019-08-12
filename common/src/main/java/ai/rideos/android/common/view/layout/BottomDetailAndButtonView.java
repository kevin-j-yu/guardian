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
import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.view.ViewMarginProvider;
import ai.rideos.android.common.view.ViewMargins;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;

public class BottomDetailAndButtonView extends ConstraintLayout implements ViewMarginProvider {

    public static BottomDetailAndButtonView inflateWithUpButton(final LayoutInflater layoutInflater,
                                                                final ViewGroup container,
                                                                final Runnable onUpButtonClick,
                                                                @LayoutRes final int detailLayout) {
        return inflateWithButton(layoutInflater, container, onUpButtonClick, detailLayout, R.drawable.ic_arrow_back_24dp);
    }

    public static BottomDetailAndButtonView inflateWithMenuButton(final LayoutInflater layoutInflater,
                                                                  final ViewGroup container,
                                                                  final Activity activity,
                                                                  @LayoutRes final int detailLayout) {
        Runnable onMenuClick = () -> {};
        if (activity instanceof OpenMenuListener) {
            onMenuClick = ((OpenMenuListener) activity)::openMenu;
        }
        return inflateWithButton(layoutInflater, container, onMenuClick, detailLayout, R.drawable.ic_menu);
    }

    public static BottomDetailAndButtonView inflateWithNoButton(final LayoutInflater layoutInflater,
                                                                final ViewGroup container,
                                                                @LayoutRes final int detailLayout) {
        final BottomDetailAndButtonView bottomDetailView = inflateViewWithDetail(layoutInflater, container, detailLayout);
        bottomDetailView.findViewById(R.id.top_button).setVisibility(GONE);
        return bottomDetailView;
    }

    private static BottomDetailAndButtonView inflateWithButton(final LayoutInflater layoutInflater,
                                                               final ViewGroup container,
                                                               final Runnable onButtonClick,
                                                               @LayoutRes final int detailLayout,
                                                               @DrawableRes final int buttonDrawable) {
        final BottomDetailAndButtonView bottomDetailView = inflateViewWithDetail(layoutInflater, container, detailLayout);

        final ImageButton topButton = bottomDetailView.findViewById(R.id.top_button);
        topButton.setImageResource(buttonDrawable);
        topButton.setOnClickListener(click -> onButtonClick.run());
        return bottomDetailView;
    }

    private static BottomDetailAndButtonView inflateViewWithDetail(final LayoutInflater layoutInflater,
                                                                   final ViewGroup container,
                                                                   @LayoutRes final int detailLayout) {
        final BottomDetailAndButtonView bottomDetailView = (BottomDetailAndButtonView) layoutInflater.inflate(
            R.layout.bottom_detail_and_button,
            container,
            false
        );
        final ViewGroup bottomContentContainer = bottomDetailView.findViewById(R.id.bottom_content);
        final View bottomContent = layoutInflater.inflate(detailLayout, bottomContentContainer, false);
        bottomContentContainer.addView(bottomContent);
        return bottomDetailView;
    }

    public BottomDetailAndButtonView(final Context context) {
        super(context);
    }

    public BottomDetailAndButtonView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomDetailAndButtonView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void calculateViewMargins(final Consumer<ViewMargins> marginsConsumer) {
        final ViewGroup bottomDetailContainer = findViewById(R.id.bottom_content);

        bottomDetailContainer.measure(0, 0);

        final View topButton = findViewById(R.id.top_button);
        topButton.measure(0, 0);

        int[] topButtonLocation = new int[2];
        topButton.getLocationOnScreen(topButtonLocation);
        int bottomOfTopButton = topButtonLocation[1] + topButton.getMeasuredHeight();
        marginsConsumer.accept(ViewMargins.newBuilder()
            .setTop(bottomOfTopButton)
            .setBottom(bottomDetailContainer.getMeasuredHeight())
            .build()
        );
    }
}
