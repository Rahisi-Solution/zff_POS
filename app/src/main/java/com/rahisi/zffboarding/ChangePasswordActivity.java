package com.rahisi.zffboarding;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {
    private ProgressDialog searchDialog;
    View parentLayout;
    String token;
    MaterialButton change_password_button;
    private Dialog checkedOutDialog;
    TextInputEditText old_password;
    TextInputEditText new_password;
    TextInputEditText confirm_password;
    TicketDatabaseAdapter mDatabaseAdapter = new TicketDatabaseAdapter(this);
    String operator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Locale locale = new Locale(Config.DEFAULT_LANGUAGE);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        setContentView(R.layout.activity_change_password);

        old_password = findViewById(R.id.old_password_field);
        new_password = findViewById(R.id.new_password_field);
        confirm_password = findViewById(R.id.confirm_password_field);
        change_password_button = findViewById(R.id.change_password_button);

        parentLayout = findViewById(android.R.id.content);
        prepareData();

        change_password_button.setOnClickListener(v -> {
            if(String.valueOf(old_password.getText()).trim().isEmpty()){
                showSnackBar("Please enter old password");
            } else if(String.valueOf(new_password.getText()).trim().isEmpty()){
                showSnackBar("Please enter new password");
            }else if(String.valueOf(confirm_password.getText()).trim().isEmpty()){
                showSnackBar("Please enter confirm password");
            }else if(!String.valueOf(new_password.getText()).trim().equals(String.valueOf(confirm_password.getText()).trim())){
                showSnackBar("New and confirm password mismatch");
            } else{
                if(isOnline(this)){
                    changePin(String.valueOf(old_password.getText()), String.valueOf(new_password.getText()), String.valueOf(confirm_password.getText()));
                } else {
                    showSnackBar("You are offline connect to the internet to continue");
                }

            }
        });

    }

    public static boolean isOnline(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } else {
            return false;
        }
    }

//    private void prepareData(){
//        SharedPreferences preferences = ChangePasswordActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
//        token = preferences.getString(Config.API_KEY, "n.a");
//        System.out.println("Change Password Token: " + token);
//    }

    private void changePin(String oldPassword, String newPassword, String confirmPassword){
        searchDialog = ProgressDialog.show(ChangePasswordActivity.this, "Processing", "Please wait...");

        StringRequest request = new StringRequest(Request.Method.POST, Config.CHANGE_PASSWORD,
                response -> {
                    searchDialog.dismiss();
                    System.out.println("Change password response : " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONObject applicantResponse = jsonObject.getJSONObject("response");
                        String code = applicantResponse.getString("code");
                        String message = applicantResponse.getString("message");

                        if(code.equals("200")) {
                            showSuccessDialog(message);
                        } else {
                            showSnackBar("Failed: " + message);
                        }

                    } catch (JSONException exception) {
                        showSnackBar("Request Error: " + exception);
                    }
                },
                error -> {
                    searchDialog.dismiss();
                    if(String.valueOf(error).equals("com.android.volley.NoConnectionError: java.net.UnknownHostException: Unable to resolve host \"earrival.rahisi.co.tz\": No address associated with hostname")){
                        System.out.println("The error HERE :" + error);
                        showSnackBar("Network Error please check your Internet Bandwith");
                    } else {
                        showSnackBar(String.valueOf(error));
                    }
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("authorization", "Bearer " + token);
                params.put("password", oldPassword);
                params.put("new_password", newPassword);
                params.put("retype_password", confirmPassword);
                System.out.println("Change Password Parameters: " + params);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }
        };
        RequestQueue queue = Volley.newRequestQueue(this);
        request.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void showSuccessDialog(String displayMessage) {
        checkedOutDialog = new Dialog(this);
        checkedOutDialog.setCanceledOnTouchOutside(false);
        checkedOutDialog.setContentView(R.layout.success_dialog);

        TextView message = checkedOutDialog.findViewById(R.id.name_title);
        MaterialButton dismissButton = checkedOutDialog.findViewById(R.id.agree_button);

        message.setText(displayMessage);

        dismissButton.setOnClickListener(view -> {
            checkedOutDialog.dismiss();
            signout();

        });
        checkedOutDialog.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        checkedOutDialog.show();
    }

    void showSnackBar(String displayMessage) {
        Snackbar snackbar;
        snackbar = Snackbar.make(parentLayout, displayMessage, Snackbar.LENGTH_SHORT);
        View snackView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackView.getLayoutParams();
        params.gravity = Gravity.TOP;
        snackView.setBackgroundColor(0xFFe56b6f);
        snackbar.show();
    }


    private void prepareData(){
        SharedPreferences preferences = ChangePasswordActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        token = preferences.getString(Config.AUTH_TOKEN, "n.a");
        operator = preferences.getString(Config.KEY_OPERATOR, "NA");
        System.out.println("Token: " + token);
    }

    private void signout(){
        final ProgressDialog loading;
        loading = ProgressDialog.show(this, "Logout...", "Please wait...", false, false);
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
                        Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
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
                        Toast.makeText(ChangePasswordActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
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

}