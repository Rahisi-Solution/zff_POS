package com.rahisi.zffboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {
    private final List<Route> routeList;
    private final Context context;
    private int selectedPosition = -1;

    public RouteAdapter(List<Route> routeList, Context context) {
        this.routeList = routeList;
        this.context = context;
    }

    public static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView nameLabel, tv_time;
        RadioButton radioRouteSelected;
        RelativeLayout route_layout;

        public RouteViewHolder(View itemView) {
            super(itemView);
            nameLabel = itemView.findViewById(R.id.lblRouteName);
            tv_time = itemView.findViewById(R.id.tv_time);
            radioRouteSelected = itemView.findViewById(R.id.radioButtonShehiaSelected);
            route_layout = itemView.findViewById(R.id.route_layout);
        }
    }

    @Override
    public RouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.route_row_layout, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RouteViewHolder holder, final int position) {
        Route route = routeList.get(position);
        System.out.println("Route Adapter Name: " + route.getRouteName());
        holder.nameLabel.setText(route.getRouteName());
        holder.tv_time.setText(route.getDeparture_time());
        holder.radioRouteSelected.setChecked(position == selectedPosition);

        View.OnClickListener clickListener = v -> {
            updateSelection(position);
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.route_layout.setOnClickListener(clickListener);
        holder.nameLabel.setOnClickListener(clickListener);
        holder.tv_time.setOnClickListener(clickListener);
        holder.radioRouteSelected.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    private void updateSelection(int position) {
        if (position == RecyclerView.NO_POSITION)
            return;
        if (selectedPosition >= 0 && selectedPosition < routeList.size()) {
            routeList.get(selectedPosition).setSelected(false);
            notifyItemChanged(selectedPosition);
        }

        selectedPosition = position;
        routeList.get(position).setSelected(true);
        notifyItemChanged(position);
    }

    public Route getSelectedRoute() {
        if (selectedPosition >= 0 && selectedPosition < routeList.size()) {
            return routeList.get(selectedPosition);
        }
        return null;
    }
}
