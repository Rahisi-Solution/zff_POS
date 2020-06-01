package com.rahisi.zffboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class ModeActivity extends AppCompatActivity {

    Button btn_boarding, btn_verify;
    public ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);
        progressBar = findViewById(R.id.progressBar);

        btn_boarding = findViewById(R.id.btn_boarding);
        btn_boarding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                //Starting  activity
                Intent intent = new Intent(ModeActivity.this, RouteActivity.class);
                intent.putExtra("action","boarding");
                startActivity(intent);
                finish();

            }
        });

        btn_verify = findViewById(R.id.btn_verify);
        btn_verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Starting  activity
                progressBar.setVisibility(View.VISIBLE);
                Intent intent = new Intent(ModeActivity.this, RouteActivity.class);
                intent.putExtra("action","verify");
                startActivity(intent);
                finish();
            }
        });

    }

}
