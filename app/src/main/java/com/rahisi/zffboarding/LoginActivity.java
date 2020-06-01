package com.rahisi.zffboarding;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A login screen that offers login via code/key.
 */
public class LoginActivity extends AppCompatActivity {
    // UI references.
    private EditText mOperatorCodeView;
    private EditText mOperatorKeyView;
    private Button mOperatorSignInButton;
    private boolean loggedIn = false;
    private Context mContext = LoginActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Locale locale = new Locale(Config.DEFAULT_LANGUAGE);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set up the login form.
        mOperatorCodeView = findViewById(R.id.operator_code);
        mOperatorKeyView = findViewById(R.id.operator_key);
        mOperatorKeyView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    hideOnScreenKeyboard();
                    login();
                }
                return false;
            }
        });

        mOperatorSignInButton = findViewById(R.id.operator_sign_in_button);
        mOperatorSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //In onresume fetching value from sharedpreference
        SharedPreferences sharedPreferences = getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        //Fetching the boolean value form sharedpreferences
        loggedIn = sharedPreferences.getBoolean(Config.LOGGEDIN_SHARED_PREF, false);
        //loggedIn = true;
        //If we will get true
        if(loggedIn){
            //We will start the Profile Activity
            Intent intent = new Intent(mContext, MainActivity.class);
            startActivity(intent);
        }
    }

    private void login(){
        //Getting values from edit texts
        final String operator = mOperatorCodeView.getText().toString().trim();
        final String pin = mOperatorKeyView.getText().toString().trim();

        final ProgressDialog loading;
        loading = ProgressDialog.show(mContext, "Signing in...", "Please wait...", false, false);

        //Creating a string request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.LOGIN_URL,
                new Response.Listener<String>() {
                    // required for operator
                    String result_status = "";
                    String operator_name = "";
                    JSONArray routes;
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        loading.dismiss();
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray operatorResult = jsonObject.getJSONArray("operator");
                            routes = jsonObject.getJSONArray("routes");
                            JSONObject operatorData = operatorResult.getJSONObject(0);
                            result_status = operatorData.getString(Config.KEY_STATUS);
                            operator_name = operatorData.getString(Config.KEY_OPERATOR_NAME);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            System.out.println("MESSAGE ERROR " + e.getMessage());
                        }

                        if(result_status.equalsIgnoreCase(Config.LOGIN_SUCCESS)){
                            //Creating a shared preference
                            SharedPreferences sharedPreferences = mContext.getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                            //Creating editor to store values to shared preferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            //Adding values to editor
                            editor.putBoolean(Config.LOGGEDIN_SHARED_PREF, true);
                            editor.putString(Config.OPERATOR_NAME_SHARED_PREF, operator_name);
                            editor.putString("pin", pin);
                            editor.putString("operator", operator);
                            editor.putString("routes", routes.toString());
                            //Saving values to editor
                            editor.commit();
                            //Starting  activity
                            Intent intent = new Intent(mContext, ModeActivity.class);
                            intent.putExtra("UID","LoginActivity");
                            startActivity(intent);
                            finish();
                        }else{
                            //If the server response is not success
                            //Displaying an error message on toast
                            Toast.makeText(mContext, R.string.failed_login, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("ERROR: " + error.getMessage());
                        //You can handle error here if you wan
                        loading.dismiss();
                        Toast.makeText(mContext, error.toString(), Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String,String> params = new HashMap<>();
                //Adding parameters to request
                params.put("operator_no", operator);
                params.put("operator_pin", pin);
                params.put("user_device_id", "1");
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
                //System.out.println("CREDENTIALS: " + auth + "\n");
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



    @Override
    protected void onPause() {
        super.onPause();
        System.gc();
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    @Override
    public void onBackPressed() {

    }

    public void hideOnScreenKeyboard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mOperatorCodeView.getWindowToken(),
                InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }


}

