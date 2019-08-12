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
package ai.rideos.android.interactors;

import ai.rideos.android.common.model.LocationAutocompleteResult;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.user_storage.StorageKey;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserStorageHistoricalSearchInteractor implements HistoricalSearchInteractor {
    public interface HistoricalOptionsSerializer {
        String serialize(final List<LocationAutocompleteResult> options);

        List<LocationAutocompleteResult> deserialize(final String serializedOptions) throws DeserializeException;
    }

    public static class DeserializeException extends Exception {
        DeserializeException(final Exception e) {
            super(e);
        }
    }

    private static final int DEFAULT_MAX_OPTIONS = 5;
    private static final StorageKey<String> STORAGE_KEY = new StorageKey<>("historical_searches", "");
    private final UserStorageReader userStorageReader;
    private final UserStorageWriter userStorageWriter;
    private final HistoricalOptionsSerializer serializer;
    private final SchedulerProvider schedulerProvider;
    private final int maxOptions;

    public UserStorageHistoricalSearchInteractor(final UserStorageReader userStorageReader,
                                                 final UserStorageWriter userStorageWriter) {
        this(
            userStorageReader,
            userStorageWriter,
            new GsonSerializer(),
            DEFAULT_MAX_OPTIONS,
            new DefaultSchedulerProvider()
        );
    }

    public UserStorageHistoricalSearchInteractor(final UserStorageReader userStorageReader,
                                                 final UserStorageWriter userStorageWriter,
                                                 final HistoricalOptionsSerializer serializer,
                                                 final int maxOptions,
                                                 final SchedulerProvider schedulerProvider) {
        this.userStorageReader = userStorageReader;
        this.userStorageWriter = userStorageWriter;
        this.serializer = serializer;
        this.maxOptions = maxOptions;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<List<LocationAutocompleteResult>> getHistoricalSearchOptions() {
        return Observable.fromCallable(this::getResultsFromUserStorage)
            // This is reading from storage, potentially blocking
            .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Completable storeSearchedOption(final LocationAutocompleteResult searchOption) {
        return Completable.fromRunnable(() -> storeSearchOptionAndTrim(searchOption))
            .subscribeOn(schedulerProvider.io());
    }

    private List<LocationAutocompleteResult> getResultsFromUserStorage() {
        final String storedSearchString = userStorageReader.getStringPreference(STORAGE_KEY);
        try {
            return serializer.deserialize(storedSearchString);
        } catch (final DeserializeException e) {
            userStorageWriter.storeStringPreference(STORAGE_KEY, "");
            return Collections.emptyList();
        }
    }

    private void storeSearchOptionAndTrim(final LocationAutocompleteResult newOption) {
        final List<LocationAutocompleteResult> storedOptions = new ArrayList<>(getResultsFromUserStorage());
        // Remove existing option if it exists, so that we can reorder the latest search
        storedOptions.remove(newOption);
        // Add option to front
        storedOptions.add(0, newOption);
        if (storedOptions.size() > maxOptions) {
            // Trim end of list
            storedOptions.subList(maxOptions, storedOptions.size()).clear();
        }
        final String serializedOptions = serializer.serialize(storedOptions);
        userStorageWriter.storeStringPreference(STORAGE_KEY, serializedOptions);
    }

    public static class GsonSerializer implements HistoricalOptionsSerializer {
        @Override
        public String serialize(final List<LocationAutocompleteResult> options) {
            final Type listType = new TypeToken<List<LocationAutocompleteResult>>() {}.getType();
            return new Gson().toJson(options, listType);
        }

        @Override
        public List<LocationAutocompleteResult> deserialize(final String serializedOptions) throws DeserializeException {
            if (serializedOptions.isEmpty()) {
                return Collections.emptyList();
            }
            final Type listType = new TypeToken<List<LocationAutocompleteResult>>() {}.getType();
            try {
                return new Gson().fromJson(serializedOptions, listType);
            } catch (final JsonSyntaxException e) {
                throw new DeserializeException(e);
            }
        }
    }
}
