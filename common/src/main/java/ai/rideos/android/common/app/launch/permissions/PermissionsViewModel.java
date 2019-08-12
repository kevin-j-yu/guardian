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
package ai.rideos.android.common.app.launch.permissions;

import io.reactivex.Observable;
import java.util.List;

public interface PermissionsViewModel {
    /**
     * Mark all notifications from `getPermissionsToCheck` as enabled or disabled.
     */
    void permissionsEnabled(final boolean areEnabled);

    /**
     * Return a list of device permissions to check that they are enabled.
     */
    Observable<List<String>> getPermissionsToCheck();
}
