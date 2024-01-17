package com.rahisi.zffboarding;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Config {
    static String  app_ip = "https://booking.zff.co.tz/api/scanner/";
    static String logsEndpoint = "https://172.19.255.4/api/";

   // static String  app_ip = "http://172.16.10.143:8052/api/scanner/";
   // static String logsEndpoint = "http://172.16.10.143:8052/api/";

    //static String  app_ip = "https://demo.zanzibarfastferries.com/api/scanner/";
    public static final String LOGIN_URL = app_ip + "scanner_login.php";
    public static final String BOOKING_URL = app_ip + "scanner_fetch_bookings.php";
    public static final String UPDATE_TICKET_URL = app_ip + "customerboardingchecking";
    public static final String FIND_TICKET_URL = app_ip + "scanner_get_ticket.php";
    public static final String VERIFY_TICKET_URL = app_ip + "scanner_verify_ticket.php";
    public static final String REPORT_ERROR_LOG = logsEndpoint + "issues_reporting/error_reporting.php";


    public static final String API_USER_NAME = "goandroy";
    public static final String API_PASSWORD = "12345";
    public static final String API_KEY = "D6H8SKKRL79RJ4WWP5LASYNGJ";
    public static final String API_KEY_NAME = "API-KEY";

    //Keys for operator id and password as defined in our $_POST['key'] in login.php
    public static final String KEY_OPERATOR = "operator";
    public static final String KEY_PASSWORD = "password";

    //Keys for operator id and password as defined in our $_POST['key'] in login.php
    public static final String KEY_OPERATOR_NAME = "operator_name";
    public static final String KEY_STATUS = "status";
    public static final String USER_PORT_NAME = "user_port_id";
    public static final String USER_DEVICE_NAME = "user_device_id";

    //If server response is equal to this that means login is successful
    public static final String LOGIN_SUCCESS = "success";

    //Keys for Sharedpreferences
    //This would be the name of our shared preferences
    public static final String SHARED_PREF_NAME = "operator_login";
    //This would be used to store the operator id of current logged in user
    public static final String OPERATOR_SHARED_PREF = "operator";
    public static final String ROUTE_SHARED_PREF = "route";
    //We will use this to store the boolean in sharedpreference to track user is loggedin or not
    public static final String LOGGEDIN_SHARED_PREF = "loggedin";
    // Used to store the name of the currently logged in operator
    public static final String OPERATOR_NAME_SHARED_PREF = "operator_name";
    public static final String OPERATOR_TYPE_SHARED_PREF = "operator_type";

    // OFFLINE DATABASE CONFIGURATIONS
    public static final String TICKET_DATABASE_NAME = "zanfastTicketsDb";
    public static final String TICKET_TABLE_NAME = "zanfastTikets";
    public static final int TICKET_DATABASE_VERSION = 3;
    public static final String TICKET_ID = "id";
    public static final String BOOKING_ID = "booking_id";
    public static final String TICKET_NUMBER = "ticket_id";
    public static final String PASSENGER_NAME = "name";
    public static final String PASSENGER_AGE = "age";
    public static final String DEPARTURE_DATE = "journey_date";
    public static final String DEPARTURE_TIME = "departure_time";
    public static final String BOARDING = "boarding";
    public static final String TICKET_SCANNED = "ticket_scanned";
    public static final String TICKET_STATUS = "status";
    public static final String RESPONSE_CODE = "code";
    public static final String TICKET_SCAN_COUNT = "scan_count";

    public static final String SHARED_TOTAL_TICKETS = "total_tickets";
    public static final String SHARED_SCANNED_TICKETS = "scanned_tickets";
    public static final String SHARED_DEPARTURE_DATE = "departure_date";
    public static final String SHARED_USER_PORT_NAME = "user_port_id";
    public static final String SHARED_USER_DEVICE_NAME = "user_device_id";

    public static final String DEFAULT_LANGUAGE = "en";

    public  static String getSerialNo(){
        String serial = Build.SERIAL;
        return serial;
    }

    public static void doVibration(Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            v.vibrate(500);
        }
    }
}
