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
package ai.rideos.android.driver_app;

import ai.rideos.android.common.app.map.MapInsetFragmentListener;
import ai.rideos.android.common.architecture.EmptyArg;
import ai.rideos.android.common.architecture.FragmentNavigationController;
import ai.rideos.android.common.architecture.ListenerRegistry;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {
    private MainCoordinator mainCoordinator;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainCoordinator = new MainCoordinator(
            getContext(),
            new FragmentNavigationController(getChildFragmentManager(), R.id.overlay_fragment_container, ListenerRegistry.get())
        );
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.map_detail_fragment_container, container, false);
        getChildFragmentManager().beginTransaction()
            .replace(R.id.map_fragment_container, DriverDependencyRegistry.mapDependencyFactory().getMapFragment())
            .commit();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(new MapInsetFragmentListener(), true);
        mainCoordinator.start(EmptyArg.create());
    }

    public void onDestroyView() {
        super.onDestroyView();
        mainCoordinator.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainCoordinator.destroy();
    }
}
