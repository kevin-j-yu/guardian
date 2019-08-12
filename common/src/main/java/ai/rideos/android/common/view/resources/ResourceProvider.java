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
package ai.rideos.android.common.view.resources;

import java.util.Locale;

public interface ResourceProvider {
    /**
     * Get the locale of a user from the app resources. This is useful for displaying strings.
     */
    Locale getLocale();

    /**
     * Retrieve a color in the SRGB space given a color attribute id.
     * See https://developer.android.com/reference/android/graphics/ColorSpace.Named#SRGB for more details.
     */
    int getColor(final int colorAttributeId);

    /**
     * Get a string formatted with arguments from app resources.
     */
    String getString(final int stringResourceId, Object... args);

    /**
     * Get a string that changes based on the quantity provided. For example to show the number of cars in an area,
     * for quantity 0 you might want to display "No cars", for quantity 1 "1 car" and for quantity 2 "2 cars".
     */
    String getQuantityString(final int pluralResourceId, int quantity, Object... args);

    /**
     * Get the id of a drawable asset from an attribute id.
     */
    int getDrawableId(final int attributeId);
}
