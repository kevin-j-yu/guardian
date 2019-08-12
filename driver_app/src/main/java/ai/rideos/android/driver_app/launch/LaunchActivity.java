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
package ai.rideos.android.driver_app.launch;

import ai.rideos.android.common.R;
import ai.rideos.android.common.app.launch.LaunchPresenter;
import ai.rideos.android.common.app.launch.login.LoginPresenter;
import ai.rideos.android.common.app.launch.permissions.PermissionsPresenter;
import ai.rideos.android.driver_app.MainFragmentActivity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;

public class LaunchActivity extends AppCompatActivity {
    private LaunchPresenter launchViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        launchViewController = new LaunchPresenter(
            this,
            Arrays.asList(
                LoginPresenter::new,
                PermissionsPresenter::new
            ),
            this::doneLaunching
        );
        launchViewController.attach(findViewById(android.R.id.content));
    }

    private void doneLaunching() {
        Intent intent = new Intent(this, MainFragmentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        launchViewController.detach();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        launchViewController.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
