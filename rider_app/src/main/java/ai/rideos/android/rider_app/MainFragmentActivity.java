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
package ai.rideos.android.rider_app;

import ai.rideos.android.common.app.AppUpdateManagerLifecycleListener;
import ai.rideos.android.common.app.menu_navigator.LoggedOutListener;
import ai.rideos.android.common.app.menu_navigator.MenuOptionFragmentRegistry;
import ai.rideos.android.common.app.menu_navigator.OpenMenuListener;
import ai.rideos.android.common.app.menu_navigator.menu.MenuNavigator;
import ai.rideos.android.common.app.menu_navigator.menu.MenuPresenter;
import ai.rideos.android.common.connectivity.ConnectivityMonitor;
import ai.rideos.android.common.device.FusedLocationDeviceLocator;
import ai.rideos.android.common.fleets.DefaultFleetResolver;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.user_storage.SharedPreferencesUserStorageReader;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.viewmodel.BackListener;
import ai.rideos.android.rider_app.dependency.RiderDependencyRegistry;
import ai.rideos.android.rider_app.launch.LaunchActivity;
import android.content.Intent;
import android.os.Bundle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class MainFragmentActivity
    extends FragmentActivity
    implements BackListener, OpenMenuListener, LoggedOutListener {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private MenuNavigator menuNavigator;
    private MenuPresenter menuController;
    private ConnectivityMonitor connectivityMonitor;
    private AppUpdateManagerLifecycleListener appUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drawer_layout);
        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        final MenuOptionFragmentRegistry registry = RiderDependencyRegistry.riderDependencyFactory()
            .getMenuOptions(this);
        menuController = new MenuPresenter(this, registry.getMenuOptions(), this);
        menuController.attach(drawerLayout);

        menuNavigator = new MenuNavigator(registry, menuController, getSupportFragmentManager(), R.id.content_frame);

        connectivityMonitor = new ConnectivityMonitor(findViewById(R.id.connectivity_banner));

        appUpdateManager = new AppUpdateManagerLifecycleListener();
        appUpdateManager.onActivityCreated(this);

        resolveFleetId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager.onActivityResumed(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        appUpdateManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
        menuController.detach();
    }

    @Override
    public void onStart() {
        super.onStart();
        connectivityMonitor.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        connectivityMonitor.stop();
    }

    /**
     * Propagate phone's back button signal down to child view controllers. If the child view controllers cannot handle
     * the signal (i.e. they're at the first state in a workflow), it is propagated back up through BackListeners
     */
    @Override
    public void onBackPressed() {
        if (menuController.isMenuOpen()) {
            menuController.closeMenu();
            return;
        }
        menuNavigator.propagateBackSignal(this::back);
    }

    private void resolveFleetId() {
        final DefaultFleetResolver fleetResolver = new DefaultFleetResolver(
            RiderDependencyRegistry.riderDependencyFactory().getFleetInteractor(this),
            new FusedLocationDeviceLocator(this)
        );

        final Disposable subscription = fleetResolver.resolveFleet(
            SharedPreferencesUserStorageReader.forContext(this)
                .observeStringPreference(StorageKeys.FLEET_ID)
        )
            .doOnDispose(fleetResolver::shutDown)
            .subscribe(fleet -> ResolvedFleet.get().setFleetInfo(fleet));
        compositeDisposable.add(subscription);
    }

    @Override
    public void back() {
        super.onBackPressed();
    }

    @Override
    public void openMenu() {
        menuController.openMenu();
    }

    @Override
    public void loggedOut() {
        Intent intent = new Intent(this, LaunchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
