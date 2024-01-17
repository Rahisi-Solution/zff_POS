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
    private List<Route> routeList;
    private int lastSelectedPosition = -1;
    private Context context;

    public class RouteViewHolder extends RecyclerView.ViewHolder {
        public TextView nameLabel, tv_time;
        public RadioButton radioRouteSelected;
        public RelativeLayout route_layout;

        public RouteViewHolder(View view) {
            super(view);
            nameLabel =  view.findViewById(R.id.lblRouteName);
            tv_time =  view.findViewById(R.id.tv_time);
            radioRouteSelected = view.findViewById(R.id.radioButtonShehiaSelected);
            route_layout = view.findViewById(R.id.route_layout);
        }
    }

    public RouteAdapter(List<Route> routeList, Context context) {
        this.routeList = routeList;
        this.context = context;
    }

    @Override
    public RouteAdapter.RouteViewHolder onCreateViewHolder( ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_row_layout, parent, false);

        return new RouteAdapter.RouteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder( final RouteAdapter.RouteViewHolder holder, final int position) {
        Route routeObject = routeList.get(position);
        holder.nameLabel.setText(routeObject.getRouteName());
        holder.tv_time.setText(routeObject.getDeparture_time());
        holder.radioRouteSelected.setChecked(lastSelectedPosition == position);
        holder.radioRouteSelected.setTag(routeList.get(position));

        holder.radioRouteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                for(Route _route: routeList){
                    _route.setSelected(false);
                }
                lastSelectedPosition = position;
                RadioButton radioButton = (RadioButton) v;
                Route shehia = (Route) radioButton.getTag();
                shehia.setSelected(true);
                routeList.get(position).setSelected(true);
                notifyDataSetChanged();
            }
        });

        holder.nameLabel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(Route _route: routeList){
                    _route.setSelected(false);
                }
                lastSelectedPosition = position;
                RadioButton radioButton = holder.radioRouteSelected;
                Route route = (Route) radioButton.getTag();
                route.setSelected(true);
                routeList.get(position).setSelected(true);
                notifyDataSetChanged();
            }
        });

        holder.route_layout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(Route _route: routeList){
                    _route.setSelected(false);
                }
                lastSelectedPosition = position;
                RadioButton radioButton = holder.radioRouteSelected;
                Route route = (Route) radioButton.getTag();
                route.setSelected(true);
                routeList.get(position).setSelected(true);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }
}
