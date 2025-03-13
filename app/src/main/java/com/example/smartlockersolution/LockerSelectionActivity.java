package com.example.smartlockersolution;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LockerSelectionActivity extends AppCompatActivity {

    private Button smallLockerButton, mediumLockerButton, largeLockerButton, extraLargeLockerButton;
    private Button cancelButton, nextButton;
    private EditText seizureReportEditText;
    private ImageView scannerImageView;
    private List<Button> lockerButtons = new ArrayList<>();
    private Button selectedLockerButton = null;
    private TextView dateTimeTextView, headerTitle, profileName, timerTextView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Mapping from locker type (small, medium, etc.) to available IDs (as strings)
    private HashMap<String, List<String>> availableLockersMapping = new HashMap<>();

    // Runnable to update time every second
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            handler.postDelayed(this, 1000); // 'this' refers to the current Runnable instance.
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_size);

        initializeViews();
        updateHeaderAndProfile();
        updateDateTime();
        setupLockerButtons();
        setupButtonListeners();
        fetchLockerAvailability();
    }

    private void initializeViews() {
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        seizureReportEditText = findViewById(R.id.seizureReportEditText);
        scannerImageView = findViewById(R.id.scannerImageView);
        smallLockerButton = findViewById(R.id.smallLockerButton);
        mediumLockerButton = findViewById(R.id.mediumLockerButton);
        largeLockerButton = findViewById(R.id.largeLockerButton);
        extraLargeLockerButton = findViewById(R.id.extraLargeLockerButton);
        cancelButton = findViewById(R.id.cancelButton);
        nextButton = findViewById(R.id.nextButton);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);
        TempDataHolder.startTimer();

    }

    private void updateHeaderAndProfile() {
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileName.setText("John Doe, Investigation Officer");
        } else {
            profileName.setText("Agatha, Case Store Officer");
        }
        if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {
            headerTitle.setText("Deposit/Redeposit");
        } else {
            headerTitle.setText("Withdraw");
        }
    }

    private void setupLockerButtons() {
        lockerButtons.add(smallLockerButton);
        lockerButtons.add(mediumLockerButton);
        lockerButtons.add(largeLockerButton);
        lockerButtons.add(extraLargeLockerButton);
    }

    private void setupButtonListeners() {
        // Locker size buttons
        for (Button button : lockerButtons) {
            button.setOnClickListener(view -> handleLockerSelection((Button) view));
        }
        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(LockerSelectionActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(LockerSelectionActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Set up buttons and their actions
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(LockerSelectionActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });
        // Next button
        nextButton.setOnClickListener(view -> {
            if (selectedLockerButton == null) {
                showToast("Please select a locker size first");
                return;
            }
            if (seizureReportEditText.getText().toString().trim().isEmpty()) {
                showToast("Please enter or scan a seizure report number");
                return;
            }
            proceedToNextActivity();
        });
    }

    private void handleLockerSelection(Button selectedButton) {
        for (Button button : lockerButtons) {
            if (!button.isEnabled()) continue;
            button.setSelected(false);
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9ec1d9")));
            button.setTextColor(Color.parseColor("#333333"));
        }
        selectedButton.setSelected(true);
        selectedLockerButton = selectedButton;
        selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#01163d")));
        selectedButton.setTextColor(ColorStateList.valueOf(Color.parseColor("#354662")));
    }

    // Fetch locker availability using the centralized ApiManager.
    private void fetchLockerAvailability() {
        String authToken = TempDataHolder.getAuthToken();
        ApiManager.getInstance().fetchLockerAvailability(authToken, new ApiManager.ApiCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                runOnUiThread(() -> updateLockerButtons(data));
            }
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> showToast("Error fetching locker data: " + errorMessage));
            }
        });
    }

    private void updateLockerButtons(JSONArray lockerData) {
        try {
            resetLockerButtons();
            availableLockersMapping.clear();
            // Process each locker type from the API response
            for (int i = 0; i < lockerData.length(); i++) {
                JSONObject lockerTypeObj = lockerData.getJSONObject(i);
                String type = lockerTypeObj.getString("type").toLowerCase();
                JSONArray cells = lockerTypeObj.getJSONArray("cells");
                List<String> availableIds = new ArrayList<>();
                for (int j = 0; j < cells.length(); j++) {
                    JSONObject cell = cells.getJSONObject(j);
                    if ("available".equalsIgnoreCase(cell.getString("status"))) {
                        availableIds.add(cell.getString("id"));
                    }
                }
                availableLockersMapping.put(type, availableIds);
                updateButtonState(type, !availableIds.isEmpty());
            }
        } catch (JSONException e) {
            showToast("Error processing locker data");
        }
    }

    // Reset all locker buttons to default disabled state.
    private void resetLockerButtons() {
        for (Button button : lockerButtons) {
            button.setEnabled(false);
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BFBABABA")));
            button.setTextColor(Color.parseColor("#AEAEAE"));
        }
    }

    // Update a specific button's state based on availability.
    private void updateButtonState(String lockerType, boolean isAvailable) {
        Button targetButton = getButtonForLockerType(lockerType);
        if (targetButton != null) {
            targetButton.setEnabled(isAvailable);
            if (isAvailable) {
                targetButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#84b8db")));
                targetButton.setTextColor(ContextCompat.getColor(this, R.color.text_blue));
            } else {
                targetButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BABABA40")));
                targetButton.setTextColor(Color.parseColor("#BABABA40"));
            }
        }
    }

    // Helper to map locker type string to its corresponding button.
    private Button getButtonForLockerType(String lockerType) {
        switch (lockerType) {
            case "small":
                return smallLockerButton;
            case "medium":
                return mediumLockerButton;
            case "large":
                return largeLockerButton;
            case "extra-large":
                return extraLargeLockerButton;
            default:
                return null;
        }
    }

    // Normalize the locker size string (e.g., "Extra Large" to "extra-large").
    private String normalizeLockerSize(String lockerSize) {
        if ("Extra Large".equalsIgnoreCase(lockerSize)) {
            return "extra-large";
        } else if (lockerSize.contains(" ")) {
            return lockerSize.split(" ")[0].toLowerCase();
        } else {
            return lockerSize.toLowerCase();
        }
    }

    // Handle navigation to the next activity.
    private void proceedToNextActivity() {
        String lockerSize = normalizeLockerSize(selectedLockerButton.getText().toString().trim());
        TempDataHolder.setSelectedLockerSize(lockerSize);
        List<String> availableIds = availableLockersMapping.get(lockerSize);
        if (availableIds == null) {
            availableIds = new ArrayList<>();
        }
        TempDataHolder.setAvailableLockersId(availableIds);

        Intent intent = !TempDataHolder.getChangeLockerSize()
                ? new Intent(this, ScanTepBagsActivity.class)
                : new Intent(this, PhotoCaptureTabletActivity.class);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
