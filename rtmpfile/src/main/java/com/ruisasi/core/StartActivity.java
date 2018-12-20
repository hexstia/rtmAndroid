package com.ruisasi.core;


import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;

import com.wangheart.rtmpfile.R;


public class StartActivity extends Activity {
    public static final String TAG = "StartActivity";
    private Button btn_start;
    private Button btn_stop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_act);
        btn_start = findViewById(R.id.start);
        btn_stop = findViewById(R.id.stop);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new  Intent(StartActivity.this,MainService.class);
                startService(i);
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new  Intent(StartActivity.this,MainService.class);
                stopService(i);

            }
        });

    }
}

