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
package ai.rideos.android.adapters;

import ai.rideos.android.model.LocationSearchOptionModel;
import ai.rideos.android.rider_app.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class LocationSearchRecyclerAdapter extends RecyclerView.Adapter<LocationSearchRecyclerAdapter.ViewHolder> {

    private List<LocationSearchOptionModel> locations;
    private final OnClickRowListener onClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // For now, each row is just a string
        public View view;

        ViewHolder(View view) {
            super(view);
            this.view = view;
        }
    }

    public interface OnClickRowListener {
        void clicked(final LocationSearchOptionModel selectedRow);
    }

    public LocationSearchRecyclerAdapter(final OnClickRowListener onClickListener) {
        this.onClickListener = onClickListener;
        this.locations = Collections.emptyList();
    }

    @NonNull
    @Override
    public LocationSearchRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.location_recycler_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final TextView primaryTextView = holder.view.findViewById(R.id.location_row_primary_name);
        final TextView secondaryTextView = holder.view.findViewById(R.id.location_row_secondary_name);
        final TextView singleTextView = holder.view.findViewById(R.id.location_row_single_name);
        final AppCompatImageView imageView = holder.view.findViewById(R.id.option_icon);
        final LocationSearchOptionModel location = locations.get(position);

        // View holders will be reused, so explicitly set each text view to visible/invisible as required.
        if (location.getSecondaryName() == null || location.getSecondaryName().isEmpty()) {
            primaryTextView.setVisibility(View.INVISIBLE);
            secondaryTextView.setVisibility(View.INVISIBLE);
            singleTextView.setVisibility(View.VISIBLE);
            singleTextView.setText(location.getPrimaryName());
        } else {
            primaryTextView.setVisibility(View.VISIBLE);
            secondaryTextView.setVisibility(View.VISIBLE);
            singleTextView.setVisibility(View.INVISIBLE);
            primaryTextView.setText(location.getPrimaryName());
            secondaryTextView.setText(location.getSecondaryName());
        }

        if (location.getDrawableIcon().isPresent()) {
            imageView.setImageResource(location.getDrawableIcon().get());
        } else {
            // 0 is the empty resource id
            imageView.setImageResource(0);
        }

        holder.view.setOnClickListener(v -> onClickListener.clicked(locations.get(position)));

    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void setLocations(final List<LocationSearchOptionModel> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }
}
