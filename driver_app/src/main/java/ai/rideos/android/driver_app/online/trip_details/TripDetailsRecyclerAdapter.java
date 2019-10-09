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
package ai.rideos.android.driver_app.online.trip_details;

import ai.rideos.android.driver_app.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class TripDetailsRecyclerAdapter extends RecyclerView.Adapter<TripDetailsRecyclerAdapter.ViewHolder> {

    private List<TripDetail> tripDetails;
    private final Activity parentActivity;
    private final OnClickRowListener onClickRowListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // For now, each row is just a string
        public View view;

        ViewHolder(View view) {
            super(view);
            this.view = view;
        }
    }

    public interface OnClickRowListener {
        void clickedRow(final TripDetail tripDetail);
    }

    public TripDetailsRecyclerAdapter(final Activity parentActivity, final OnClickRowListener onClickRowListener) {
        this.parentActivity = parentActivity;
        this.onClickRowListener = onClickRowListener;
        this.tripDetails = Collections.emptyList();
    }

    @NonNull
    @Override
    public TripDetailsRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.trip_detail_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final TextView passengerTextView = holder.view.findViewById(R.id.trip_detail_passenger_name);
        final TextView actionButtonText = holder.view.findViewById(R.id.trip_detail_action_button);
        final ViewGroup pickupSection = holder.view.findViewById(R.id.trip_detail_pickup_section);
        final TextView pickupTextView = holder.view.findViewById(R.id.trip_detail_pickup_address);
        final TextView dropOffTextView = holder.view.findViewById(R.id.trip_detail_drop_off_address);
        final View contactButton = holder.view.findViewById(R.id.contact_button);
        final TripDetail tripDetail = tripDetails.get(position);

        passengerTextView.setText(tripDetail.getPassengerName());

        switch (tripDetail.getActionToPerform()) {
            case REJECT_TRIP:
                actionButtonText.setText(R.string.trip_detail_reject_ride_action_button);
                actionButtonText.setOnClickListener(click -> checkAndPerformAction(
                    tripDetail,
                    R.string.trip_detail_reject_alert_title,
                    R.string.trip_detail_reject_alert_description,
                    R.string.trip_detail_reject_alert_positive_button
                ));
                break;
            case CANCEL_TRIP:
                actionButtonText.setText(R.string.trip_detail_cancel_trip_action_button);
                actionButtonText.setOnClickListener(click -> checkAndPerformAction(
                    tripDetail,
                    R.string.trip_detail_cancel_alert_title,
                    R.string.trip_detail_cancel_alert_description,
                    R.string.trip_detail_cancel_alert_positive_button
                ));
                break;
            case END_TRIP:
                actionButtonText.setText(R.string.trip_detail_end_trip_action_button);
                actionButtonText.setOnClickListener(click -> checkAndPerformAction(
                    tripDetail,
                    R.string.trip_detail_end_alert_title,
                    R.string.trip_detail_end_alert_description,
                    R.string.trip_detail_end_alert_positive_button
                ));
                break;
        }

        if (tripDetail.getPickupAddress().isPresent()) {
            pickupSection.setVisibility(View.VISIBLE);
            pickupTextView.setText(tripDetail.getPickupAddress().get());
        } else {
            pickupSection.setVisibility(View.GONE);
        }
        dropOffTextView.setText(tripDetail.getDropOffAddress());

        if (tripDetail.getPassengerPhone().isPresent()) {
            final String phoneLink = String.format("tel:%s", tripDetail.getPassengerPhone().get());
            contactButton.setVisibility(View.VISIBLE);
            contactButton.setOnClickListener(click -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(phoneLink));
                parentActivity.startActivity(intent);
            });
        } else {
            contactButton.setVisibility(View.GONE);
        }
    }

    private void checkAndPerformAction(final TripDetail tripDetail,
                                       @StringRes final int alertTitle,
                                       @StringRes final int alertDescription,
                                       @StringRes final int alertPositiveText) {
        new Builder(parentActivity, R.style.DefaultAlertDialogTheme)
            .setTitle(alertTitle)
            .setMessage(alertDescription)
            .setPositiveButton(alertPositiveText, (dialog, i) -> onClickRowListener.clickedRow(tripDetail))
            .setNegativeButton(R.string.trip_detail_alert_negative_button, null)
            .show();
    }

    @Override
    public int getItemCount() {
        return tripDetails.size();
    }

    public void setTripDetails(final List<TripDetail> tripDetails) {
        this.tripDetails = tripDetails;
        notifyDataSetChanged();
    }
}
