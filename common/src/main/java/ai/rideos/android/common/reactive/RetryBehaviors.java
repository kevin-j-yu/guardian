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

import androidx.core.util.Pair;
import io.reactivex.Observable;

public class RetryBehaviors {
    public static final int DEFAULT_RETRY_COUNT = 3;

    private RetryBehaviors() {
    }

    // By default, retry
    public static RetryBehavior getDefault() {
        return retryAtMost(DEFAULT_RETRY_COUNT);
    }

    public static RetryBehavior retryAtMost(final int numberOfRetries) {
        // The function required for the retryWhen operator takes in an observable error stream and returns another
        // observable. If the returned observable emits a value, the retryWhen operator re-subscribes and tries again.
        // If the returned observable emits an error, the retryWhen operator stops.
        // So, this function zips the errors with the values [1...retries+1]. When the emitted value <= retries, a
        // value is returned. On the last emitted value (value == retries + 1), an error is returned.
        return observableErrors -> observableErrors
            .zipWith(Observable.range(1, numberOfRetries + 1), Pair::create)
            .flatMap(errorAndRetryNum -> {
                if (errorAndRetryNum.second > numberOfRetries) {
                    return Observable.<Integer>error(errorAndRetryNum.first);
                }
                return Observable.just(errorAndRetryNum.second);
            });
    }

    public static RetryBehavior neverRetry() {
        return retryAtMost(0);
    }
}
