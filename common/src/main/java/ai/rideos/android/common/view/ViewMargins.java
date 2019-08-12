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

/**
 * ViewMargins describes the left, top, right, and bottom margins for any view.
 */
public class ViewMargins {
    private final int leftMargin;
    private final int topMargin;
    private final int rightMargin;
    private final int bottomMargin;

    private ViewMargins(final int leftMargin, final int topMargin, final int rightMargin, final int bottomMargin) {
        this.leftMargin = leftMargin;
        this.topMargin = topMargin;
        this.rightMargin = rightMargin;
        this.bottomMargin = bottomMargin;
    }

    public int getLeft() {
        return leftMargin;
    }

    public int getTop() {
        return topMargin;
    }

    public int getRight() {
        return rightMargin;
    }

    public int getBottom() {
        return bottomMargin;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private int leftMargin = 0;
        private int topMargin = 0;
        private int rightMargin = 0;
        private int bottomMargin = 0;

        public Builder setLeft(final int leftMargin) {
            this.leftMargin = leftMargin;
            return this;
        }

        public Builder setTop(final int topMargin) {
            this.topMargin = topMargin;
            return this;
        }

        public Builder setRight(final int rightMargin) {
            this.rightMargin = rightMargin;
            return this;
        }

        public Builder setBottom(final int bottomMargin) {
            this.bottomMargin = bottomMargin;
            return this;
        }

        public ViewMargins build() {
            return new ViewMargins(leftMargin, topMargin, rightMargin, bottomMargin);
        }
    }
}
