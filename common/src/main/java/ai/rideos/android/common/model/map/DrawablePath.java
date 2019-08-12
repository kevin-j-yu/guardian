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
package ai.rideos.android.common.model.map;

import ai.rideos.android.common.model.LatLng;
import java.util.List;

public class DrawablePath {
    public enum Style {
        SOLID,
        DOTTED
    }

    private final List<LatLng> coordinates;
    private final float width;
    private final int color;
    private final Style style;

    public DrawablePath(final List<LatLng> coordinates, final float width, final int color) {
        this(coordinates, width, color, Style.SOLID);
    }

    public DrawablePath(final List<LatLng> coordinates, final float width, final int color, final Style style) {
        this.coordinates = coordinates;
        this.width = width;
        this.color = color;
        this.style = style;
    }

    public List<LatLng> getCoordinates() {
        return coordinates;
    }

    public float getWidth() {
        return width;
    }

    public int getColor() {
        return color;
    }

    public Style getStyle() {
        return style;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DrawablePath)) {
            return false;
        }
        final DrawablePath otherModel = (DrawablePath) other;
        return coordinates.equals(otherModel.getCoordinates())
            && width == otherModel.getWidth()
            && color == otherModel.getColor()
            && style == otherModel.getStyle();
    }
}
