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
package ai.rideos.android.common.view;

import android.view.View;

/**
 * A Presenter binds a view to updates. These updates can (and usually do) come from a ViewModel. Note that a presenter
 * does not own the view passed in and is not responsible for its lifecycle. For lifecycle-aware components, we should
 * use the FragmentViewController.
 */
public interface Presenter<V extends View> {
    void attach(final V view);

    void detach();
}
