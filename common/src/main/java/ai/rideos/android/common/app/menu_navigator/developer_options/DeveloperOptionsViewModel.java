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
package ai.rideos.android.common.app.menu_navigator.developer_options;

import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.model.SingleSelectOptions.Option;
import ai.rideos.android.common.user_storage.ApiEnvironment;
import ai.rideos.android.common.viewmodel.ViewModel;
import io.reactivex.Observable;

public interface DeveloperOptionsViewModel extends ViewModel {
    void selectFleetId(final Option<String> fleetIdOption);

    void selectEnvironment(final Option<ApiEnvironment> environmentOption);

    Observable<String> getResolvedFleetId();

    Observable<String> getUserId();

    Observable<SingleSelectOptions<String>> getFleetOptions();

    Observable<SingleSelectOptions<ApiEnvironment>> getEnvironmentOptions();
}
