package com.example.smartlockersolution;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmailSelectionActivity extends AppCompatActivity {

    private Spinner emailSpinner;
    private Button cancelButton, nextButton;
    private TextView dateTimeTextView, profileNameTextView, headerTitle, timerTextView;
    String selectedEmail;
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
        dateTimeTextView.setText(sdf.format(new Date()));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email);

        emailSpinner = findViewById(R.id.emailSpinner);
        cancelButton = findViewById(R.id.cancelButton);
        nextButton = findViewById(R.id.nextButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        profileNameTextView = findViewById(R.id.profileNameTextView);
        headerTitle = findViewById(R.id.headerTitle);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);

        // Set profile name based on role
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileNameTextView.setText("John Doe, Investigation Officer");
        } else {
            profileNameTextView.setText("Agatha, Case Store Officer");
        }
        headerTitle.setText("Select Officer");

        updateDateTime();
        fetchEmails();

        emailSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEmail = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedEmail = null;
            }
        });

        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(EmailSelectionActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(EmailSelectionActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(EmailSelectionActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });

        nextButton.setOnClickListener(v -> {
            if (selectedEmail != null) {
                Toast.makeText(EmailSelectionActivity.this, "Selected: " + selectedEmail, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EmailSelectionActivity.this, LockerSelectionActivity.class);
                intent.putExtra("selectedEmail", selectedEmail);
                startActivity(intent);
            } else {
                Toast.makeText(EmailSelectionActivity.this, "Please select an email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetch emails using the centralized ApiManager
    public void fetchEmails() {
        String authToken = TempDataHolder.getAuthToken();
        String role = TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR ? "investigator" : "cs-officer";

        ApiManager.getInstance().fetchEmails(authToken, role, new ApiManager.ApiObjectCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    if (json.getBoolean("status")) {
                        JSONArray emailArray = json.getJSONObject("data").getJSONArray("emails");
                        List<String> emails = new ArrayList<>();
                        for (int i = 0; i < emailArray.length(); i++) {
                            emails.add(emailArray.getString(i));
                        }
                        runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(EmailSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, emails);
                            emailSpinner.setAdapter(adapter);
                            if (emails.isEmpty()) {
                                Toast.makeText(EmailSelectionActivity.this, "No emails found", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            // Set an empty adapter when status is false.
                            emailSpinner.setAdapter(new ArrayAdapter<>(EmailSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
                            Toast.makeText(EmailSelectionActivity.this, "No emails found", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        // Set an empty adapter if parsing fails.
                        emailSpinner.setAdapter(new ArrayAdapter<>(EmailSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
                        Toast.makeText(EmailSelectionActivity.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    // Set an empty adapter if API call fails.
                    emailSpinner.setAdapter(new ArrayAdapter<>(EmailSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
                    Toast.makeText(EmailSelectionActivity.this, "API call failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

}
