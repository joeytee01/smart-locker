package com.example.smartlockersolution;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private Button investigatorButton, caseStoreOfficerButton, selfEnrollmentButton;
    private TextView dateTimeTextView, helpTextView, watchTutorialTextView; // For displaying date and time
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime(); // Update time
            handler.postDelayed(this, 1000); // Repeat every second
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        TempDataHolder.resetTimer();
        handler.post(updateTimeRunnable); // Start updating time
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable); // Stop updating when activity is not visible
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        investigatorButton = findViewById(R.id.investigatorButton);
        caseStoreOfficerButton = findViewById(R.id.caseStoreOfficerButton);
        selfEnrollmentButton = findViewById(R.id.selfEnrollmentButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView); // Ensure this ID exists in your layout
        helpTextView = findViewById(R.id.helpTextView);
        watchTutorialTextView = findViewById(R.id.watchTutorialTextView);

        // Update date and time dynamically
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
        TempDataHolder.setAfterSignature(false);

        TempDataHolder.resetTimer();

        investigatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the new InvestigatorActivity
                TempDataHolder.setCurrentRole(TempDataHolder.UserRole.INVESTIGATOR);
                Intent intent = new Intent(MainActivity.this, Homepage.class);
                startActivity(intent);
            }
        });


        caseStoreOfficerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TempDataHolder.setCurrentRole(TempDataHolder.UserRole.CASE_STORE_OFFICER);
                Intent intent = new Intent(MainActivity.this, Homepage.class);
                startActivity(intent);
            }
        });

        selfEnrollmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EmailEnrollmentActivity.class);
                TempDataHolder.setCurrentRole(TempDataHolder.UserRole.SELF_ENROLL);
                TempDataHolder.setCurrentAction(TempDataHolder.UserAction.SELF_ENROLLMENT);
                startActivity(intent);
            }
        });

        View.OnClickListener tutorialClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, VideoTutorialsActivity.class);
            startActivity(intent);
        };

        helpTextView.setOnClickListener(tutorialClickListener);
        watchTutorialTextView.setOnClickListener(tutorialClickListener);
    }
}
