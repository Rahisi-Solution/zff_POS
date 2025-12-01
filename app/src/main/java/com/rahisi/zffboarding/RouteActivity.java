package com.rahisi.zffboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteActivity extends AppCompatActivity {
    private List<Route> routeList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private RouteAdapter routeAdapter;
    private Button btnSelectRoute;
    private SharedPreferences appPrefs;
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    private String action = "NA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        action = getIntent().getStringExtra("action");
        if (action == null) {
            Toast.makeText(this, "Missing action - returning", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ModeActivity.class));
            finish();
            return;
        }

        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                try {
                    barcodeReader = manager.createBarcodeReader();
                } catch (InvalidScannerNameException e) {
                    Toast.makeText(RouteActivity.this, "Scanner Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(RouteActivity.this, "Scanner Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });


        appPrefs = getSharedPreferences(Config.SHARED_PREF_NAME, MODE_PRIVATE);

        loadRoutesFromJson();

        mRecyclerView = findViewById(R.id.route_view);
        btnSelectRoute = findViewById(R.id.btnSelectRoute);

        routeAdapter = new RouteAdapter(routeList, this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(routeAdapter);

        btnSelectRoute.setOnClickListener(v -> selectRouteAndContinue());
    }

    private void loadRoutesFromJson() {
        routeList.clear();
        try {
            String json = appPrefs.getString(Config.PREF_ROUTES_JSON, "");
            System.out.println("Available Route JSON: " + json);
            if (json == null || json.trim().isEmpty()) {
                Toast.makeText(this, "No routes available. Please login again.", Toast.LENGTH_LONG).show();
                return;
            }
            JSONArray routes = new JSONArray(json);
            for (int i = 0; i < routes.length(); i++) {
                JSONObject r = routes.getJSONObject(i);
                Route route = new Route(r.getString("id"), r.getString("txt_name"), r.getString("tim_departure_time"));
                routeList.add(route);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load routes: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void selectRouteAndContinue() {
        String route_id = "";
        String route_name = "";
        String departure_time = "";
        for (Route r : routeList) {
            if (r.isSelected()) {
                route_id = r.getRoute_id();
                route_name = r.getRouteName();
                departure_time = r.getDeparture_time();
                break;
            }
            System.out.println("Selected Route and Departure Time: " + route_id + " Route Name: " + route_name + " Departure Time: " + departure_time);
        }

        if (route_id.isEmpty()) {
            Toast.makeText(this, "Please select a route first", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(Config.PREF_SELECTED_ROUTE_ID, route_id);
        editor.putString(Config.PREF_SELECTED_ROUTE_NAME, route_name);
        editor.putString(Config.PREF_SELECTED_ROUTE_TIME, departure_time);
        editor.putString(Config.PREF_SELECTED_ACTION, action);
        editor.apply();

        Intent intent = new Intent(RouteActivity.this, MainActivity.class);
        intent.putExtra("route_id", route_id);
        intent.putExtra("route_name", route_name);
        intent.putExtra("departure_time", departure_time);
        intent.putExtra("action", action);
        System.out.println("Selected Route ID: " + route_id + " Route Name: " + route_name + " Departure Time: " + departure_time + " Action: " + action);
        startActivity(intent);
        finish();
    }

    public static BarcodeReader getBarcodeObject() {
        return barcodeReader;
    }
}
