package com.rahisi.zffboarding;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;
import com.telpo.tps550.api.decode.Decode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {
    TextView mOperatorDisplayLabel;
    TextView mOperatorDisplay;
    TextView mTicketNumberDisplay;
    TextView mPassengerNameDisplay;
    TextView tv_route;
    TextView tv_time;
    TextView tv_date;
    TextView tv_action_performed;
    EditText mTicketNumberEditor;
    Button mVerifyTicketButton;
    Button mSearchTicketButton;
    Button mLogoutButton;
    SharedPreferences mSharedPreferences;
    SharedPreferences  routeSharedPref;
    Dialog manualDialog;
    Dialog childDialog;
    String mTicketNumber;
    String pin;
    String operator;
    String qr_departure_date;
    String qr_departure_time;
    String qr_destination;
    String qr_ticket_number;
    String qr_today_date;
    String qr_route;
    String qr_age;
    String qr_name;
    String today_date;
    String  route_id;
    String  route_name;
    String routesJson;
    String route_departure_time;
    String action;
    String service_to_call;
    String mDepartureDate;
    String result_status;
    String token;
    private int mTotalTickets;
    int mScannedTickets;
    SimpleDateFormat db_date_format = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat qr_date_format = new SimpleDateFormat("dd MMMM, yyyy");
    TicketDatabaseAdapter mDatabaseAdapter = new TicketDatabaseAdapter(this);
    private com.honeywell.aidc.BarcodeReader barcodeReader;
    private Context mContext = MainActivity.this;
    private static final long MAX_SESSION_TIME = 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        setContentView(R.layout.activity_main);
        prepareData();

        routeSharedPref = getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        mSharedPreferences = getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        routesJson = mSharedPreferences.getString(Config.PREF_ROUTES_JSON, "");
        route_id = mSharedPreferences.getString(Config.PREF_SELECTED_ROUTE_ID, "NA");
        route_name = mSharedPreferences.getString(Config.PREF_SELECTED_ROUTE_NAME, "NA");
        route_departure_time = mSharedPreferences.getString(Config.PREF_SELECTED_ROUTE_TIME, "NA");
        action = mSharedPreferences.getString(Config.PREF_SELECTED_ACTION, "NA");
        System.out.println("Route id: " + route_id + " Route Json: " + routesJson + " Route Name: " + route_name + " Route time: " + route_departure_time + " Action: "  + action);

        loadRouteFromJson();

        tv_action_performed = findViewById(R.id.tv_task_performed);
        mOperatorDisplayLabel = findViewById(R.id.textViewOperatorLabel);
        mOperatorDisplay = findViewById(R.id.textViewOperator);
        mVerifyTicketButton = findViewById(R.id.buttonVerifyTicket);
        mSearchTicketButton = findViewById(R.id.buttonWriteManual);
        mLogoutButton = findViewById(R.id.buttonLogout);
        mTicketNumberDisplay = findViewById(R.id.textViewTicketNumber);
        mPassengerNameDisplay = findViewById(R.id.textViewPassengerName);
        mScannedTickets = mSharedPreferences.getInt(Config.SHARED_SCANNED_TICKETS, 0);
        operator = mSharedPreferences.getString(Config.KEY_OPERATOR, "NA");
        pin = mSharedPreferences.getString("pin", "NA");
        mOperatorDisplayLabel.setVisibility(View.INVISIBLE);

        tv_route = findViewById(R.id.tv_route);
        tv_time = findViewById(R.id.tv_time);
        tv_date = findViewById(R.id.tv_date);

        setupDateAndRouteUI();

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Date today = new Date();
        String dateToStr = format.format(today);
        today_date = db_date_format.format(today);
        qr_today_date = qr_date_format.format(today);
        tv_date.setText(dateToStr);
        tv_time.setText(route_departure_time.substring(0, 5));
        tv_route.setText(route_name);

        if(Build.MODEL.equals("EDA50K") || Build.MODEL.equals("EDA51K")){
            mVerifyTicketButton.setVisibility(View.GONE);
        }

        if(action.equals("boarding")) {
            tv_action_performed.setText(getResources().getString(R.string.now_boarding));
            getDailyTickets(route_id, route_departure_time);
        }else{
            tv_action_performed.setText(getResources().getString(R.string.now_verifing));
        }

        mOperatorDisplay.setText(mSharedPreferences.getString(Config.OPERATOR_NAME_SHARED_PREF, getString(R.string.prompt_empty_values)));
        mOperatorDisplayLabel.setText("SCANNED: " + Integer.toString(mScannedTickets));
        System.out.println("Selected Time: " + mSharedPreferences.getString(Config.SHARED_DEPARTURE_DATE, "NA"));

        mVerifyTicketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("MODEL " + Build.MODEL);
                if(Build.MODEL.equals("EDA50K") || Build.MODEL.equals("EDA51K")){
                    mVerifyTicketButton.setVisibility(View.GONE);
                }
                new GetTicketNumberTask().execute();
            }
        });

        mSearchTicketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showManualDialog();
            }
        });

        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        barcodeReader = RouteActivity.getBarcodeObject();

        if (barcodeReader != null) {
            System.out.println("Barcode Available Here:" + barcodeReader);
            barcodeReader.addBarcodeListener(this);
            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
                barcodeReader.setProperty(BarcodeReader.PROPERTY_NOTIFICATION_GOOD_READ_ENABLED, false);
            } catch (UnsupportedPropertyException e) {
                Toast.makeText(this, "Failed to apply properties", Toast.LENGTH_SHORT).show();
            }
            barcodeReader.addTriggerListener(this);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
            properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
            properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
            barcodeReader.setProperties(properties);
        }else{
            if(Build.MODEL.equals("EDA50K") || Build.MODEL.equals("EDA51K")) {
                Intent intent = new Intent(MainActivity.this, RouteActivity.class);
                startActivity(intent);
                finish();
            }else{
                mVerifyTicketButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadRouteFromJson() {
        try {
            String json = mSharedPreferences.getString(Config.PREF_ROUTES_JSON, "");
            String selectedRouteId  = mSharedPreferences.getString(Config.PREF_SELECTED_ROUTE_ID, "NA");
            String selectedRouteName = mSharedPreferences.getString(Config.PREF_SELECTED_ROUTE_NAME, "NA");
            String selectedRouteTime = mSharedPreferences.getString(Config.PREF_SELECTED_ROUTE_TIME, "NA");
            String selectedAction     = mSharedPreferences.getString(Config.PREF_SELECTED_ACTION, "NA");

            System.out.println("Saved Route ID: " + selectedRouteId);
            System.out.println("Saved Route JSON: " + json);
            System.out.println("Saved Route Name: " + selectedRouteName);
            System.out.println("Saved Route Time: " + selectedRouteTime);
            System.out.println("Saved Action: " + selectedAction);

            route_id = selectedRouteId;
            route_name = selectedRouteName;
            route_departure_time = selectedRouteTime;
            action = selectedAction;

            if (json.isEmpty() || selectedRouteId.equals("NA")) {
                route_id = "NA";
                route_name = "NA";
                route_departure_time = "NA";
                action = "NA";
            }

        } catch (Exception e) {
            route_id = "NA";
            route_name = "NA";
            route_departure_time = "NA";
            action = "NA";
        }
    }


    private void setupDateAndRouteUI() {
        Date today = new Date();
        today_date = db_date_format.format(today);
        tv_date.setText(new SimpleDateFormat("dd/MM/yyyy").format(today));
        tv_time.setText(route_departure_time.substring(0, 5));
        tv_route.setText(route_name);
        tv_action_performed.setText(
                action.equals("boarding")
                        ? getString(R.string.now_boarding)
                        : getString(R.string.now_verifing)
        );

        if (action.equals("boarding")) {
            getDailyTickets(route_id, route_departure_time);
        }
    }

    private void playSound(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.reset();
            mp.release();
        });
        mediaPlayer.start();
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Sound starts here:" + event.getCodeId());
                mTicketNumberDisplay.setText("Result");
                if(event.getCodeId().toLowerCase().equals("j")) {
                    System.out.println("HERE NOW " + event.getCodeId());
                    searchTicket(event.getBarcodeData());
                }else if (event.getCodeId().toLowerCase().equals("s")){
                    extractTicketNumberFromQRCode(event.getBarcodeData());
                    System.out.println("Sound Details: " + qr_departure_date + " - " + qr_today_date);
                    if(qr_ticket_number == null){
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.not_available_beep);
                        mPassengerNameDisplay.setText("Invalid Ticket");
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mPassengerNameDisplay.setTextColor(Color.parseColor("#A93226"));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                    }else if(!qr_ticket_number.substring(0, 3).equals("ZFF") && !qr_ticket_number.substring(0, 3).equals("ZFA")){
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.failure_beep);
                        mPassengerNameDisplay.setTextColor(Color.RED);
                        mPassengerNameDisplay.setText(R.string.company_invalid);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_no_entry, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                    }else if(!qr_departure_date.equals(qr_today_date)){
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.failure_beep);
                        mPassengerNameDisplay.setTextColor(Color.RED);
                        mPassengerNameDisplay.setText(R.string.date_invalid);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));                            mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                    }
