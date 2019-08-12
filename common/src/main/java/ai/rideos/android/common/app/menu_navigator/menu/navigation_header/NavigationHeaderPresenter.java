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
package ai.rideos.android.common.app.menu_navigator.menu.navigation_header;

import ai.rideos.android.common.R;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.view.Presenter;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class NavigationHeaderPresenter implements Presenter<View> {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final NavigationHeaderViewModel viewModel;

    public NavigationHeaderPresenter(final Context context) {
        viewModel = new DefaultNavigationHeaderViewModel(
            User.get(context),
            SharedPreferencesUserStorageReader.forContext(context)
        );
    }

    @Override
    public void attach(final View view) {
        final ImageView profileImage = view.findViewById(R.id.profile_image);
        final TextView fullNameText = view.findViewById(R.id.user_full_name);
        final TextView emailText = view.findViewById(R.id.user_email);

        compositeDisposable.addAll(
            viewModel.getFullName().observeOn(AndroidSchedulers.mainThread()).subscribe(fullNameText::setText),
            viewModel.getEmail().observeOn(AndroidSchedulers.mainThread()).subscribe(emailText::setText),
            viewModel.getProfilePictureUrl()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(url -> displayImageView(profileImage, url))
        );
    }

    private void displayImageView(final ImageView imageView, final String imageUrl) {
        Picasso.get()
            .load(imageUrl)
            .into(imageView);
    }

    @Override
    public void detach() {
        compositeDisposable.dispose();
    }
}
