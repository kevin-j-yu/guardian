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
package ai.rideos.android.common.architecture;

import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.Serializable;

public abstract class ModalFragmentViewController<Args extends Serializable, Listener>
    extends BottomSheetDialogFragment
    implements ViewController<Args, Listener> {

    private Args args;
    private Listener listener;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        final Bundler<Args, Listener> bundler = getBundler();
        args = bundler.getArgs(bundle);
        listener = bundler.getListener(bundle);
    }

    protected Args getArgs() {
        return args;
    }

    protected Listener getListener() {
        return listener;
    }

    private Bundler<Args, Listener> getBundler() {
        return new Bundler<>(this, ListenerRegistry.get());
    }
}
