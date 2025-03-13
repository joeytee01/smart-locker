package com.example.smartlockersolution;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check device configuration—using smallestScreenWidthDp
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            // Tablet: 600dp and above is typically considered a tablet.
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Phone: less than 600dp.
            startActivity(new Intent(this, PhoneMainActivity.class));
        }
        finish();
    }
}

