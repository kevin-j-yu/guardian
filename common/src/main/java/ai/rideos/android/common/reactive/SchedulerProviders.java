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

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;

public class SchedulerProviders {
    /**
     * Default threads - to be used in production environments.
     */
    public static class DefaultSchedulerProvider implements SchedulerProvider {
        @Override
        public Scheduler io() {
            return Schedulers.io();
        }

        @Override
        public Scheduler computation() {
            return Schedulers.computation();
        }

        @Override
        public Scheduler mainThread() {
            return AndroidSchedulers.mainThread();
        }

        @Override
        public Scheduler single() {
            return Schedulers.single();
        }
    }

    /**
     * Trampoline runs all observables in a single thread, so everything is synchronous.
     */
    public static class TrampolineSchedulerProvider implements SchedulerProvider {
        @Override
        public Scheduler io() {
            return Schedulers.trampoline();
        }

        @Override
        public Scheduler computation() {
            return Schedulers.trampoline();
        }

        @Override
        public Scheduler mainThread() {
            return Schedulers.trampoline();
        }

        @Override
        public Scheduler single() {
            return Schedulers.trampoline();
        }
    }

    public static class TestSchedulerProvider implements SchedulerProvider {
        private final TestScheduler testScheduler;

        public TestSchedulerProvider(final TestScheduler testScheduler) {
            this.testScheduler = testScheduler;
        }

        @Override
        public Scheduler io() {
            return testScheduler;
        }

        @Override
        public Scheduler computation() {
            return testScheduler;
        }

        @Override
        public Scheduler mainThread() {
            return testScheduler;
        }

        @Override
        public Scheduler single() {
            return testScheduler;
        }
    }
}
