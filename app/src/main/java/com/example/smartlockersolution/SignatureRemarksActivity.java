package com.example.smartlockersolution;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignatureRemarksActivity extends AppCompatActivity {

    SignatureView signatureView;
    private EditText etRemarks;
    private String transactionId;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
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
        setContentView(R.layout.activity_signature_remarks);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);
        TempDataHolder.startTimer();

        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileName.setText("John Doe, Investigation Officer");
        } else {
            profileName.setText("Agatha, Case Store Officer");
        }

        headerTitle.setText(TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT ?
                "Deposit/Redeposit" : "Withdraw");

        updateDateTime();

        transactionId = String.valueOf(TempDataHolder.getTransactionId());
        if (transactionId == null || transactionId.isEmpty()) {
            showToast("Invalid transaction ID");
            finish();
            return;
        }

        initializeViews();
    }

    private void initializeViews() {
        signatureView = findViewById(R.id.signatureView);
        etRemarks = findViewById(R.id.etRemarks);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnNext = findViewById(R.id.btnNext);

        btnClear.setOnClickListener(v -> signatureView.clear());
        btnCancel.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(SignatureRemarksActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(SignatureRemarksActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(SignatureRemarksActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });

        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR &&
                TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
            btnNext.setText("Open Cell");
        }

        btnNext.setOnClickListener(v -> {
            if (isSignatureEmpty()) {
                showToast("Please provide a signature");
                return;
            }
            String remarks = etRemarks.getText().toString().trim();
            if (remarks.isEmpty()) {
                showToast("Please enter remarks");
                return;
            }
            if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR &&
                    TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
                TempDataHolder.setAfterSignature(true);
                processSubmission(signatureView.getSignature(), remarks);
                handleOpenCell(12);
            } else {
                processSubmission(signatureView.getSignature(), remarks);
            }
        });
    }

    private boolean isSignatureEmpty() {
        return signatureView.isEmpty();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    // Process submission by updating the transaction using ApiManager.
    private void processSubmission(Bitmap signature, String remarks) {
        String signatureBase64 = bitmapToBase64(signature);
        try {
            JSONObject json = new JSONObject();
            json.put("action", TempDataHolder.getCurrentAction());
            json.put("signature", signatureBase64);
            json.put("remarks", remarks);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            ApiManager.getInstance().updateTransaction(
                    TempDataHolder.getAuthToken(),
                    transactionId,
                    body,
                    new ApiManager.ApiObjectCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            runOnUiThread(() -> {
                                showToast("Transaction updated successfully");
                                if(TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {
                                    startActivity(new Intent(SignatureRemarksActivity.this, PhotoCaptureTabletActivity.class));
                                    finish();
                                }
                            });
                        }
                        @Override
                        public void onFailure(String errorMessage) {
                            runOnUiThread(() -> showToast("Error: " + errorMessage));
                        }
                    }
            );
        } catch (JSONException e) {
            showToast("Error creating request: " + e.getMessage());
        }
    }

    // Open the locker cell by calling ApiManager.
    private void handleOpenCell(int lockerId) {
        String urlEndpoint = "lockers/" + lockerId;
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("action", TempDataHolder.getCurrentAction());
            jsonBody.put("transaction_id", String.valueOf(TempDataHolder.getTransactionId()));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);

        ApiManager.getInstance().openLocker(
                TempDataHolder.getAuthToken(),
                lockerId,
                requestBody,
                new ApiManager.ApiObjectCallback() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        runOnUiThread(() -> {
                            showToast("Locker " + lockerId + " is now open");
                            new Handler().postDelayed(() -> showLockerDialog(lockerId), 1000);
                        });
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> showToast("API call failed: " + errorMessage));
                    }
                }
        );
    }

    private void showLockerDialog(int lockerId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_locker_withdrawal, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnReopen = dialogView.findViewById(R.id.btnReopen);

        tvMessage.setText("Locker " + lockerId + " is open");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(SignatureRemarksActivity.this, ScanTepBagsActivity.class);
            intent.putExtra("isLockerOpened", true);
            startActivity(intent);
        });

        btnReopen.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
