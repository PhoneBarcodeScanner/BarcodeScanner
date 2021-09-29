package com.example.timbersmartbarcodescanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvTitle;
    private TextView tvDesc;
    private SeekBar sbTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tvTitle);
        tvDesc = findViewById(R.id.tvDesc);
        sbTime = findViewById(R.id.sbTime);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (tvTitle != null) {
            tvTitle.setText("Setting");
        }
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.mipmap.ic_back);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> finish());


        SharedPreferences sp = getSharedPreferences("Scan interval", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        sbTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int ii = progress + 1;
                tvDesc.setText("Scan interval time is " + ii + "s");
                editor.putLong("Scan interval time", ii * 1000);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


}