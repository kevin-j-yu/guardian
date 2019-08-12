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

import java.util.List;
import java.util.Optional;
import timber.log.Timber;

/**
 * SingleSelectOptions models the state of a dropdown menu, where one option can be chosen at a time.
 * It has a list of options with values and display names, and the index of the selected option (if it exists).
 * @param <T> Type of value stored in an option
 */
public class SingleSelectOptions<T> {
    private final static int NO_SELECTION_INDEX = -1;

    public static class Option<T> {
        private final String displayText;
        private final T value;

        public Option(final String displayText, final T value) {
            this.displayText = displayText;
            this.value = value;
        }

        public String getDisplayText() {
            return displayText;
        }

        public T getValue() {
            return value;
        }
    }

    private final List<Option<T>> options;
    private final int selectedOptionIndex;

    /**
     * Create a SingleSelectOptions with one option selected. This method will log an error if the index is out of range.
     */
    public static <T> SingleSelectOptions<T> withSelection(final List<Option<T>> options, final int selectedOptionIndex) {
        if (selectedOptionIndex < 0 || selectedOptionIndex >= options.size()) {
            Timber.e("Invalid selection index %d for options size %d", selectedOptionIndex, options.size());
            return SingleSelectOptions.withNoSelection(options);
        }
        return new SingleSelectOptions<>(options, selectedOptionIndex);
    }

    /**
     * Create a SingleSelectOptions with no options selected.
     */
    public static <T> SingleSelectOptions<T> withNoSelection(final List<Option<T>> options) {
        return new SingleSelectOptions<>(options, NO_SELECTION_INDEX);
    }

    private SingleSelectOptions(final List<Option<T>> options, final int selectedOptionIndex) {
        this.options = options;
        this.selectedOptionIndex = selectedOptionIndex;
    }

    public List<Option<T>> getOptions() {
        return options;
    }

    /**
     * Return the selected index, if it exists.
     */
    public Optional<Integer> getSelectionIndex() {
        if (selectedOptionIndex == NO_SELECTION_INDEX) {
            return Optional.empty();
        }
        return Optional.of(selectedOptionIndex);
    }
}
