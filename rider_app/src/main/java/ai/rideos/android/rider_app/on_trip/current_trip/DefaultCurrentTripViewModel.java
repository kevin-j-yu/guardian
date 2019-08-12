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
package ai.rideos.android.rider_app.on_trip.current_trip;

import ai.rideos.android.common.authentication.User;
import ai.rideos.android.common.interactors.GeocodeInteractor;
import ai.rideos.android.common.model.FleetInfo;
import ai.rideos.android.common.model.LatLng;
import ai.rideos.android.common.model.NamedTaskLocation;
import ai.rideos.android.common.model.TaskLocation;
import ai.rideos.android.common.reactive.Result;
import ai.rideos.android.common.reactive.SchedulerProvider;
import ai.rideos.android.common.reactive.SchedulerProviders.DefaultSchedulerProvider;
import ai.rideos.android.common.user_storage.UserStorageWriter;
import ai.rideos.android.interactors.RiderTripStateInteractor;
import ai.rideos.android.model.FollowTripDisplayState;
import ai.rideos.android.model.NamedPickupDropOff;
import ai.rideos.android.model.TripStateModel;
import ai.rideos.android.settings.RiderStorageKeys;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import com.google.gson.Gson;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class DefaultCurrentTripViewModel implements CurrentTripViewModel {
    private static final int DEFAULT_POLL_INTERVAL_MILLI = 2000;
    private static final int GEOCODE_RETRY_COUNT = 2;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final BehaviorSubject<String> tripIdSubject = BehaviorSubject.create();
    private final Observable<TripStateModel> observablePassengerState;
    private final BehaviorSubject<NamedTaskLocation> pickup = BehaviorSubject.create();
    private final BehaviorSubject<NamedTaskLocation> dropOff = BehaviorSubject.create();

    private final CurrentTripListener listener;
    private final RiderTripStateInteractor tripStateInteractor;
    private final GeocodeInteractor geocodeInteractor;
    private final SchedulerProvider schedulerProvider;

    public DefaultCurrentTripViewModel(final CurrentTripListener listener,
                                       final RiderTripStateInteractor tripStateInteractor,
                                       final GeocodeInteractor geocodeInteractor,
                                       final User user,
                                       final UserStorageWriter userStorageWriter,
                                       final Observable<FleetInfo> observableFleet) {
        this(
            listener,
            tripStateInteractor,
            geocodeInteractor,
            user,
            observableFleet,
            createDefaultDebugConsumer(userStorageWriter),
            new DefaultSchedulerProvider(),
            DEFAULT_POLL_INTERVAL_MILLI
        );
    }

    public DefaultCurrentTripViewModel(final CurrentTripListener listener,
                                       final RiderTripStateInteractor tripStateInteractor,
                                       final GeocodeInteractor geocodeInteractor,
                                       final User user,
                                       final Observable<FleetInfo> observableFleet,
                                       final Consumer<TripDebugData> tripDebugConsumer,
                                       final SchedulerProvider schedulerProvider,
                                       final int pollIntervalMilli) {
        this.listener = listener;
        this.tripStateInteractor = tripStateInteractor;
        this.geocodeInteractor = geocodeInteractor;
        this.schedulerProvider = schedulerProvider;

        final String passengerId = user.getId();

        observablePassengerState = Observable
            .interval(0, pollIntervalMilli, TimeUnit.MILLISECONDS, schedulerProvider.io())
            .observeOn(schedulerProvider.computation())
            .skipUntil(tripIdSubject)
            .flatMapSingle(time -> observableFleet.firstOrError())
            .flatMapSingle(fleetInfo -> tripStateInteractor.getTripState(tripIdSubject.getValue(), fleetInfo.getId())
                .map(Result::success)
                .doOnError(e -> Timber.e(e, "Failed to get passenger state"))
                .onErrorReturn(Result::failure)
            )
            .filter(Result::isSuccess)
            .map(Result::get)
            .doOnNext(passengerStateModel -> tripDebugConsumer.accept(
                new TripDebugData(
                    passengerId,
                    tripIdSubject.getValue(),
                    passengerStateModel
                )
            ));
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
        tripStateInteractor.shutDown();
    }

    @Override
    public Observable<FollowTripDisplayState> getDisplay() {
        return observablePassengerState
            // Continue on same thread as state machine
            .scan(
                // Junk value
                Pair.<TripStateModel, Boolean>create(null, false),
                (oldState, newState) -> Pair.create(
                    newState,
                    oldState.first == null || oldState.first.getStage() != newState.getStage()
                )
            )
            .filter(pair -> pair.first != null)
            .observeOn(schedulerProvider.computation())
            .flatMap(passengerStateAndChanged -> {
                final TripStateModel passengerState = passengerStateAndChanged.first;
                final boolean hasChanged = passengerStateAndChanged.second;
                return Single.zip(
                    reverseGeocodeIfChanged(pickup, passengerState.getPassengerPickupLocation()),
                    reverseGeocodeIfChanged(dropOff, passengerState.getPassengerDropOffLocation()),
                    NamedPickupDropOff::new
                )
                    .map(pickupDropOff -> new FollowTripDisplayState(hasChanged, passengerState, pickupDropOff))
                    .toObservable();
            });
    }

    @Override
    public void initialize(final String tripId) {
        tripIdSubject.onNext(tripId);
    }

    @Override
    public void cancelTrip() {
        listener.cancelTrip();
    }

    @Override
    public void changePickup() {
        listener.changePickup();
    }

    @Override
    public void tripFinished() {
        listener.tripFinished();
    }

    @Override
    public void doneConfirmingCancellation() {
        listener.tripFinished();
    }

    private Single<NamedTaskLocation> reverseGeocodeIfChanged(final BehaviorSubject<NamedTaskLocation> geocodeSubject,
                                                              final LatLng newLocation) {
        final NamedTaskLocation currentValue = geocodeSubject.getValue();
        if (currentValue != null && currentValue.getLocation().getLatLng().equals(newLocation)) {
            return Single.just(currentValue);
        }
        return geocodeInteractor.getBestReverseGeocodeResult(newLocation)
            .observeOn(schedulerProvider.computation())
            .retry(GEOCODE_RETRY_COUNT)
            .doOnError(e -> Timber.e(e, "Failed to reverse geocode task location"))
            .onErrorReturn(Result::failure)
            .map(result -> result.isSuccess() ? result.get() : new NamedTaskLocation("", new TaskLocation(newLocation)))
            // cache result
            .doOnNext(geocodeSubject::onNext)
            .firstOrError();
    }

    public static class TripDebugData {
        private final String riderId;
        private final String tripId;
        private final TripStateModel tripStateModel;

        public TripDebugData(final String riderId, final String tripId, final TripStateModel tripStateModel) {
            this.riderId = riderId;
            this.tripId = tripId;
            this.tripStateModel = tripStateModel;
        }
    }

    private static Consumer<TripDebugData> createDefaultDebugConsumer(final UserStorageWriter userStorageWriter) {
        return tripDebugData -> {
            final String dataJson = new Gson().toJson(tripDebugData, TripDebugData.class);
            userStorageWriter.storeStringPreference(RiderStorageKeys.TRIP_STATE, dataJson);
        };
    }
}
