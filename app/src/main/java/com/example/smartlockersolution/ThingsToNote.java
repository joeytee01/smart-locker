package com.example.smartlockersolution;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class ThingsToNote extends AppCompatActivity {

    private Button continueButton;
    private TextView dateTimeTextView, headerTitle, profileName, timerTextView;
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
        handler.removeCallbacks(updateTimeRunnable);
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_things_to_note);

        continueButton = findViewById(R.id.continueButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);
        TempDataHolder.startTimer();
        if(TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR){
            profileName.setText("John Doe, Investigation Officer");
        }
        else{
            profileName.setText("Agatha, Case Store Officer");
        }

        if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {
            headerTitle.setText("Deposit/Redeposit");
        }

        else{
            headerTitle.setText("Withdraw");
        }

        // Update date and time dynamically
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);

        dateTimeTextView.setText(currentDateAndTime);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ThingsToNote.this, CameraCaptureActivity.class);
                startActivity(intent);
            }
        });
    }
}
