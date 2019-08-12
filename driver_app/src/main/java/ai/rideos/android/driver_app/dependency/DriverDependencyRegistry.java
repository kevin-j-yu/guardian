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
package ai.rideos.android.driver_app.dependency;

import ai.rideos.android.common.app.dependency.CommonDependencyRegistry;
import ai.rideos.android.common.app.dependency.MapDependencyFactory;

public class DriverDependencyRegistry {
    private static DriverDependencyFactory DRIVER_DEPENDENCY_FACTORY;
    private static MapDependencyFactory MAP_DEPENDENCY_FACTORY;

    public static void init(final MapDependencyFactory mapDependencyFactory) {
        init(new DefaultDriverDependencyFactory(), mapDependencyFactory);
    }

    public static void init(final DriverDependencyFactory driverDependencyFactory,
                            final MapDependencyFactory mapDependencyFactory) {
        DRIVER_DEPENDENCY_FACTORY = driverDependencyFactory;
        MAP_DEPENDENCY_FACTORY = mapDependencyFactory;
        CommonDependencyRegistry.init(driverDependencyFactory, mapDependencyFactory);
    }

    public static DriverDependencyFactory driverDependencyFactory() {
        if (DRIVER_DEPENDENCY_FACTORY == null) {
            throw new RuntimeException(
                "Driver dependency factory not set. Call DriverDependencyRegistry.init() before accessing " +
                    "driverDependencyFactory()"
            );
        }
        return DRIVER_DEPENDENCY_FACTORY;
    }

    public static MapDependencyFactory mapDependencyFactory() {
        if (MAP_DEPENDENCY_FACTORY == null) {
            throw new RuntimeException(
                "Map dependency factory not set. Call DriverDependencyRegistry.init() before accessing " +
                    "mapDependencyFactory()"
            );
        }
        return MAP_DEPENDENCY_FACTORY;
    }
}
