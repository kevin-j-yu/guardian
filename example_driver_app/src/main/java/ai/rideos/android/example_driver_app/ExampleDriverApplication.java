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
package ai.rideos.android.example_driver_app;

import ai.rideos.android.common.app.BaseApplication;
import ai.rideos.android.common.app.MetadataReader;
import ai.rideos.android.driver_app.dependency.DriverDependencyRegistry;
import ai.rideos.android.google.GoogleMetadataKeys;
import ai.rideos.android.google.dependency.GoogleMapDependencyFactory;

public class ExampleDriverApplication extends BaseApplication {
    @Override
    protected void registerDependencies() {
        final String gmsKey = new MetadataReader(getApplicationContext())
            .getStringMetadata(GoogleMetadataKeys.GMS_API_KEY)
            .getOrThrow();
        DriverDependencyRegistry.init(new GoogleMapDependencyFactory(getApplicationContext(), gmsKey));
    }
}
