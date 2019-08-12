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
package ai.rideos.android.model;

import java.util.Objects;

public class MainViewState {
    public enum Step {
        START_SCREEN,
        PRE_TRIP,
        ON_TRIP,
    }

    private final Step step;
    private final String taskId;

    public MainViewState(final Step step, final String taskId) {
        this.step = step;
        this.taskId = taskId;
    }

    public Step getStep() {
        return step;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MainViewState)) {
            return false;
        }
        final MainViewState otherModel = (MainViewState) other;
        return Objects.equals(taskId, otherModel.taskId)
            && step == otherModel.step;
    }
}
