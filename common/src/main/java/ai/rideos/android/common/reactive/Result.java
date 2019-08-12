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

public class Result<T> {
    private enum ResultType {
        SUCCESS,
        FAILURE
    }

    private final T resultObject;
    private final ResultType resultType;
    private final Throwable throwable;

    private Result(final T resultObject, final ResultType resultType, final Throwable throwable) {
        this.resultObject = resultObject;
        this.resultType = resultType;
        this.throwable = throwable;
    }

    public static <T> Result<T> success(final T resultObject) {
        return new Result<>(resultObject, ResultType.SUCCESS, null);
    }

    public static <T> Result<T> failure(final Throwable throwable) {
        return new Result<>(null, ResultType.FAILURE, throwable);
    }

    public boolean isSuccess() {
        return resultType == ResultType.SUCCESS;
    }

    public boolean isFailure() {
        return resultType == ResultType.FAILURE;
    }

    public T get() {
        return resultObject;
    }

    public Throwable getError() {
        return throwable;
    }
}

