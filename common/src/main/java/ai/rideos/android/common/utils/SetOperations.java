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
package ai.rideos.android.common.utils;

import java.util.HashSet;
import java.util.Set;

public final class SetOperations {
    public static class DiffResult<T> {
        private final Set<T> onlyLeft;
        private final Set<T> intersect;
        private final Set<T> onlyRight;

        DiffResult(final Set<T> onlyLeft, final Set<T> intersect, final Set<T> onlyRight) {
            this.onlyLeft = onlyLeft;
            this.intersect = intersect;
            this.onlyRight = onlyRight;
        }

        public Set<T> getOnlyOnLeft() {
            return onlyLeft;
        }

        public Set<T> getIntersecting() {
            return intersect;
        }

        public Set<T> getOnlyOnRight() {
            return onlyRight;
        }
    }

    /**
     * For two sets, Left and Right, getDifferences computes the objects only in Left, the objects shared by both
     * Left and Right, and the objects only in Right.
     * @param left - First Set
     * @param right - Second Set
     * @param <T> - Type of objects in set
     * @return Objects only on Left, objects shared by both, and objects only on Right
     */
    public static <T> DiffResult<T> getDifferences(final Set<T> left, final Set<T> right) {
        final Set<T> onlyOnLeft = new HashSet<>(left);
        onlyOnLeft.removeAll(right);

        final Set<T> onlyOnRight = new HashSet<>(right);
        onlyOnRight.removeAll(left);

        final Set<T> intersect = new HashSet<>(left);
        intersect.retainAll(right);
        return new DiffResult<>(onlyOnLeft, intersect, onlyOnRight);
    }
}
