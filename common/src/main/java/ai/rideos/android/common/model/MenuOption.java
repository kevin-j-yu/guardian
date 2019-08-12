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
package ai.rideos.android.common.model;

/**
 * MenuOption is a model representing a row in a menu. The row can have title text and a displayable icon. It also
 * needs to be tagged with some sort of ID to identify a row when it's selected.
 */
public class MenuOption {
    private final int id;
    private final String title;
    private final int drawableIcon;

    public MenuOption(final int id, final String title, final int drawableIcon) {
        this.id = id;
        this.title = title;
        this.drawableIcon = drawableIcon;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDrawableIcon() {
        return drawableIcon;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MenuOption)) {
            return false;
        }
        final MenuOption otherModel = (MenuOption) other;
        return id == otherModel.id
            && title.equals(otherModel.title)
            && drawableIcon == otherModel.drawableIcon;
    }
}
