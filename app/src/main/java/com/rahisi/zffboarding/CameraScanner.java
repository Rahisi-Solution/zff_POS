package com.rahisi.zffboarding;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CameraScanner extends CaptureActivity implements DecoratedBarcodeView.TorchListener {
    private DecoratedBarcodeView mBarcodeScannerView;


    RequestQueue mQueue;
    SharedPreferences mSharedPreferences;
    String mMobile;
    String mPin;

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_camera_scanner);

        mBarcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        mBarcodeScannerView.setTorchListener(this);


        mQueue = Volley.newRequestQueue(CameraScanner.this);


        return (DecoratedBarcodeView)findViewById(R.id.zxing_barcode_scanner);
    }

    public void onCancelActivity(View view) {
        onBackPressed();
    }


    @Override
    public void onTorchOn() {

    }

    @Override
    public void onTorchOff() {

    }
}
