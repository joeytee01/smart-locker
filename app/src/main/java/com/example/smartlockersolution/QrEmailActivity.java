package com.example.smartlockersolution;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QrEmailActivity extends AppCompatActivity {
    private TextView dateTimeTextView, headerTitle, profileName, timerTextView;
    private ImageView profileIcon;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateTimeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable); // Stop updating when activity is not visible
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_email); // Ensure the correct layout is set

        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        profileIcon = findViewById(R.id.profileIcon);
        TempDataHolder.attachTimerTextView(timerTextView, this);
        // Find the ImageButton
        ImageButton scanImageButton = findViewById(R.id.scanImageButton);

        if(TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR){
            profileName.setText("John Doe, Investigation Officer");
            headerTitle.setText("Withdraw");
        }
        else{
            profileIcon.setVisibility(View.GONE);
            profileName.setText("");
            headerTitle.setText("Self Enrollment");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);

        // Set click listener to open new activity
        scanImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TempDataHolder.getCurrentRole()== TempDataHolder.UserRole.SELF_ENROLL) {
                    Intent intent = new Intent(QrEmailActivity.this, DivisionPass.class);
                    intent.putExtra("email", getIntent().getStringExtra("email"));
                    startActivity(intent);
                }
                else{
                    Intent intent = new Intent(QrEmailActivity.this, CameraCaptureActivity.class);
                    startActivity(intent);
                }

            }
        });
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
    }
}
