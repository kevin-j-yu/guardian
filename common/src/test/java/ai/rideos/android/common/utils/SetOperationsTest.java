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

import static org.junit.Assert.assertEquals;

import ai.rideos.android.common.utils.SetOperations.DiffResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;

public class SetOperationsTest {
    @Test
    public void testDiffBothSetsEmpty() {
        final DiffResult<String> result = SetOperations.getDifferences(
            new HashSet<>(),
            new HashSet<>()
        );
        assertEquals(0, result.getOnlyOnLeft().size());
        assertEquals(0, result.getOnlyOnRight().size());
        assertEquals(0, result.getIntersecting().size());
    }

    @Test
    public void testLeftEmpty() {
        final DiffResult<String> result = SetOperations.getDifferences(
            new HashSet<>(),
            new HashSet<>(Arrays.asList("a", "b", "c"))
        );
        assertEquals(0, result.getOnlyOnLeft().size());
        assertEquals(3, result.getOnlyOnRight().size());
        assertEquals(0, result.getIntersecting().size());
    }

    @Test
    public void testRightEmpty() {
        final DiffResult<String> result = SetOperations.getDifferences(
            new HashSet<>(Arrays.asList("a", "b", "c")),
            new HashSet<>()
        );
        assertEquals(3, result.getOnlyOnLeft().size());
        assertEquals(0, result.getOnlyOnRight().size());
        assertEquals(0, result.getIntersecting().size());
    }

    @Test
    public void testNoIntersection() {
        final HashSet<String> left = new HashSet<>(Arrays.asList("a", "b", "c"));
        final HashSet<String> right = new HashSet<>(Arrays.asList("d", "e", "f"));
        final DiffResult<String> result = SetOperations.getDifferences(left, right);
        assertEquals(left, result.getOnlyOnLeft());
        assertEquals(right, result.getOnlyOnRight());
        assertEquals(0, result.getIntersecting().size());
    }

    @Test
    public void testIntersecting() {
        final HashSet<String> left = new HashSet<>(Arrays.asList("a", "b", "c"));
        final HashSet<String> right = new HashSet<>(Arrays.asList("b", "c", "d"));
        final DiffResult<String> result = SetOperations.getDifferences(left, right);
        assertEquals(new HashSet<>(Collections.singletonList("a")), result.getOnlyOnLeft());
        assertEquals(new HashSet<>(Collections.singletonList("d")), result.getOnlyOnRight());
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), result.getIntersecting());
    }

    @Test
    public void testOnlyIntersecting() {
        final HashSet<String> both = new HashSet<>(Arrays.asList("a", "b", "c"));
        final DiffResult<String> result = SetOperations.getDifferences(both, both);
        assertEquals(both, result.getIntersecting());
        assertEquals(0, result.getOnlyOnLeft().size());
        assertEquals(0, result.getOnlyOnRight().size());
    }
}
