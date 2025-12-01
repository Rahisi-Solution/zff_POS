package com.rahisi.zffboarding;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class ResetPasswordActivity extends AppCompatActivity {
    private ProgressDialog searchDialog;
    private Dialog checkedOutDialog;
    View parentLayout;
    EditText operatorNumber;
    Button resetButton;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        prepareData();

        parentLayout = findViewById(android.R.id.content);
        operatorNumber = findViewById(R.id.reset_field);
        resetButton = findViewById(R.id.reset_password_button_field);

        resetButton.setOnClickListener(v -> {
            if(String.valueOf(operatorNumber.getText()).trim().isEmpty()){
                showSnackBar("Please enter operator number");
            } else {
                if(isOnline(this)){
                    resetPin(String.valueOf(operatorNumber.getText()));
                } else {
                    showSnackBar("Looks like you are offline please connect to internet to continue");
                }
            }
        });

    }

    private void prepareData(){
        SharedPreferences preferences = ResetPasswordActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        token = preferences.getString(Config.API_KEY, "n.a");
        System.out.println("Reset Password Token: " + token);
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

    private void resetPin(String username){
        searchDialog = ProgressDialog.show(ResetPasswordActivity.this, "Processing", "Please wait...");

        StringRequest request = new StringRequest(Request.Method.POST, Config.RESET_PASSWORD,
                response -> {
                    searchDialog.dismiss();
                    System.out.println("Reset password response: " + response);
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
                        showSnackBar("Request Error: " + "Incorrect Officer Number");
                    }
                },
                error -> {
                    searchDialog.dismiss();
                    if(String.valueOf(error).equals("com.android.volley.NoConnectionError: java.net.UnknownHostException: Unable to resolve host \"earrival.rahisi.co.tz\": No address associated with hostname")){
                        System.out.println("The error HERE:" + error);
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
                params.put("username", username);
                System.out.println("Reset Password Parameters: " + params);
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

    void showSnackBar(String displayMessage) {
        Snackbar snackbar;
        snackbar = Snackbar.make(parentLayout, displayMessage, Snackbar.LENGTH_SHORT);
        View snackView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackView.getLayoutParams();
        params.gravity = Gravity.TOP;
        snackView.setBackgroundColor(0xFFe56b6f);
        snackbar.show();
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
            SharedPreferences preferences = ResetPasswordActivity.this.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit().clear().apply();
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        checkedOutDialog.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        checkedOutDialog.show();
    }



}