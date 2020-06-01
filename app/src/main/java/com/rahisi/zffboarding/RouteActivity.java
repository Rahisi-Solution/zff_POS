package com.rahisi.zffboarding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.InvalidScannerNameException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteActivity extends AppCompatActivity {
    private List<Route> routeList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private RouteAdapter routeAdapter;
    private Button btnSelectRoute;
    private SharedPreferences mSharedPreferences, mySharedPref;

    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        // create the AidcManager providing a Context and a
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {
                System.out.println("Creating Manager");
                manager = aidcManager;
                try{
                    if(manager == null) System.out.println("Manager is null");
                    barcodeReader = manager.createBarcodeReader();
                } catch (InvalidScannerNameException e){
                    Toast.makeText(RouteActivity.this, "Invalid Scanner Name Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception e){
                    Toast.makeText(RouteActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });


        mySharedPref = RouteActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        Intent intent = getIntent();
        if (intent.hasExtra("action")) {
            action = getIntent().getExtras().getString("action", "NA");
        } else {
            Intent intent2 = new Intent(RouteActivity.this, ModeActivity.class);
            startActivity(intent2);
            finish();
        }
        prepareRoutes();

        btnSelectRoute = findViewById(R.id.btnSelectRoute);
        btnSelectRoute.setEnabled(true);
        mRecyclerView =  findViewById(R.id.route_view);

        routeAdapter = new RouteAdapter(routeList, this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(routeAdapter);

        btnSelectRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create Ticket based on available choices
                String route_id = "";
                String route_name = "";
                String departure_time="";

                try {
                    for (int i = 0; i < routeList.size(); i++) {
                        Route route = routeList.get(i);
                        if (route.isSelected() == true) {
                            route_id = route.getRoute_id();
                            route_name =  route.getRouteName();
                            departure_time = route.getDeparture_time();
                            break;
                        }
                    }
                    SharedPreferences routeSharedPref = RouteActivity.this.getSharedPreferences(Config.ROUTE_SHARED_PREF, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = routeSharedPref.edit();
                    editor.putString("route_id", route_id);
                    editor.putString("route_name", route_name);
                    editor.putString("departure_time", departure_time);
                    editor.putString("action", action);
                    editor.commit();

                    if(!route_id.equals("")) {
                        Intent intent = new Intent(RouteActivity.this, MainActivity.class);
                        intent.putExtra("route_id", route_id);
                        intent.putExtra("route_name", route_name);
                        intent.putExtra("departure_time", departure_time);
                        intent.putExtra("action", action);
                        startActivity(intent);
                        finish();
                    }else{
                        //Displaying an error message on toast
                        Toast.makeText(RouteActivity.this, "No Route Selected", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void prepareRoutes(){
        JSONObject _route;
        try {
            // Read data from the returned menu
            mSharedPreferences = RouteActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            JSONArray routes = new JSONArray(mSharedPreferences.getString("routes", null));
            for (int index = 0; index < routes.length(); index++) {
                try {
                    _route = routes.getJSONObject(index);
                    Route obj_route = new Route(
                            _route.getString("id"),
                            _route.getString("txt_name"),
                            _route.getString("tim_departure_time")
                    );
                    routeList.add(obj_route);
                } catch (JSONException e) {
                    System.out.println(e.toString());
                }
            }
        }catch (JSONException e){

        }
    }

    private Route getRoute( String route_id, String destination, String departure_time) {
        return new Route(route_id, destination, departure_time);
    }

    static BarcodeReader getBarcodeObject() {
        return barcodeReader;
    }
}
