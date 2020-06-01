package com.rahisi.zffboarding;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {
    TextView mOperatorDisplayLabel;
    TextView mOperatorDisplay;
    TextView mTicketNumberDisplay;
    TextView mPassengerNameDisplay, tv_route, tv_time, tv_date, tv_action_performed;
    EditText mTicketNumberEditor;
    Button mVerifyTicketButton;
    Button mSearchTicketButton;
    Button mLogoutButton;
    SharedPreferences mSharedPreferences, routeSharedPref;
    Dialog  manualDialog;
    String mTicketNumber, operator, pin;
    int mScannedTickets;
    String qr_departure_date, qr_departure_time, qr_destination, qr_ticket_number, qr_today_date;
    private int mTotalTickets;
    SimpleDateFormat db_date_format = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat qr_date_format = new SimpleDateFormat("dd MMMM, yyyy");
    String today_date;
    String route_departure_time, route_id, route_name, today;
    TicketDatabaseAdapter mDatabaseAdapter = new TicketDatabaseAdapter(this);

    private com.honeywell.aidc.BarcodeReader barcodeReader;
    String action, service_to_call;
    String mDepartureDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = MainActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        routeSharedPref = MainActivity.this.getSharedPreferences(Config.ROUTE_SHARED_PREF, Context.MODE_PRIVATE);
        route_departure_time = routeSharedPref.getString("departure_time", "NA");
        route_id = routeSharedPref.getString("route_id", "NA");
        System.out.println("Route id: " + route_id);
        route_name = routeSharedPref.getString("route_name", "NA");
        System.out.println("Route time: " + route_departure_time);
        action = routeSharedPref.getString("action", "NA");
        System.out.println("Action: "  + action);

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

        if(Build.MODEL.equals("EDA50K")){
            mVerifyTicketButton.setVisibility(View.GONE);
        }

        tv_route = findViewById(R.id.tv_route);
        tv_time = findViewById(R.id.tv_time);
        tv_date = findViewById(R.id.tv_date);

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

        Date today = new Date();
        String dateToStr = format.format(today);

        today_date = db_date_format.format(today);
        qr_today_date = qr_date_format.format(today);

        tv_date.setText(dateToStr);
        tv_time.setText(route_departure_time);
        tv_route.setText(route_name);

        if(action.equals("boarding")) {
            tv_action_performed.setText(getResources().getString(R.string.now_boarding));
            getDailyTickets(route_id, route_departure_time);
        }else{
            tv_action_performed.setText(getResources().getString(R.string.now_verifing));
        }

        mOperatorDisplay.setText(mSharedPreferences.getString(Config.OPERATOR_NAME_SHARED_PREF, getString(R.string.prompt_empty_values)));
        mOperatorDisplayLabel.setText("SCANNED: " + Integer.toString(mScannedTickets));
        //System.out.println("SELECTED TIME: " + mSharedPreferences.getString(Config.SHARED_DEPARTURE_DATE, "NA"));

        mVerifyTicketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("MODEL " + Build.MODEL);
                if(Build.MODEL.equals("EDA50K")){
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


        // get bar code instance from MainActivity
        barcodeReader = RouteActivity.getBarcodeObject();

        if (barcodeReader != null) {
            System.out.println("BARCODE AVAILABLE");
            // register bar code event listener
            barcodeReader.addBarcodeListener(this);
            // set the trigger mode to client control
            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                        BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
            } catch (UnsupportedPropertyException e) {
                Toast.makeText(this, "Failed to apply properties", Toast.LENGTH_SHORT).show();
            }
            // register trigger state change listener
            barcodeReader.addTriggerListener(this);
            Map<String, Object> properties = new HashMap<String, Object>();
            // Set Symbologies On/Off
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
            // Set Max Code 39 barcode length
            properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
            // Turn on center decoding
            properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
            // Enable bad read response
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
            // Apply the settings
            barcodeReader.setProperties(properties);
        }else{
            if(Build.MODEL.equals("EDA50K")) {
                Intent intent = new Intent(MainActivity.this, RouteActivity.class);
                startActivity(intent);
                finish();
            }else{
                mVerifyTicketButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(event.getCodeId().toLowerCase().equals("j")) {
                    searchTicket(event.getBarcodeData());
                }else if (event.getCodeId().toLowerCase().equals("s")){
                   extractTicketNumberFromQRCode(event.getBarcodeData());
                    System.out.println(qr_departure_date + " - " + qr_today_date);
                    if(!qr_departure_date.equals(qr_today_date)){
                        Config.doVibration(MainActivity.this);
                        mPassengerNameDisplay.setTextColor(Color.RED);
                        mPassengerNameDisplay.setText(R.string.date_invalid);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                    }else if(!qr_departure_time.equals(route_departure_time)){
                        Config.doVibration(MainActivity.this);
                        mPassengerNameDisplay.setTextColor(Color.RED);
                        mPassengerNameDisplay.setText(R.string.time_invalid);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                   }else{

                       searchTicket(qr_ticket_number);
                   }
                }
            }
        });
    }

    private void extractTicketNumberFromQRCode(String qrdata){
        try {
            JSONObject jsonObject = new JSONObject(qrdata);
            JSONObject ticket = jsonObject.getJSONObject("Ticket");
            qr_ticket_number = ticket.get("Ticket Number").toString();
            qr_departure_date =  ticket.get("Departure Date").toString();
            qr_departure_time =  ticket.get("Departure Time").toString();
            qr_destination =  ticket.get("Destination").toString();
        }catch (JSONException e) {
            System.out.println("Barcode Error: " + e);
        }
    }

    // When using Automatic Trigger control do not need to implement the
    // onTriggerEvent function
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
            // release the scanner claim so we don't get any scanner
            // notifications while paused.
            barcodeReader.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (barcodeReader != null) {
            // unregister barcode event listener
            barcodeReader.removeBarcodeListener(this);
            // unregister trigger state change listener
            barcodeReader.removeTriggerListener(this);
        }
    }

    //Logout function
    public void logout(){
        //Creating an alert dialog to confirm logout
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
        //Showing the alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showManualDialog(){
        manualDialog = new Dialog(this);
        Button btnSearch;
        //final EditText etUnit_no;
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
                if (!mTicketNumber.startsWith("ZFF")){
                    mTicketNumber = "ZFF" + mTicketNumber;
                }
                mTicketNumberDisplay.setText(getString(R.string.barcode_title_text).replace(getString(R.string.barcode_placeholder), mTicketNumber));
                if (mTicketNumber != null && mTicketNumber.length() > 0) {
                    JSONObject ticket = mDatabaseAdapter.getTicket(mTicketNumber);
                    if (ticket.has(Config.TICKET_ID)) {
                        try {
                            // Check if the ticket is already scanned
                            int isScanned = ticket.getInt(Config.TICKET_SCANNED);
                            if (isScanned > 0) {
                                mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                mVerifyTicketButton.setEnabled(true);
                                return;
                            } else if (ticket.getString(Config.TICKET_STATUS).toLowerCase().equals("cancelled")) {
                                mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
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
                        processTicket(ticket);
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
        //Getting out sharedpreferences
        SharedPreferences preferences = getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        //Getting editor
        SharedPreferences.Editor editor = preferences.edit();
        //Puting the value false for loggedin
        editor.putBoolean(Config.LOGGEDIN_SHARED_PREF, false);
        //Putting blank value to email
        editor.putString(Config.OPERATOR_SHARED_PREF, "");
        editor.putString(Config.OPERATOR_NAME_SHARED_PREF, "");
        editor.putInt(Config.SHARED_TOTAL_TICKETS, 0);
        editor.putInt(Config.SHARED_SCANNED_TICKETS, 0);
        //Saving the sharedpreferences
        editor.commit();
        mDatabaseAdapter.clearDatabase();
        //Starting login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity(intent);
        finish();
    }

    private void processTicket(JSONObject ticket) {
        System.out.println("Process Ticket");
        try {
            mPassengerNameDisplay.setText(ticket.getString(Config.PASSENGER_NAME));
            mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
            mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
            mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
            mScannedTickets += 1;
            mOperatorDisplayLabel.setText("SCANNED: " + Integer.toString(mScannedTickets));
            mDatabaseAdapter.update(ticket.getString(Config.TICKET_ID), ticket.getInt(Config.TICKET_SCAN_COUNT) + 1);
            //markTicketBoarded();
            System.out.println("Action: " +  action);
            if(action.equals("boarding")) {
                System.out.println("Boarding Ticket...");
                processOnlineTicket();
            }else{
                System.out.println("Verify Ticket...");
                verifyTicket();
            }
        } catch (JSONException ex) {
            if(action.equals("boarding")) {
                processOnlineTicket();
            }else{
                verifyTicket();
            }
        }
    }

    private void processOnlineTicket() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.FIND_TICKET_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        System.out.println("Search Ticket RESPONSE: " + response);
                        try {
                            JSONObject responseData = new JSONObject(response);
                            try {
                                if (responseData.has(Config.RESPONSE_CODE)) {
                                    if (responseData.getInt(Config.RESPONSE_CODE) == 100){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } else  if (responseData.getInt(Config.RESPONSE_CODE) == 400){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } else  if (responseData.getInt(Config.RESPONSE_CODE) == 700){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_notfound);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 200){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.GREEN);
                                        mPassengerNameDisplay.setText(R.string.ticket_valid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        //mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        //mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } else  if (responseData.getInt(Config.RESPONSE_CODE) == 600) {
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.date_invalid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 650){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.time_invalid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                    }
                                    /*else if (!route_id.equals(route)){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.route_invalid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    }else if (!depart_time.equals(route_departure_time)) {
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.time_invalid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } **/
                                }
                                //mPassengerNameDisplay.setText(responseData.getString(Config.PASSENGER_NAME));
                                //mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
                                //mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
                                //mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                mScannedTickets += 1;
                                mOperatorDisplayLabel.setText("SCANNED: " + Integer.toString(mScannedTickets));

                                //markTicketBoarded();
                            } catch (JSONException ex) {
                                System.out.println("JSON ERROR: " + ex);
                                mPassengerNameDisplay.setText(R.string.passenger_not_found);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                            }

                        } catch (JSONException e) {
                            System.out.println("TICKET ERRORS: " + e.getMessage());
                            e.printStackTrace();
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
                        //You can handle error here if you want
                        System.out.println("VOLLEY: " + error.toString());
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
                //Adding parameters to request
                params.put("reference", mTicketNumber);
                params.put("time", route_departure_time);
                params.put("date", today_date);
                params.put("operator", operator);
                params.put("pin", pin);

                System.out.println("SEARCH REQUEST: " + params);
                //returning parameter
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                // add headers <key,value>
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("CREDENTIALS: " + auth + "\n");
                headers.put("Authorization", auth);
                return headers;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                40000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );
        requestQueue.add(stringRequest);
    }

    private void verifyTicket() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.VERIFY_TICKET_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        System.out.println("Verify Ticket RESPONSE: " + response);
                        try {
                            JSONObject responseData = new JSONObject(response);
                            try {
                                if (responseData.has(Config.RESPONSE_CODE)) {
                                    if (responseData.getInt(Config.RESPONSE_CODE) == 100){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.never_scanned);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } else  if (responseData.getInt(Config.RESPONSE_CODE) == 400){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 500){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_expired);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 300){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_expired);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 350){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_pending);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } else  if (responseData.getInt(Config.RESPONSE_CODE) == 700){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.ticket_notfound);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                        return;
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 200){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.GREEN);
                                        mPassengerNameDisplay.setText(R.string.scanned);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        //mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        //mVerifyTicketButton.setEnabled(true);
                                        return;
                                    } else  if (responseData.getInt(Config.RESPONSE_CODE) == 600) {
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.date_invalid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                    }else  if (responseData.getInt(Config.RESPONSE_CODE) == 650){
                                        Config.doVibration(MainActivity.this);
                                        mPassengerNameDisplay.setTextColor(Color.RED);
                                        mPassengerNameDisplay.setText(R.string.time_invalid);
                                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                                        mVerifyTicketButton.setEnabled(true);
                                    }

                                }
                                mScannedTickets += 1;
                                mOperatorDisplayLabel.setText("SCANNED: " + Integer.toString(mScannedTickets));
                            } catch (JSONException ex) {
                                System.out.println("JSON ERROR: " + ex);
                                mPassengerNameDisplay.setText(R.string.passenger_not_found);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                            }
                        } catch (JSONException e) {
                            System.out.println("TICKET ERRORS: " + e.getMessage());
                            e.printStackTrace();
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
                        //You can handle error here if you want
                        System.out.println("VOLLEY: " + error.toString());
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
                //Adding parameters to request
                params.put("reference", mTicketNumber);
                params.put("time", route_departure_time);
                params.put("date", today_date);
                params.put("operator", operator);
                params.put("pin", pin);

                System.out.println("VERIFY REQUEST: " + params);
                //returning parameter
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                // add headers <key,value>
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("CREDENTIALS: " + auth + "\n");
                headers.put("Authorization", auth);
                return headers;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                40000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );
        requestQueue.add(stringRequest);
    }

    private void markTicketBoarded() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.UPDATE_TICKET_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject responseData = new JSONObject(response);
                            System.out.println(responseData.toString());
                            /*try {
                                mPassengerNameDisplay.setText(responseData.getString(Config.PASSENGER_NAME));
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_tick_box_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(24, 106, 59));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT,150);
                            } catch (JSONException ex) {
                                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE,150);
                                mPassengerNameDisplay.setText(R.string.passenger_not_found);
                                mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                                mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                                mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_failure_display_border));
                            }*/

                        } catch (JSONException e) {
                            System.out.println("TICKET ERRORS MARK TICKET: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //You can handle error here if you wan
                        System.out.println(error.toString());
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                String operator = mSharedPreferences.getString(Config.OPERATOR_SHARED_PREF, "0");
                Map<String,String> params = new HashMap<>();
                //Adding parameters to request
                params.put("ticket_id", mTicketNumber);
                params.put("boarding_status", "YES");
                params.put("operator_id", operator);
                params.put("device_id", getIMEI());
                params.put(Config.API_KEY_NAME, Config.API_KEY);

                //returning parameter
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                // add headers <key,value>
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("CREDENTIALS: " + auth + "\n");
                headers.put("Authorization", auth);
                return headers;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                40000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );
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
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_L,150);

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
                    // Check if the ticket is already scanned
                    int isScanned = ticket.getInt(Config.TICKET_SCANNED);
                    if (isScanned > 0) {
                        mPassengerNameDisplay.setText(R.string.ticket_already_scanned);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                        mVerifyTicketButton.setText(R.string.button_scan_sticker);
                        mVerifyTicketButton.setEnabled(true);
                        return;
                    } else if (ticket.getString(Config.TICKET_STATUS).toLowerCase().equals("cancelled")) {
                        mPassengerNameDisplay.setText(R.string.ticket_cancelled);
                        mPassengerNameDisplay.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.icons8_close_window_48, 0, 0);
                        mTicketNumberDisplay.setTextColor(Color.rgb(169, 50, 38));
                        mTicketNumberDisplay.setBackground(getResources().getDrawable(R.drawable.ticket_success_display_border));
                     //   toneGen1.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE,150);
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

                if(action.equals("boarding")) {
                    System.out.println("Boarding Ticket...");
                    processTicket(ticket);
                }else{
                    System.out.println("Verify Ticket...");
                    verifyTicket();
                }

            } else {
                if(action.equals("boarding")) {
                    System.out.println("Boarding Ticket...");
                    processOnlineTicket();
                }else{
                    System.out.println("Verify Ticket...");
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
            // We do not have this permission. Let's ask the user
            //Activity.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
            IMEI = "IMEI UNAVAILABLE";
        }else {
            TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            IMEI = tm.getDeviceId();

        }
        return IMEI;
    }


    //option menu at the top
    @Override
    public boolean onCreateOptionsMenu( Menu menu) {
        //Adding our menu_main to toolbar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuLogout) {
            //calling logout method when the logout button is clicked
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
                        //System.out.println("BOOKINGS: " + response);
                        JSONArray tickets;
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            tickets = jsonObject.getJSONArray("tickets");
                            mDatabaseAdapter.clearDatabase();

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
                                    if (id > 1) {
                                        mTotalTickets += 1;
                                    }
                                    System.out.println("TICKET: " + ticket.getString("ticket_number"));
                                }
                        } catch (JSONException e) {
                            System.out.println("TICKETS ERRORS: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //You can handle error here if you wan

                        //Toast.makeText(mContext, error.toString(), Toast.LENGTH_LONG).show();
                        System.out.println(error.toString());
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String,String> params = new HashMap<>();
                //Adding parameters to request
                params.put("date", formattedDate);
                params.put("route", route_id);
                params.put("time", route_departure_time);
                params.put("operator", operator);
                params.put("pin", pin);



                //returning parameter
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                // add headers <key,value>
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                40000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );
        requestQueue.add(stringRequest);
    }

}