//                    else if(!qr_departure_time.substring(0, 8).equals(route_departure_time.substring(0, 8))){
//                        System.out.println("Not for this time: " + qr_departure_time + "Not for this route: " + route_departure_time);
//                        Config.doVibration(MainActivity.this);
//                        playSound(R.raw.failure_beep);
//                        mPassengerNameDisplay.setTextColor(Color.RED);
//                        mPassengerNameDisplay.setText(R.string.time_invalid);
//                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
//                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
//                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
//                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
//                        mVerifyTicketButton.setEnabled(true);
//                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
//                    }
                    else if(!qr_route.equals(route_name)){
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.failure_beep);
                        mPassengerNameDisplay.setTextColor(Color.RED);
                        mPassengerNameDisplay.setText(R.string.ticket_not_for_this_route);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                    } else{
                        searchTicket(qr_ticket_number);
                    }
                }
            }
        });
    }

    private void extractTicketNumberFromQRCode(String qrdata){
        System.out.println("Ticket Extraction: " + qrdata);
        if(!qrdata.startsWith("{")){
            qr_ticket_number = null;
        } else {
            try {
                JSONObject jsonObject = new JSONObject(qrdata);
                JSONObject ticket = jsonObject.getJSONObject("Ticket");
                System.out.println("Ticket Data here: " + qrdata);
                qr_ticket_number = ticket.get("Ticket Number").toString();
                qr_departure_date =  ticket.get("Departure Date").toString();
                qr_departure_time =  ticket.get("Departure Time").toString();
                qr_destination =  ticket.get("Destination").toString();
                qr_name =  ticket.get("Passenger Name").toString();
                qr_age =  ticket.get("Age").toString();
                qr_route =  ticket.get("Boarding Point").toString() +" - " + ticket.get("Destination").toString();
            }catch (JSONException e) {
                Config.doVibration(MainActivity.this);
                System.out.println("Barcode Error: " + e);
            }
        }
    }

    @Override
    public void onTriggerEvent( TriggerStateChangeEvent event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onFailureEvent( BarcodeFailureEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeReader != null) {
            barcodeReader.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (barcodeReader != null) {
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.removeTriggerListener(this);
        }
    }

    public void logout(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.logout_confirmation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        signout();
                    }
                });

        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showManualDialog(){
        manualDialog = new Dialog(this);
        Button btnSearch;
        manualDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        manualDialog.setContentView(R.layout.dialog_add_manual);
        mTicketNumberEditor = manualDialog.findViewById(R.id.et_ticket_number);
        mTicketNumberEditor.setHint("Enter Ticket Number");
        manualDialog.setCancelable(false);
        manualDialog.setCanceledOnTouchOutside(true);
        manualDialog.show();

        btnSearch = manualDialog.findViewById(R.id.btn_search);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manualDialog.dismiss();
                mTicketNumber = mTicketNumberEditor.getText().toString();
                if (!mTicketNumber.startsWith("ZFF") && !mTicketNumber.startsWith("ZFA")) {
                    mTicketNumber = "ZFF" + mTicketNumber;
                    System.out.println("Ticket Number: " + mTicketNumber);
                }

                mTicketNumberDisplay.setText(getString(R.string.barcode_title_text).replace(getString(R.string.barcode_placeholder), mTicketNumber));
                if (mTicketNumber != null && mTicketNumber.length() > 0) {
                    JSONObject ticket = mDatabaseAdapter.getTicket(mTicketNumber);
                    if (ticket.has(Config.TICKET_ID)) {
                        try {
                            int isScanned = ticket.getInt(Config.TICKET_SCANNED);
                            if (isScanned > 0) {
                                mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                mVerifyTicketButton.setEnabled(true);
                                return;
                            } else if (ticket.getString(Config.TICKET_STATUS).toLowerCase().equals("cancelled")) {
                                mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                mVerifyTicketButton.setEnabled(true);
                                return;
                            }
                        } catch (JSONException ex) {
                            mPassengerNameDisplay.setText(ex.getMessage());
                            mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                            mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                            mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                            mVerifyTicketButton.setText(R.string.button_scan_sticker);
                            mVerifyTicketButton.setEnabled(true);
                            return;
                        }
                        processOnlineTicket();
                    } else {
                        processOnlineTicket();
                        return;
                    }
                } else {
                    mPassengerNameDisplay.setText(R.string.passenger_not_found);
                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                }

                mVerifyTicketButton.setText(R.string.button_scan_sticker);
                mVerifyTicketButton.setEnabled(true);
            }
        });
    }

    private void signout(){
        final ProgressDialog loading;
        loading = ProgressDialog.show(mContext, "Logout...", "Please wait...", false, false);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.LOGOUT_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("Logout Response" + response);
                        loading.dismiss();
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            System.out.println("Decoded response: " + jsonObject);
//                            var code = jsonObject.getString("code");
//                            System.out.println("Code: " + code);
//                            if(code.toString() == "200"){
//
//                            }else{
//                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            System.out.println("Logout Error: " + e.getMessage());
                        }

                        SharedPreferences preferences = getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(Config.LOGGEDIN_SHARED_PREF, false);
                        editor.putString(Config.OPERATOR_SHARED_PREF, "");
                        editor.putString(Config.OPERATOR_NAME_SHARED_PREF, "");
                        editor.putInt(Config.SHARED_TOTAL_TICKETS, 0);
                        editor.putInt(Config.SHARED_SCANNED_TICKETS, 0);
                        editor.commit();
                        mDatabaseAdapter.clearDatabase();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK );
                        startActivity(intent);
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("Logout Error Response: " + error.getMessage());
                        loading.dismiss();
                        Toast.makeText(mContext, error.toString(), Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("username", operator);
//                params.put("username", "718192");
                params.put(Config.API_KEY_NAME, Config.API_KEY);
                System.out.println("Logout Params:" + params);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private void processTicket(JSONObject ticket) {
        System.out.println("Process Ticket");
        try {
            Config.doVibration(MainActivity.this);
            playSound(R.raw.success_beep);
            mPassengerNameDisplay.setTextColor(Color.parseColor("#358f80"));
            mPassengerNameDisplay.setText(qr_name);
            mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_check, 0, 0);
            mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
            mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
            mSearchTicketButton.setBackgroundColor(Color.parseColor("#186A3B"));
            mScannedTickets += 1;
            mOperatorDisplayLabel.setText("SCANNED: " + Integer.toString(mScannedTickets));
            mDatabaseAdapter.update(ticket.getString(Config.TICKET_ID), ticket.getInt(Config.TICKET_SCAN_COUNT) + 1);
            System.out.println("Scanned Tickets: " + mScannedTickets + "Scanned Tickets Action: " +  action);
            if(action.equals("boarding")) {
                System.out.println("Boarding Ticket:" + ticket);
                processOnlineTicket();
            }else{
                System.out.println("Verify Ticket:" + ticket);
                verifyTicket();
            }
        } catch (JSONException ex) {
            if(action.equals("boarding")) {
                processOnlineTicket(

                );
            }else{
                verifyTicket();
            }
        }
    }

    private void processOnlineTicket() {
        final ProgressDialog loading;
        loading = ProgressDialog.show(mContext, "Checking...", "Please wait...", false, false);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.FIND_TICKET_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        loading.dismiss();
                        System.out.println("Search Ticket Response: " + response);
                        try {
                            JSONObject responseData = new JSONObject(response);
                            JSONObject decodedResponse = responseData.getJSONObject("response");
                            JSONObject decodedData = decodedResponse.getJSONObject("data");
                            int code = decodedData.getInt("code");
                                if(!mTicketNumber.substring(0, 3).equals("ZFF") && !mTicketNumber.substring(0, 3).equals("ZFA")){
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.company_invalid);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_no_entry, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                }else if (code == 100){
                                    System.out.println("Process Online Ticket Data" + responseData);
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.parseColor("#A93226"));
                                    mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else  if (code == 400){
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else  if (code == 700){
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_notfound);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else  if (code == 800){
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_notfound);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                }else  if (code == 655){
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_not_for_this_route);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                }
                                else  if (code == 200){
                                    mScannedTickets += 1;
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.success_beep);
                                    if(Objects.equals(qr_name, null)){
                                        mPassengerNameDisplay.setText(R.string.ticket_valid);
                                    }else{
                                        mPassengerNameDisplay.setText(qr_name);
                                    }
                                    if(!qr_age.equals("Adult")){
                                        mPassengerNameDisplay.setTextColor(Color.parseColor("#9c6644"));
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_child_check, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.child_border));
                                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#9c6644"));
                                    }else{
                                        mPassengerNameDisplay.setTextColor(Color.parseColor("#186A3B"));
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#186A3B"));
                                    }
                                    return;
                                }
                                else  if (code == 600) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.date_invalid);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                }else  if (code == 650){
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.time_invalid);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                } else {
                                    mPassengerNameDisplay.setText(R.string.passenger_not_found);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                }
                                mScannedTickets += 1;
                                mOperatorDisplayLabel.setText("SCANNED: " + mScannedTickets);
                            } catch (JSONException ex) {
                            System.out.println("Online Ticket Process Error: " + ex);
                            mPassengerNameDisplay.setText(R.string.passenger_not_found);
                            mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                            mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                            mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        }
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loading.dismiss();
                        System.out.println("On Error Response: " + error.toString());
                        mPassengerNameDisplay.setText(R.string.no_tickets_found);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_info_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_empty_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("reference", mTicketNumber);
                params.put("time", route_departure_time.substring(0, 5) + ":00");
                params.put("date", today_date);
                params.put("operator", operator);
                params.put("pin", pin);
                params.put("route", route_id);
                params.put("authorization", "Bearer " + token);
                System.out.println("Process Online Ticket Params: " + params);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("Process Online Ticket Credentials: " + auth + "\n");
                headers.put("Authorization", auth);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private void verifyTicket() {
        final ProgressDialog loading;
        loading = ProgressDialog.show(mContext, "Verifying...", "Please wait...", false, false);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.VERIFY_TICKET_URL,
                response -> {
                    System.out.println("Verify Ticket Response:" + response);
                    loading.dismiss();
                    try {
                        JSONObject responseData = new JSONObject(response);
                        JSONObject decodedResponse = responseData.getJSONObject("response");
                        JSONObject decodedData = decodedResponse.getJSONObject("data");
                        int code = decodedData.getInt("code");
                        int decodedCode = decodedResponse.getInt("code");

                            if (decodedCode == 200) {
                                if (code == 100) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else if (code == 400) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else if (code == 500) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_expired);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else if (code == 300) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_expired);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else if (code == 655) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_not_for_this_route);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else if (code == 350) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_pending);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                    return;
                                } else if (code == 700) {
                                    Config.doVibration(MainActivity.this);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.ticket_notfound);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    return;
                                } else if (code == 200) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.success_beep);
                                    if (Objects.equals(qr_name, null)) {
                                        mPassengerNameDisplay.setText(R.string.ticket_valid);
                                    } else {
                                        mPassengerNameDisplay.setText(qr_name);
                                    }
                                    if (!qr_age.equals("Adult")) {
                                        mPassengerNameDisplay.setTextColor(Color.parseColor("#9c6644"));
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_child_check, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.child_border));
                                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#9c6644"));
                                    } else {
                                        mPassengerNameDisplay.setTextColor(Color.parseColor("#186A3B"));
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#186A3B"));
                                    }
                                    return;
                                } else if (code == 600) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.date_invalid);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                } else if (code == 650) {
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.time_invalid);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                } else if (!qr_ticket_number.substring(0, 3).equals("ZFF") && !qr_ticket_number.substring(0, 3).equals("ZAF")) {//changed from ZFA to ZAF
                                    Config.doVibration(MainActivity.this);
                                    playSound(R.raw.failure_beep);
                                    mPassengerNameDisplay.setTextColor(Color.RED);
                                    mPassengerNameDisplay.setText(R.string.company_invalid);
                                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_no_entry, 0, 0);
                                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                    mVerifyTicketButton.setEnabled(true);
                                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                                }

                            } else {
                                mPassengerNameDisplay.setText(R.string.passenger_not_found);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));

                        }
                        mScannedTickets += 1;
                        mOperatorDisplayLabel.setText("SCANNED: " + mScannedTickets);

                    }  catch (JSONException e) {
                        System.out.println("Error During Verify Ticket: " + e.getMessage());
                        e.printStackTrace();
                        mPassengerNameDisplay.setText(R.string.passenger_not_found);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                    }
                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                    mVerifyTicketButton.setEnabled(true);
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loading.dismiss();
                        System.out.println("Error Verify Ticket Volley: "  + error.toString());
                        mPassengerNameDisplay.setText(R.string.no_tickets_found);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_info_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_empty_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("reference", mTicketNumber);
                params.put("time", route_departure_time);
                params.put("date", today_date);
                params.put("operator", operator);
                params.put("pin", pin);
                params.put("route", route_id);
                params.put("authorization", "Bearer " + token);
                System.out.println("Verify Ticket Params: "  + params);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("Verify Ticket Credentials: "  + auth + "\n");
                headers.put("Authorization", auth);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private void markTicketBoarded() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.UPDATE_TICKET_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject responseData = new JSONObject(response);
                            System.out.println("Update Ticket:" + responseData);

                        } catch (JSONException e) {
                            System.out.println("TICKET ERRORS MARK TICKET: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("Update Ticket Error: " + error.toString());
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                String operator = mSharedPreferences.getString(Config.OPERATOR_SHARED_PREF, "0");
                Map<String,String> params = new HashMap<>();
                params.put("ticket_id", mTicketNumber);
                params.put("boarding_status", "YES");
                params.put("operator_id", operator);
                params.put("device_id", getIMEI());
                params.put("authorization", "Bearer " + token);
                System.out.println("Update Ticket Params: " + params);
                params.put(Config.API_KEY_NAME, Config.API_KEY);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("Update Ticket Credentials: " + auth);
                headers.put("Authorization", auth);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private class GetTicketNumberTask extends AsyncTask<Void, Void, Exception> {
        String message;
        @Override
        protected Exception doInBackground(Void... params) {
            Exception result = null;
            try{
                Decode.open();
                message = Decode.read(10000);
            }catch (Exception e){
                e.printStackTrace();
                result = e;
            }finally {
                Decode.close();
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mVerifyTicketButton.setText(R.string.operating);
            mVerifyTicketButton.setEnabled(false);
        }

        @Override
        protected void onPostExecute(Exception result) {
            super.onPostExecute(result);
            Config.doVibration(MainActivity.this);
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.USE_DEFAULT_STREAM_TYPE, 50);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,150);
            if(result == null){
                searchTicket(message);
            }else {
                Toast.makeText(MainActivity.this, getString(R.string.operation_fail), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    private void searchTicket(String message){
        mTicketNumber = message;
        mTicketNumberDisplay.setText(getString(R.string.barcode_title_text).replace(getString(R.string.barcode_placeholder), mTicketNumber));
        if (mTicketNumber != null && mTicketNumber.length() > 0) {
            JSONObject ticket = mDatabaseAdapter.getTicket(mTicketNumber);
            if (ticket.has(Config.TICKET_ID)) {
                try {
                    int isScanned = ticket.getInt(Config.TICKET_SCANNED);
                    if(!mTicketNumber.substring(0, 3).equals("ZFF") && !mTicketNumber.substring(0, 3).equals("ZFA")){
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.failure_beep);
                        mPassengerNameDisplay.setTextColor(Color.RED);
                        mPassengerNameDisplay.setText(R.string.company_invalid);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_no_entry, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                    }else if (isScanned > 0) {
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.failure_beep);
                        mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                        mPassengerNameDisplay.setTextColor(Color.parseColor("#A93226"));
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.parseColor("#A93226"));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                        return;
                    } else if (ticket.getString(Config.TICKET_STATUS).toLowerCase().equals("cancelled")) {
                        Config.doVibration(MainActivity.this);
                        playSound(R.raw.failure_beep);
                        mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                        mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                        return;
                    }
                } catch (JSONException ex) {
                    Config.doVibration(MainActivity.this);
                    playSound(R.raw.failure_beep);
                    mPassengerNameDisplay.setText(ex.getMessage());
                    mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                    mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                    mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                    mVerifyTicketButton.setText(R.string.button_scan_sticker);
                    mVerifyTicketButton.setEnabled(true);
                    mSearchTicketButton.setBackgroundColor(Color.parseColor("#A93226"));
                    return;
                }

                if(action.equals("boarding")) {
                    System.out.println("Boarding Ticket:" + mTicketNumber);
                    processOnlineTicket();
                }else{
                    System.out.println("Verify Ticket:" + mTicketNumber);
                    verifyTicket();
                }
            } else {
                if(action.equals("boarding")) {
                    System.out.println("Boarding Ticket:" + mTicketNumber);
                    processOnlineTicket();
                }else{
                    System.out.println("Verify Ticket:" + mTicketNumber);
                    verifyTicket();
                }
                return;
            }
        } else {
            mPassengerNameDisplay.setText(R.string.passenger_not_found);
            mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
            mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
            mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
        }

        mVerifyTicketButton.setText(R.string.button_scan_sticker);
        mVerifyTicketButton.setEnabled(true);
    }

    public String getIMEI(){
        String IMEI = "";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            IMEI = "IMEI UNAVAILABLE";
        }else {
            TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            IMEI = tm.getDeviceId();
        }
        return IMEI;
    }


    @Override
    public boolean onCreateOptionsMenu( Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuLogout) {
            logout();
        }else if (id == R.id.menuChangeLocation) {
            Intent intent = new Intent(MainActivity.this, RouteActivity.class);
            intent.putExtra("action",action);
            startActivity(intent);
            finish();
        }else if (id == R.id.menuChangeMode) {
            Intent intent = new Intent(MainActivity.this, ModeActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getDailyTickets(final String route_id, final String route_departure_time) {
        Date date = new Date();
        String strDateFormat = "yyyy-MM-dd";
        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        final String formattedDate = dateFormat.format(date);
        mDepartureDate = formattedDate;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.BOOKING_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        System.out.println("Get Daily Tickets Response : 💥 " + response);
                        JSONArray tickets;
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject responseData = jsonObject.getJSONObject("response");
                             tickets = responseData.getJSONArray("data");
                            mDatabaseAdapter.clearDatabase();
                            System.out.println("Get Daily Tickets: " + tickets);
                            for (int i = 0; i < tickets.length(); i++){
                                JSONObject ticket = tickets.getJSONObject(i);
                                long id = mDatabaseAdapter.insertData(
                                        ticket.getString("id"),
                                        ticket.getString("ticket_number"),
                                        ticket.getString("txt_name"),
                                        ticket.getString("age"),
                                        ticket.getString("departure_date"),
                                        ticket.getString("departure_time"),
                                        ticket.getString("ticket_status"),
                                        0,
                                        ticket.getString("ticket_status"),
                                        0);
                                System.out.println("Daily Tickets: " + ticket);
                                if (id > 1) {
                                    mTotalTickets += 1;
                                }
                                System.out.println("Daily Tickets Number: " + ticket.getString("ticket_number"));
                            }
                        } catch (JSONException e) {
                            System.out.println("Daily Tickets Number Error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("Daily Tickets Number Volley Error: "+ error.toString());
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("date", formattedDate);
                params.put("route", route_id);
                params.put("time", route_departure_time.substring(0, 5) + ":00");
                params.put("operator", operator);
                params.put("pin", pin);
                params.put("authorization", "Bearer " + token);
                System.out.println("Daily Tickets Number Parameters: " + params);
                return params;
            }


            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                System.out.println("Daily Tickets Number Headers: " + headers);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private void childDialog(String ticket_age){
        TextView _ticket_age;
        childDialog = new Dialog(this);
        childDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        childDialog.setContentView(R.layout.dialog_child);
        _ticket_age = childDialog.findViewById(R.id.tv_ticket_age);
        _ticket_age.setText(ticket_age);
        childDialog.setCancelable(false);
        childDialog.setCanceledOnTouchOutside(true);
        childDialog.show();
    }

    private void prepareData(){
        SharedPreferences preferences = MainActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        token = preferences.getString(Config.AUTH_TOKEN, "n.a");
        System.out.println("Token: " + token);
    }

}
