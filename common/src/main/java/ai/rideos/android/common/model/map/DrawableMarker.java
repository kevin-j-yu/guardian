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

public class DrawableMarker {
    public enum Anchor {
        CENTER,
        BOTTOM
    }

    private final LatLng position;
    private final float rotation;
    private final int drawableIcon;
    private final Anchor anchor;

    public DrawableMarker(final LatLng position, final float rotation, final int drawableIcon, final Anchor anchor) {
        this.position = position;
        this.rotation = rotation;
        this.drawableIcon = drawableIcon;
        this.anchor = anchor;
    }

    public LatLng getPosition() {
        return position;
    }

    public float getRotation() {
        return rotation;
    }

    public int getDrawableIcon() {
        return drawableIcon;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DrawableMarker)) {
            return false;
        }
        final DrawableMarker otherModel = (DrawableMarker) other;
        return position.equals(otherModel.getPosition())
            && rotation == otherModel.getRotation()
            && drawableIcon == otherModel.getDrawableIcon()
            && anchor == otherModel.getAnchor();
    }
}
