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
package ai.rideos.android.common.app.menu_navigator.developer_options;

import ai.rideos.android.common.R;
import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.fleets.FleetResolver;
import ai.rideos.android.common.fleets.ResolvedFleet;
import ai.rideos.android.common.interactors.FleetInteractor;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.SingleSelectOptions;
import ai.rideos.android.common.model.SingleSelectOptions.Option;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.user_storage.ApiEnvironment;
import ai.rideos.android.common.user_storage.StorageKeys;
import ai.rideos.android.common.user_storage.UserStorageReader;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.common.view.resources.ResourceProvider;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import timber.log.Timber;

/**
 * DefaultDeveloperOptionsViewModel has a unique usage of interactors, because the interactor calls may change based
 * on the API env. Instead of supplying a interactor directly, an interactor supplier is required. The currently used
 * interactor is then updated whenever the API environment changes.
 */
public class DefaultDeveloperOptionsViewModel implements DeveloperOptionsViewModel {
    private static final int FLEET_RETRY_COUNT = 3;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final BehaviorSubject<ApiEnvironment> currentEnv;
    private final FleetInteractor fleetInteractor;

    private final SchedulerProvider schedulerProvider;
    private final ResourceProvider resourceProvider;
    private final UserStorageReader userStorageReader;
    private final UserStorageWriter userStorageWriter;
    private final User user;
    private final ResolvedFleet resolvedFleet;

    public DefaultDeveloperOptionsViewModel(final FleetInteractor fleetInteractor,
                                            final UserStorageReader userStorageReader,
                                            final UserStorageWriter userStorageWriter,
                                            final ResourceProvider resourceProvider,
                                            final User user,
                                            final ResolvedFleet resolvedFleet) {
        this(
            fleetInteractor,
            userStorageReader,
            userStorageWriter,
            resourceProvider,
            user,
            resolvedFleet,
            new DefaultSchedulerProvider()
        );
    }

    public DefaultDeveloperOptionsViewModel(final FleetInteractor fleetInteractor,
                                            final UserStorageReader userStorageReader,
                                            final UserStorageWriter userStorageWriter,
                                            final ResourceProvider resourceProvider,
                                            final User user,
                                            final ResolvedFleet resolvedFleet,
                                            final SchedulerProvider schedulerProvider) {
        this.userStorageReader = userStorageReader;
        this.userStorageWriter = userStorageWriter;
        this.resourceProvider = resourceProvider;
        this.schedulerProvider = schedulerProvider;
        this.user = user;
        this.resolvedFleet = resolvedFleet;
        this.fleetInteractor = fleetInteractor;
        currentEnv = BehaviorSubject.createDefault(getPreferredEnvironment());
    }

    @Override
    public void selectFleetId(final Option<String> fleetId) {
        userStorageWriter.storeStringPreference(StorageKeys.FLEET_ID, fleetId.getValue());
    }

    @Override
    public void selectEnvironment(final Option<ApiEnvironment> environment) {
        userStorageWriter.storeStringPreference(StorageKeys.RIDEOS_API_ENV, environment.getValue().getStoredName());
        currentEnv.onNext(environment.getValue());
    }

    @Override
    public Observable<String> getResolvedFleetId() {
        return resolvedFleet.observeFleetInfo()
            .map(fleetInfo -> {
                if (fleetInfo.getId().isEmpty()) {
                    return resourceProvider.getString(R.string.default_fleet_id_option_display);
                }
                return fleetInfo.getId();
            });
    }

    @Override
    public Observable<String> getUserId() {
        return Observable.just(user.getId());
    }

    @Override
    public Observable<SingleSelectOptions<String>> getFleetOptions() {
        // Update the values for the fleet id options if the environment changes
        return fleetInteractor.getFleets()
            .doOnError(e -> Timber.e(e, "Failed to retrieve fleet options"))
            .retry(FLEET_RETRY_COUNT)
            .onErrorReturnItem(Collections.emptyList())
            .map(DefaultDeveloperOptionsViewModel::addAutomaticFleetOption)
            .map(availableFleets -> {
                // TODO use fleet resolver
                final OptionalInt fleetIndex = IntStream.range(0, availableFleets.size())
                    .filter(i -> availableFleets.get(i).getId().equals(getPreferredFleetId()))
                    .findFirst();
                if (fleetIndex.isPresent()) {
                    return SingleSelectOptions.withSelection(getFleetIdOptions(availableFleets), fleetIndex.getAsInt());
                }
                return SingleSelectOptions.withNoSelection(getFleetIdOptions(availableFleets));
            });
    }

    @Override
    public Observable<SingleSelectOptions<ApiEnvironment>> getEnvironmentOptions() {
        final List<ApiEnvironment> allowedEnvs = Arrays.asList(ApiEnvironment.values());

        final int envIndex = getPreferredEnvironment().ordinal();
        return Observable.just(SingleSelectOptions.withSelection(getEnvOptions(allowedEnvs), envIndex));
    }

    private static List<FleetInfo> addAutomaticFleetOption(final List<FleetInfo> availableFleetIds) {
        // Default fleet id is an empty string. Add it if it doesn't exist
        final List<FleetInfo> fleetListCopy = new ArrayList<>(availableFleetIds);
        fleetListCopy.add(0, new FleetInfo(FleetResolver.AUTOMATIC_FLEET_ID));
        return fleetListCopy;
    }

    private List<Option<String>> getFleetIdOptions(final List<FleetInfo> availableFleetIds) {
        return availableFleetIds.stream()
            .map(fleetInfo -> {
                if (fleetInfo.getId().isEmpty()) {
                    return new Option<>(resourceProvider.getString(R.string.default_fleet_id_option_display), fleetInfo.getId());
                }
                if (fleetInfo.getDisplayName().equals(fleetInfo.getId())) {
                    return new Option<>(fleetInfo.getDisplayName(), fleetInfo.getId());
                }
                return new Option<>(
                    fleetInfo.getDisplayName() + " - " + fleetInfo.getId(),
                    fleetInfo.getId()
                );
            })
            .collect(Collectors.toList());
    }

    private List<Option<ApiEnvironment>> getEnvOptions(final List<ApiEnvironment> allowedEnvs) {
        return allowedEnvs.stream()
            .map(env -> {
                switch (env) {
                    case DEVELOPMENT:
                        return new Option<>(resourceProvider.getString(R.string.development_env_option), env);
                    case STAGING:
                        return new Option<>(resourceProvider.getString(R.string.staging_env_option), env);
                    case PRODUCTION:
                        return new Option<>(resourceProvider.getString(R.string.production_env_option), env);
                    default:
                        Timber.e("Unknown API env %s", env.getEndpoint());
                        return new Option<>(env.getEndpoint(), env);
                }
            })
            .collect(Collectors.toList());
    }

    private String getPreferredFleetId() {
        return userStorageReader.getStringPreference(StorageKeys.FLEET_ID);
    }

    private ApiEnvironment getPreferredEnvironment() {
        return ApiEnvironment.fromStoredNameOrThrow(userStorageReader.getStringPreference(StorageKeys.RIDEOS_API_ENV));
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        fleetInteractor.destroy();
    }
}
