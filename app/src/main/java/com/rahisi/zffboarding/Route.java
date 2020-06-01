package com.rahisi.zffboarding;

public class Route {
    private String route_id;
    private String route_name;
    private String departure_time;
    private boolean selected;

    public Route(String route_id, String destination_port, String departure_time) {
        this.route_id = route_id;
        this.route_name = destination_port;
        this.departure_time = departure_time;
    }

    public String getRoute_id() {
        return route_id;
    }

    public void setRoute_id(String route_id) {
        this.route_id = route_id;
    }

    public String getRouteName() {
        return route_name;
    }

    public void setRouteName(String destination_port) {
        this.route_name = destination_port;
    }

    public String getDeparture_time() {
        return departure_time;
    }

    public void setDeparture_time(String departure_time) {
        this.departure_time = departure_time;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
