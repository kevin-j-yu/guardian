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
package ai.rideos.android.driver_app.vehicle_settings;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.reactive.RetryBehaviors;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.interactors.DriverVehicleInteractor;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class DefaultVehicleSettingsViewModel implements VehicleSettingsViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final BehaviorSubject<String> editedLicenseSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> savedLicenseSubject = BehaviorSubject.create();

    private final User user;
    private final DriverVehicleInteractor driverVehicleInteractor;
    private final SchedulerProvider schedulerProvider;

    public DefaultVehicleSettingsViewModel(final User user,
                                           final DriverVehicleInteractor driverVehicleInteractor) {
        this(user, driverVehicleInteractor, new DefaultSchedulerProvider());
    }

    public DefaultVehicleSettingsViewModel(final User user,
                                           final DriverVehicleInteractor driverVehicleInteractor,
                                           final SchedulerProvider schedulerProvider) {
        this.user = user;
        this.driverVehicleInteractor = driverVehicleInteractor;
        this.schedulerProvider = schedulerProvider;

        compositeDisposable.add(
            driverVehicleInteractor.getVehicleInfo(user.getId())
                .retry(RetryBehaviors.DEFAULT_RETRY_COUNT)
                .subscribe(
                    profile -> savedLicenseSubject.onNext(profile.getLicensePlate()),
                    error -> Timber.e(error, "Couldn't retrieve vehicle info")
                )
        );
    }

    @Override
    public void save() {
        final String license = editedLicenseSubject.getValue();
        if (license != null) {
            compositeDisposable.add(
                driverVehicleInteractor.updateLicensePlate(user.getId(), license)
                    .retry(RetryBehaviors.DEFAULT_RETRY_COUNT)
                    .subscribe(
                        () -> savedLicenseSubject.onNext(license),
                        error -> Timber.e(error, "Couldn't save vehicle info")
                    )
            );
        }
    }

    @Override
    public void editLicensePlate(final String licensePlate) {
        editedLicenseSubject.onNext(licensePlate);
    }

    @Override
    public Observable<String> getLicensePlate() {
        return savedLicenseSubject;
    }

    @Override
    public Observable<Boolean> isSavingEnabled() {
        return Observable.combineLatest(
            editedLicenseSubject,
            savedLicenseSubject,
            (edited, saved) -> !edited.equals(saved)
        )
            .observeOn(schedulerProvider.computation())
            .startWith(false)
            .distinctUntilChanged();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        driverVehicleInteractor.shutDown();
    }
}
