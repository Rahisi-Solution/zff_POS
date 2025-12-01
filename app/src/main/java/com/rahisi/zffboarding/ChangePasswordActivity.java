package com.rahisi.zffboarding;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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

    private void prepareData(){
        SharedPreferences preferences = ChangePasswordActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        token = preferences.getString(Config.API_KEY, "n.a");
        System.out.println("Change Password Token: " + token);
    }

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
            SharedPreferences preferences = ChangePasswordActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit().clear().apply();
            Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
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

}