package com.rahisi.zffboarding;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class LogExceptions {
    public static void saveLog(String file, String line, String message, String trace, Context context) throws JSONException {

        StringRequest request = new StringRequest(Request.Method.POST, Config.REPORT_ERROR_LOG, response -> {
            System.out.println("Save log Response: " + response);
        }, error -> {
            System.out.println("Save log Error: " + error);
        }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("type", String.valueOf(8));
                params.put("file", file);
                params.put("line", line);
                params.put("message", message);
                params.put("trace", trace);
                params.put("project_code", "ZFF001");
                params.put("url", Config.REPORT_ERROR_LOG);
                params.put("occurrence_date", "2022-10-12");
                System.out.println("Report Log Params: " + params);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        request.setRetryPolicy(new DefaultRetryPolicy(40000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }
}
