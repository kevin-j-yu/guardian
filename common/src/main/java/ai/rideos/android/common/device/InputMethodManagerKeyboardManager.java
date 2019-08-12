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
package ai.rideos.android.common.device;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class InputMethodManagerKeyboardManager implements KeyboardManager {
    private final InputMethodManager methodManager;
    private final View viewInFocus;

    /**
     * Create a keyboard manager controlled by InputMethodManager.
     * @param context - Application context
     * @param viewInFocus - The view currently receiving input. This is important for showing the keyboard because
     *                    it must be in focus. Hiding the keyboard can take in any view.
     */
    public InputMethodManagerKeyboardManager(final Context context, final View viewInFocus) {
        methodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        this.viewInFocus = viewInFocus;
    }

    @Override
    public void showKeyboard() {
        methodManager.showSoftInput(viewInFocus, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void hideKeyboard() {
        methodManager.hideSoftInputFromWindow(viewInFocus.getWindowToken(), 0);
    }
}
