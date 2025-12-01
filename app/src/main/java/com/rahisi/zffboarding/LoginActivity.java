package com.rahisi.zffboarding;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameField;
    private EditText passwordField;
    private Button loginButton;
    private Button forgotPasswordButton;
    private Context mContext = LoginActivity.this;
    private View parentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Locale locale = new Locale(Config.DEFAULT_LANGUAGE);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        parentLayout = findViewById(android.R.id.content);

        usernameField = findViewById(R.id.operator_code);
        passwordField = findViewById(R.id.operator_key);
        loginButton = findViewById(R.id.operator_sign_in_button);
        forgotPasswordButton = findViewById(R.id.operator_forgot_password);

        passwordField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard();
                if (isOnline(mContext)) login();
                else showSnackBar("No internet connection. Connect to continue.");
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            if (isOnline(mContext)) login();
            else showSnackBar("No internet connection");
        });

        forgotPasswordButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
        });
    }

    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void showSnackBar(String message) {
        Snackbar snackbar = Snackbar.make(parentLayout, message, Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.BOTTOM;
        view.setLayoutParams(params);
        view.setBackgroundColor(0xFFe56b6f);
        snackbar.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences(Config.SHARED_PREF_NAME, MODE_PRIVATE);
        boolean loggedIn = sp.getBoolean(Config.LOGGEDIN_SHARED_PREF, false);
        if (loggedIn) {
            startActivity(new Intent(mContext, MainActivity.class));
            finish();
        }
    }

    private void login() {
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        System.out.println("Operator Username: " + username + "Operator Password: " + password);
        if (username.isEmpty() || password.isEmpty()) {
            showSnackBar("Both username and password are required.");
            return;
        }

        ProgressDialog loading = ProgressDialog.show(mContext, "Signing in...", "Please wait...", false, false);
        StringRequest loginReq = new StringRequest(Request.Method.POST, Config.LOGIN_URL,
                response -> {
                    System.out.println("Operator Login Response: " + response);
                    loading.dismiss();
                    try {
                        JSONObject root = new JSONObject(response).getJSONObject("response");
                        int code = root.getInt("code");
                        String message = root.optString("message", "No message");
                        if (code == 200) {
                            JSONObject user = root.getJSONObject("data").getJSONObject("user_details");
                            String loginId = user.optString("login_credential_id", "");
                            String userId = user.optString("user_id", "");
                            String usernameResp = user.optString("username", "");
                            String domain = user.optString("domain", "");ge
                            String token = user.optString("token", "");
                            saveLoginInfo(loginId, userId, usernameResp, domain, token);
                            SharedPreferences preferences = getSharedPreferences(Config.SHARED_PREF_NAME, MODE_PRIVATE);
                            preferences.edit().putString(Config.KEY_OPERATOR, username).apply();
                            fetchRoutesAndOpenMain();
                        } else if (code == 100) {
                            showSnackBar(message);
                        } else {
                            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(mContext, "Invalid server response", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                },

                error -> {
                    loading.dismiss();
                    Toast.makeText(mContext, "Server error: " + error.toString(), Toast.LENGTH_LONG).show();
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                params.put(Config.API_KEY_NAME, Config.API_KEY);
                System.out.println("Operator Login Params: " + params);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("Operator Login Headers: " + auth);
                headers.put("Authorization", auth);
                return headers;
            }
        };

        loginReq.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(loginReq);
    }

    private void fetchRoutesAndOpenMain() {
        ProgressDialog loading = ProgressDialog.show(this, "Loading routes...", "Please wait...", false, false);
        StringRequest req = new StringRequest(Request.Method.POST, Config.SPLASH_URL,
                response -> {
                    System.out.println("Operator Route Response: " + response);
                    loading.dismiss();
                    try {
                        JSONObject root = new JSONObject(response).getJSONObject("response");
                        int code = root.optInt("code", 0);
                        String message = root.optString("message", "No message");
                        if (code == 200 && root.has("data")) {
                            JSONArray routes = root.getJSONArray("data");
                            if (routes.length() == 0) {
                                Toast.makeText(this, "No routes available from server.", Toast.LENGTH_LONG).show();
                            } else {
                                saveRoutesJson(routes.toString());
                            }
                        } else {
                            Toast.makeText(this, "Failed to load routes: " + message, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid route response", Toast.LENGTH_LONG).show();
                    }
                    startActivity(new Intent(this, ModeActivity.class));
                    finish();
                },
                error -> {
                    loading.dismiss();
                    Toast.makeText(this, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String credentials = Config.API_USER_NAME + ":" + Config.API_PASSWORD;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                System.out.println("Splash Headers Authorization: " + auth);
                headers.put("Authorization", auth);
                return headers;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(req);
    }

    private void saveRoutesJson(String json) {
        SharedPreferences sp = getSharedPreferences(Config.SHARED_PREF_NAME, MODE_PRIVATE);
        sp.edit().putString(Config.PREF_ROUTES_JSON, json).apply();
    }

    private void saveLoginInfo(String loginId, String userId, String username, String domain, String token) {
        SharedPreferences sp = getSharedPreferences(Config.SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(Config.LOGGEDIN_SHARED_PREF, true);
        ed.putString("login_credential_id", loginId);
        ed.putString("user_id", userId);
        ed.putString("username", username);
        ed.putString("domain", domain);
        ed.putString("token", token);
        ed.putLong("login_time", System.currentTimeMillis());
        ed.apply();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(passwordField.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}
