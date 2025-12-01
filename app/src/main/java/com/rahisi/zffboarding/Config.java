package com.rahisi.zffboarding;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Config {
    /* Logs Endpoint */
    static String logsEndpoint = "https://172.19.255.4/api/";

    // static String app_ip = "https://booking.zff.co.tz/api/scanner/";

    // static String app_ip = "https://demo.zanzibarfastferries.com/api/scanner/";

    // static String app_ip = "https://ferries.rahisi.co.tz/api/scanner/";

    // static String app_ip = "http://172.16.10.217:2019/scanner/";  // Vicent
    static String app_ip = "http://172.16.10.171:7800/scanner/";     // John

    /* API Routes */
    public static final String SPLASH_URL = app_ip + "splash";
    public static final String LOGIN_URL = app_ip + "scanner_login.php";
    public static final String LOGOUT_URL = app_ip + "scanner_logout.php";
    public static final String CHANGE_PASSWORD = app_ip + "api/change_password";
    public static final String RESET_PASSWORD = app_ip + "api/reset_password";
    public static final String BOOKING_URL = app_ip + "scanner_fetch_bookings.php";
    public static final String UPDATE_TICKET_URL = app_ip + "customerboardingchecking";
    public static final String FIND_TICKET_URL = app_ip + "scanner_get_ticket.php";
    public static final String VERIFY_TICKET_URL = app_ip + "scanner_verify_ticket.php";
    public static final String REPORT_ERROR_LOG = logsEndpoint + "issues_reporting/error_reporting.php";

    public static final String API_USER_NAME = "goandroy";
    public static final String API_PASSWORD = "12345";
    public static final String API_KEY = "D6H8SKKRL79RJ4WWP5LASYNGJ";
    public static final String API_KEY_NAME = "API-KEY";
    public static final String KEY_OPERATOR = "operator";
    public static final String KEY_OPERATOR_NAME = "operator_name";
    public static final String KEY_STATUS = "status";
    public static final String LOGIN_SUCCESS = "success";
    public static final String LOGOUT_SUCCESS = "You have successfully logged out";
    public static final String SHARED_PREF_NAME = "operator_login";
    public static final String ROUTE_SHARED_PREF = "route_prefs";
    public static final String LOGGEDIN_SHARED_PREF = "loggedin";
    public static final String OPERATOR_SHARED_PREF = "operator";
    public static final String OPERATOR_NAME_SHARED_PREF = "operator_name";
    public static final String SHARED_PREF_NAME_ROUTES = "zff_prefs";
    public static final String PREF_ROUTES_JSON = "routes_json";
    public static final String PREF_SELECTED_ROUTE_ID = "selected_route_id";
    public static final String PREF_SELECTED_ROUTE_NAME = "selected_route_name";
    public static final String PREF_SELECTED_ROUTE_TIME = "selected_route_time";
    public static final String PREF_SELECTED_ACTION = "selected_action";
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
    public static final String DEFAULT_LANGUAGE = "en";

    public static String getSerialNo() {
        return Build.SERIAL;
    }

    public static void doVibration(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }
    }
}
