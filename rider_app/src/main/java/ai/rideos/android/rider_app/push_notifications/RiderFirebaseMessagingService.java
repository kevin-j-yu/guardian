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
package ai.rideos.android.rider_app.push_notifications;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.rider_app.MainFragmentActivity;
import ai.rideos.android.rider_app.R;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import timber.log.Timber;

public class RiderFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        final User user = User.get(getApplicationContext());
        final String userId = user.getId();

        if (userId.isEmpty()) {
            // User is logged out. Wait for the app to start up and log in
            return;
        }

        RiderDependencyRegistry.riderDependencyFactory().getDeviceRegistryInteractor(this)
            .registerRiderDevice(userId, token);
        Timber.i("Successfully updated device for user %s and device %s", userId, token);
    }

    /**
     * Messages are received here when the app is in the foreground and a push notification is sent. In this case,
     * we need to create a notification to show over the current activity.
     */
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        final Intent intent = new Intent(this, MainFragmentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        final String channelId = getString(R.string.trip_alert_channel_id);

        final NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(remoteMessage.getNotification().getTitle());
        bigTextStyle.bigText(remoteMessage.getNotification().getBody());

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.car) // TODO consider a specific icon for foreground alerts
            .setContentTitle(remoteMessage.getNotification().getTitle())
            .setContentText(remoteMessage.getNotification().getBody())
            .setStyle(bigTextStyle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(0, builder.build());
    }
}
