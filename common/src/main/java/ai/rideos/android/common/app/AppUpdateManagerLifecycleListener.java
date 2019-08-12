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
package ai.rideos.android.common.app;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import timber.log.Timber;

/**
 * AppUpdateManagerLifecycleListener can listen to several activity lifecycle events to trigger app update mechanisms.
 * When the activity is created, it checks whether there is an update. When resumed, it checks if there is an update
 * in progress and resumes the update.
 *
 * Note that these update requests are best effort. If the user cancels or otherwise disables the update, we will still
 * allow the user to use the app for the rest of the session. However, every time the app is opened, this will re-trigger.
 */
public class AppUpdateManagerLifecycleListener {
    private static final int UPDATE_REQUEST_CODE = 3695;

    private AppUpdateManager appUpdateManager;

    public void onActivityCreated(final Activity activity) {
        appUpdateManager = AppUpdateManagerFactory.create(activity);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (isUpdateAvailable(appUpdateInfo)) {
                requestUpdate(activity, appUpdateInfo);
            } else {
                Timber.d("Not requesting update. App is up to date");
            }
        });
    }

    public void onActivityResumed(final Activity activity) {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (isUpdateInProgress(appUpdateInfo)) {
                requestUpdate(activity, appUpdateInfo);
            } else {
                Timber.d("Not requesting update. No update in progress.");
            }
        });
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Timber.e("Failed to update app: %d", resultCode);
            }
        }
    }

    private void requestUpdate(final Activity activity, final AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,
                activity,
                UPDATE_REQUEST_CODE
            );
        } catch (final SendIntentException e) {
            Timber.e(e, "Unable to request update from the app store");
        }
    }

    private static boolean isUpdateAvailable(final AppUpdateInfo appUpdateInfo) {
        return appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE);
    }

    private static boolean isUpdateInProgress(final AppUpdateInfo appUpdateInfo) {
        return appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS;
    }
}
