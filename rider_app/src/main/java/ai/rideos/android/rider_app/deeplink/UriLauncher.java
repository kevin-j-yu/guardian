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
package ai.rideos.android.rider_app.deeplink;

import ai.rideos.android.rider_app.R;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.Optional;
import timber.log.Timber;

public class UriLauncher {
    private final Activity activity;
    private final String uriString;

    public UriLauncher(final Context context, final String uriString) {
        this.activity = (Activity) context;
        this.uriString = uriString;
    }

    public void launch() {
        if (uriString.isEmpty()) {
            Timber.e("Tried to open empty URI");
            return; // Don't do anything
        }

        final Uri uri = Uri.parse(uriString);
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            activity.startActivity(browserIntent);
        } catch (final ActivityNotFoundException e) {
            tryToShowPlayStoreLink(uri);
        }
    }

    private void tryToShowPlayStoreLink(final Uri uri) {
        final Optional<String> playStoreLink = KnownAppSchemes.getPlayStoreLinkFromScheme(uri.getScheme());
        if (playStoreLink.isPresent()) {
            final Uri playStoreUri = Uri.parse(playStoreLink.get());
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, playStoreUri);
            try {
                activity.startActivity(browserIntent);
            } catch (final ActivityNotFoundException e) {
                showFailureMessage();
            }
        } else {
            showFailureMessage();
        }
    }

    private void showFailureMessage() {
        new Builder(activity)
            .setMessage(activity.getString(R.string.open_uri_error_unknown_app, uriString))
            .setPositiveButton(activity.getString(R.string.open_uri_error_confirmation_button), (dialog, i) -> {})
            .create()
            .show();
    }
}
