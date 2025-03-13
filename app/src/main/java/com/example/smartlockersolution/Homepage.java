package com.example.smartlockersolution;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Homepage extends AppCompatActivity {

    private Button depositButton, withdrawButton, backButton;
    private TextView dateTimeTextView,timerTextView;
    private CountDownTimer countDownTimer;
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
        setContentView(R.layout.activity_investigator);

        depositButton = findViewById(R.id.depositButton);
        withdrawButton = findViewById(R.id.withdrawButton);
        backButton = findViewById(R.id.backButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        timerTextView = findViewById(R.id.timerTextView);

        // Update date and time dynamically
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);

        TempDataHolder.attachTimerTextView(timerTextView,this);
        TempDataHolder.startTimer();

        depositButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                TempDataHolder.setCurrentAction(TempDataHolder.UserAction.DEPOSIT_REDEPOSIT);
                Intent intent = new Intent(Homepage.this, DivisionPass.class);
                startActivity(intent);
            }
        });


        withdrawButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO: Add action for Withdraw
                TempDataHolder.setCurrentAction(TempDataHolder.UserAction.WITHDRAW);
                Intent intent = new Intent(Homepage.this, DivisionPass.class);
                startActivity(intent);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Go back to the previous page
                finish();
            }
        });
    }

}
