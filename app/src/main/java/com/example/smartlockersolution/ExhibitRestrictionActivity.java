package com.example.smartlockersolution;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExhibitRestrictionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExhibitAdapter adapter;
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
        setContentView(R.layout.activity_exhibit_restriction);

        recyclerView = findViewById(R.id.recyclerView);
        continueButton = findViewById(R.id.continueButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);

        TempDataHolder.startTimer();

        // Set profile name based on role
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileName.setText("John Doe, Investigation Officer");
        } else {
            profileName.setText("Agatha, Case Store Officer");
        }

        // Set header title based on action
        if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {
            headerTitle.setText("Deposit/Redeposit");
        } else {
            headerTitle.setText("Withdraw");
        }

        // Set RecyclerView to a horizontal layout
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Initialize list of restricted items
        List<ExhibitItem> exhibitList = new ArrayList<>();
        exhibitList.add(new ExhibitItem("Bulky Items", R.drawable.bulky,
                "Exceeding 330mm(W) x (450mm(H) X600mm (D)"));
        exhibitList.add(new ExhibitItem("Drugs", R.drawable.drugs2, ""));
        exhibitList.add(new ExhibitItem("Valuables", R.drawable.valuables,
                "Items worth more \nthan $5000"));
        exhibitList.add(new ExhibitItem("Flammable Items", R.drawable.flammable,
                "Fuel, Lighter, pesticides and so on"));
        exhibitList.add(new ExhibitItem("Biodegradable", R.drawable.biodegradable, ""));
        exhibitList.add(new ExhibitItem("Bloodstained/Wet", R.drawable.bloodstained, ""));

        adapter = new ExhibitAdapter(this, exhibitList);
        recyclerView.setAdapter(adapter);

        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExhibitRestrictionActivity.this, ThingsToNote.class);
            startActivity(intent);
        });
    }
}
