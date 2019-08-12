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
package ai.rideos.android.common.reactive;

import static junit.framework.TestCase.assertEquals;

import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.BehaviorSubject;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class RetryBehaviorsTest {
    @Test
    public void testRetryAtMostEmitsErrorAfterLastRetry() {
        final int retryCount = 3;
        final AtomicInteger thrownErrors = new AtomicInteger(0);
        final AtomicInteger emittedErrors = new AtomicInteger(0);

        final BehaviorSubject<Object> subject = BehaviorSubject.create();
        final TestObserver<Object> testObserver = subject
            .map(o -> {
                throw new IOException();
            })
            .doOnError(o -> thrownErrors.incrementAndGet())
            .retryWhen(RetryBehaviors.retryAtMost(retryCount))
            .doOnError(o -> emittedErrors.incrementAndGet())
            .test();

        subject.onNext(true);

        assertEquals(thrownErrors.get(), retryCount + 1);
        assertEquals(emittedErrors.get(), 1);
    }

    @Test
    public void testNeverRetryEmitsErrorImmediately() {
        final AtomicInteger thrownErrors = new AtomicInteger(0);
        final AtomicInteger emittedErrors = new AtomicInteger(0);

        final BehaviorSubject<Object> subject = BehaviorSubject.create();
        final TestObserver<Object> testObserver = subject
            .map(o -> {
                throw new IOException();
            })
            .doOnError(o -> thrownErrors.incrementAndGet())
            .retryWhen(RetryBehaviors.neverRetry())
            .doOnError(o -> emittedErrors.incrementAndGet())
            .test();

        subject.onNext(true);

        assertEquals(thrownErrors.get(), 1);
        assertEquals(emittedErrors.get(), 1);
    }
}
