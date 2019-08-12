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
package ai.rideos.android.common.app.launch.login;

import ai.rideos.android.common.R;
import ai.rideos.android.common.app.launch.LaunchStep.LaunchStepListener;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.view.Presenter;
import android.content.Context;
import android.view.ViewGroup;
import com.auth0.android.Auth0;
import com.auth0.android.lock.AuthenticationCallback;
import com.auth0.android.lock.Lock;
import com.auth0.android.lock.LockCallback;
import com.auth0.android.lock.utils.LockException;
import com.auth0.android.result.Credentials;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Arrays;

public class LoginPresenter implements Presenter<ViewGroup> {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Context context;
    private final LoginViewModel loginViewModel;
    private final Lock lockScreen;

    public LoginPresenter(final Context context, final LaunchStepListener listener) {
        this.context = context;
        loginViewModel = new DefaultLoginViewModel(User.get(context), listener);

        final Auth0 account = new Auth0(context);
        account.setOIDCConformant(true);
        lockScreen = Lock.newBuilder(account, getLockCallback())
            .allowedConnections(Arrays.asList(context.getString(R.string.com_auth0_connections).split(" ")))
            .withAudience(context.getString(R.string.com_auth0_audience))
            .withScheme(context.getString(R.string.com_auth0_scheme))
            .withScope(context.getString(R.string.com_auth0_scope))
            .build(context);
    }

    @Override
    public void attach(final ViewGroup view) {
        compositeDisposable.add(
            loginViewModel.shouldLaunchLogin().observeOn(AndroidSchedulers.mainThread())
                .subscribe(notification -> context.startActivity(lockScreen.newIntent(context)))
        );
    }

    private LockCallback getLockCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onAuthentication(final Credentials credentials) {
                loginViewModel.loginComplete(credentials);
            }

            @Override
            public void onCanceled() {
            }

            @Override
            public void onError(final LockException error) {
                loginViewModel.loginFailure(error);
            }
        };
    }

    @Override
    public void detach() {
        compositeDisposable.dispose();
        lockScreen.onDestroy(context);
    }
}
