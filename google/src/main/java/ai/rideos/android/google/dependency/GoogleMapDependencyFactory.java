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
package ai.rideos.android.google.dependency;

import ai.rideos.android.common.app.dependency.MapDependencyFactory;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.interactors.LocationAutocompleteInteractor;
import ai.rideos.android.google.interactors.AndroidGeocodeInteractor;
import ai.rideos.android.google.interactors.GooglePlacesAutocompleteInteractor;
import ai.rideos.android.google.map.GoogleMapFragment;
import android.content.Context;
import androidx.fragment.app.Fragment;
import com.google.android.libraries.places.api.Places;

public class GoogleMapDependencyFactory implements MapDependencyFactory {
    public GoogleMapDependencyFactory(final Context applicationContext, final String gmsKey) {
        Places.initialize(applicationContext, gmsKey);
    }

    @Override
    public Fragment getMapFragment() {
        return new GoogleMapFragment();
    }

    @Override
    public LocationAutocompleteInteractor getAutocompleteInteractor(final Context context) {
        return new GooglePlacesAutocompleteInteractor(context);
    }

    @Override
    public GeocodeInteractor getGeocodeInteractor(final Context context) {
        return new AndroidGeocodeInteractor(context);
    }
}
