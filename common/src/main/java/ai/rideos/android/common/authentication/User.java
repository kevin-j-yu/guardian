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
package ai.rideos.android.common.authentication;

import static ai.rideos.android.common.user_storage.SharedPreferencesFileNames.USER_AUTH_FILE;

import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import android.content.Context;
import android.content.SharedPreferences;
import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.storage.CredentialsManager;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.jwt.JWT;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import io.reactivex.Single;

/**
 * User defines the app's current user. Currently the user can only be authenticated through Auth0 so this class
 * is not generalized with an interface.
 */
public class User {
    // Passenger ID is updated with JWT. It shouldn't be updated manually
    private static final String USER_ID_KEY = "user_id";

    private final AuthenticationAPIClient apiClient;
    private final CredentialsManager credentialsManager;
    private final SharedPreferences sharedPreferences;
    private final SchedulerProvider schedulerProvider;

    private User(final Context context, final SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
        final Auth0 auth0 = new Auth0(context);
        auth0.setOIDCConformant(true);
        apiClient = new AuthenticationAPIClient(auth0);
        // Shared preferences are used here to store login information (the user id)
        sharedPreferences = context.getSharedPreferences(USER_AUTH_FILE, Context.MODE_PRIVATE);
        credentialsManager = new CredentialsManager(apiClient, new SharedPreferencesStorage(context, USER_AUTH_FILE));
    }

    /**
     * Retrieve the current user for this context.
     */
    public static User get(final Context context) {
        return new User(context, new DefaultSchedulerProvider());
    }

    /**
     * Update the auth0 authentication credentials for the user.
     */
    public void updateCredentials(final Credentials credentials) {
        final JWT jwt = new JWT(credentials.getAccessToken());
        credentialsManager.saveCredentials(credentials);
        // Store passenger for immediate lookup
        sharedPreferences.edit().putString(USER_ID_KEY, jwt.getSubject()).apply();
    }

    /**
     * Check if the current user credentials are valid.
     */
    public boolean isLoggedIn() {
        return credentialsManager.hasValidCredentials();
    }

    /**
     * Get the user's id.
     */
    public String getId() {
        return sharedPreferences.getString(USER_ID_KEY, "");
    }

    /**
     * Fetch the user's token. This will refresh the current token if the old one has expired.
     */
    public Single<String> fetchUserToken() {
        return Single.<String>create(emitter ->
            credentialsManager.getCredentials(new BaseCallback<Credentials, CredentialsManagerException>() {
                @Override
                public void onSuccess(final Credentials payload) {
                    emitter.onSuccess(payload.getAccessToken());
                }

                @Override
                public void onFailure(final CredentialsManagerException error) {
                    emitter.onError(error);
                }
            })
        )
            .subscribeOn(schedulerProvider.io());
    }

    /**
     * Fetch the user's profile. This will refresh the current token if the old one has expired.
     */
    public Single<UserProfile> fetchUserProfile() {
        return fetchUserToken()
            .flatMap(token -> Single.<UserProfile>create(emitter -> {
                    try {
                        emitter.onSuccess(apiClient.userInfo(token).execute());
                    } catch (final Exception e) {
                        emitter.onError(e);
                    }
                })
                    .subscribeOn(schedulerProvider.io())
            );
    }

    /**
     * Clear the user's current credentials.
     */
    public void clearCredentials() {
        credentialsManager.clearCredentials();
        sharedPreferences.edit().clear().apply();
    }
}
