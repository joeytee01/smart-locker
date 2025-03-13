package com.example.smartlockersolution;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;

public class ScanTepBagsActivity extends AppCompatActivity {

    private LinearLayout tepListContainer;
    private TextView counterText;
    private int itemCount = 0;
    List<String> scannedTepIds = new ArrayList<>();
    private boolean isLockerOpened;

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
        setContentView(R.layout.activity_tep_bag_scanner);

        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);
        TempDataHolder.startTimer();

        // Setup header and profile info
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileName.setText("John Doe, Investigation Officer");
        } else {
            profileName.setText("Agatha, Case Store Officer");
        }
        headerTitle.setText(TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT ?
                "Deposit/Redeposit" : "Withdraw");

        updateDateTime();
        initializeViews();

        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR &&
                TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
            isLockerOpened = getIntent().getBooleanExtra("isLockerOpened", false);
            if(!TempDataHolder.getAfterSignature()) {
                showConfirmationDialog();
            }
        }
    }

    private void initializeViews() {
        tepListContainer = findViewById(R.id.tepListContainer);
        counterText = findViewById(R.id.counterText);
        ImageView scannerImage = findViewById(R.id.scannerImage);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button nextButton = findViewById(R.id.nextButton);

        scannerImage.setOnClickListener(v -> fetchTepBags());

        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ScanTepBagsActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(ScanTepBagsActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(ScanTepBagsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });
        nextButton.setOnClickListener(v -> validateAndProceed());
    }

    // Use ApiManager to fetch TEP codes for the given transaction
    private void fetchTepBags() {
        String transactionId = String.valueOf(TempDataHolder.getTransactionId());
        ApiManager.getInstance().fetchTepCodes(
                TempDataHolder.getAuthToken(),
                transactionId,
                new ApiManager.ApiCallback() {
                    @Override
                    public void onSuccess(JSONArray data) {
                        for (int i = 0; i < data.length(); i++) {
                            try {
                                JSONObject item = data.getJSONObject(i);
                                String tepNo = item.getString("tep_no");
                                runOnUiThread(() -> addTepBagItem(tepNo));
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(
                                        ScanTepBagsActivity.this, "Parsing error", Toast.LENGTH_SHORT).show());
                            }
                        }
                        runOnUiThread(() -> {
                            if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR &&
                                    TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW &&
                                    !isLockerOpened) {
                                validateAndProceed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() ->
                                Toast.makeText(ScanTepBagsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void addTepBagItem(String tepId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.tep_bag_item, tepListContainer, false);
        TextView numberText = row.findViewById(R.id.numberText);
        TextView idText = row.findViewById(R.id.idText);
        Button removeButton = row.findViewById(R.id.removeButton);

        itemCount++;
        numberText.setText(String.valueOf(itemCount));
        idText.setText(tepId);

        removeButton.setOnClickListener(v -> {
            tepListContainer.removeView(row);
            tepListContainer.removeView(row.getTag() != null ? (View) row.getTag() : null);
            scannedTepIds.remove(tepId);
            itemCount--;
            updateTepNumbers();
            updateCounter();
        });

        tepListContainer.addView(row);
        scannedTepIds.add(tepId);

        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
        );
        params.setMargins(0, 8, 0, 8); // Add spacing for better visibility
        divider.setLayoutParams(params);
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        tepListContainer.addView(divider);
        row.setTag(divider); // Store reference to divider for removal

        updateCounter();
    }


    private void updateTepNumbers() {
        int childCount = tepListContainer.getChildCount();
        int validIndex = 1; // Start numbering from 1

        for (int i = 0; i < childCount; i++) {
            View row = tepListContainer.getChildAt(i);

            if (row.findViewById(R.id.numberText) != null) {
                TextView numberText = row.findViewById(R.id.numberText);
                numberText.setText(String.valueOf(validIndex));
                validIndex++;
            }
        }
    }



    private void updateCounter() {
        counterText.setText(itemCount + " items scanned");
    }

    private void validateAndProceed() {
        if (scannedTepIds.isEmpty()) {
            Toast.makeText(this, "Scan at least one TEP bag", Toast.LENGTH_SHORT).show();
            return;
        }
        TempDataHolder.setScannedTepIds(scannedTepIds);
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR &&
                TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW &&
                isLockerOpened) {
            Intent intent = new Intent(this, PhotoCaptureTabletActivity.class);
            startActivity(intent);
        } else {
            showConfirmationDialog();
        }
    }

    void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_qr, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = (int) (screenWidth * 0.80);
            params.height = (int) (displayMetrics.heightPixels * 0.80);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
        Button btnBack = dialogView.findViewById(R.id.btnBack);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            callJourneyApi();
            if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.CASE_STORE_OFFICER) {
                Intent intent = new Intent(ScanTepBagsActivity.this, PhotoCaptureTabletActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(ScanTepBagsActivity.this, SignatureRemarksActivity.class);
                startActivity(intent);
            }
        });

        btnBack.setOnClickListener(v -> {
            if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR &&
                    TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
                startActivity(new Intent(this, CameraCaptureActivity.class));
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // Build JSON payload and use ApiManager to call the journey API
    private void callJourneyApi() {
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("action", TempDataHolder.getCurrentAction());
            jsonPayload.put("seizure_report_number", "009988812");
            jsonPayload.put("qr_code", "QRCODE123");
            jsonPayload.put("notification_type", "activate-phone");
            JSONArray tepsArray = new JSONArray();
            for (String tep : TempDataHolder.getScannedTepIds()) {
                tepsArray.put(tep);
            }
            jsonPayload.put("teps", tepsArray);
            jsonPayload.put("location_id", 1);
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(ScanTepBagsActivity.this, "Error building JSON payload", Toast.LENGTH_SHORT).show());
            return;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        // Create request body from the JSON payload
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonPayload.toString(), JSON);

        ApiManager.getInstance().callJourney(
                TempDataHolder.getAuthToken(),
                body,
                new ApiManager.ApiObjectCallback() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        try {
                            JSONObject dataObject = data.getJSONObject("data");
                            int transactionId = dataObject.getInt("transaction_id");
                            TempDataHolder.setTransactionId(transactionId);
                            runOnUiThread(() -> {
                                if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.CASE_STORE_OFFICER) {
                                    Intent intent = new Intent(ScanTepBagsActivity.this, PhotoCaptureTabletActivity.class);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(ScanTepBagsActivity.this, SignatureRemarksActivity.class);
                                    intent.putExtra("TRANSACTION_ID", transactionId);
                                    startActivity(intent);
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(ScanTepBagsActivity.this, "Error parsing Journey API response", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() ->
                                Toast.makeText(ScanTepBagsActivity.this, "Journey API call failed: " + errorMessage, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }
}
