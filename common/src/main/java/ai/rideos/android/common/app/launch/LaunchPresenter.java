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
package ai.rideos.android.common.app.launch;

import ai.rideos.android.common.app.launch.permissions.PermissionRequestor;
import ai.rideos.android.common.view.ParentPresenter;
import ai.rideos.android.common.view.Presenter;
import android.content.Context;
import android.view.ViewGroup;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;

public class LaunchPresenter extends ParentPresenter<ViewGroup> implements PermissionRequestor {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final Context context;
    private final List<LaunchStep> launchSteps;
    private final LaunchViewModel launchViewModel;

    public LaunchPresenter(final Context context, final List<LaunchStep> launchSteps, final Runnable launchDone) {
        this.context = context;
        this.launchSteps = launchSteps;
        launchViewModel = new DefaultLaunchViewModel(launchDone, launchSteps.size());
    }

    @Override
    public void attach(final ViewGroup container) {
        compositeDisposable.add(
            launchViewModel.getLaunchStepToDisplay().observeOn(AndroidSchedulers.mainThread())
                .subscribe(step -> {
                    final Presenter<ViewGroup> viewControllerToDisplay = launchSteps.get(step)
                        .buildStep(context, () -> launchViewModel.finishLaunchStep(step));
                    replaceChildAndAttach(viewControllerToDisplay, container);
                })
        );
    }

    @Override
    public void detach() {
        super.detach();
        compositeDisposable.dispose();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        final Presenter child = getChild();
        if (child instanceof PermissionRequestor) {
            ((PermissionRequestor) child).onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
