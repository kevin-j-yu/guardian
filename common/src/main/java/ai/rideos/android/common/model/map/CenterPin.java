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

public class CenterPin {
    private final boolean shouldShow;
    private final Integer drawablePin;

    private CenterPin(final boolean shouldShow, final Integer drawablePin) {
        this.shouldShow = shouldShow;
        this.drawablePin = drawablePin;
    }

    public static CenterPin hidden() {
        return new CenterPin(false, null);
    }

    public static CenterPin ofDrawable(final Integer drawablePin) {
        return new CenterPin(true, drawablePin);
    }

    public boolean shouldShow() {
        return shouldShow;
    }

    public Integer getDrawablePin() {
        return drawablePin;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof CenterPin)) {
            return false;
        }
        final CenterPin otherModel = (CenterPin) other;
        if (shouldShow != otherModel.shouldShow()) {
            return false;
        }
        if (shouldShow) {
            return drawablePin.equals(otherModel.getDrawablePin());
        }
        return true;
    }
}
