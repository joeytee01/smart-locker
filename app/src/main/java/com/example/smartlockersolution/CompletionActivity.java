package com.example.smartlockersolution;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CompletionActivity extends AppCompatActivity {

    private int countdown = 10;
    private Handler handler = new Handler();
    private Runnable countdownRunnable;
    private Button backButton;
    private TextView dateTimeTextView,profileName;
    private ImageView profileIcon;
    private final Handler datehandler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            datehandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        datehandler.post(updateTimeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        datehandler.removeCallbacks(updateTimeRunnable);
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completion);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        profileName = findViewById(R.id.profileNameTextView);
        profileIcon = findViewById(R.id.profileIcon);

        TempDataHolder.setChangeLockerSize(false);
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileName.setText("John Doe, Investigation Officer");
        }

        else if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.CASE_STORE_OFFICER){
            profileName.setText("Agatha, Case Store Officer");
        }

        else{
            profileIcon.setVisibility(View.GONE);
            profileName.setText("");
        }
        // Update date and time dynamically
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);

        dateTimeTextView.setText(currentDateAndTime);

        TextView completionText = findViewById(R.id.completionText);
        backButton = findViewById(R.id.backButton);

        // Set the initial text
        backButton.setText("Back to home (10)");
        if(TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.SELF_ENROLL){
            completionText.setText("Enrollment Successful!");
        }

        // Countdown logic
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown > 0) {
                    backButton.setText("Back to home (" + countdown + ")");
                    countdown--;
                    handler.postDelayed(this, 1000);
                } else {
                    navigateToHome();
                }
            }
        };

        // Start countdown
        handler.postDelayed(countdownRunnable, 1000);

        // If user clicks the button, navigate immediately
        backButton.setOnClickListener(v -> navigateToHome());
    }

    private void navigateToHome() {
        handler.removeCallbacks(countdownRunnable); // Stop countdown if navigating manually
        Intent intent = new Intent(CompletionActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
