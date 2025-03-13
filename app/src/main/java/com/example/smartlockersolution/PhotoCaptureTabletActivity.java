package com.example.smartlockersolution;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoCaptureTabletActivity extends AppCompatActivity {

    private LinearLayout tepListContainer;
    private TextView scanQR,title;
    private ImageView scanqrIcon;
    private List<String> tepIds;
    private Button doneButton;
    boolean photosCompleted = false;
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
        setContentView(R.layout.activity_takephoto_tepbag);

        // Get TEP IDs
        tepIds = TempDataHolder.getScannedTepIds();
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
        initializeViews();
        populateTepList();

        doneButton = findViewById(R.id.nextButton);
        doneButton.setOnClickListener(v -> {
            if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
                if (!photosCompleted) {
                    handlePhotoCompletion();
                } else {
                    navigateToCompletion();
                }
            } else {
                if (!photosCompleted) {
                    handlePhotoCompletion();
                } else {
                    // For deposit/redeposit, open a locker cell
                    handleOpenCellRandom();
                }
            }
        });
    }

    private void handlePhotoCompletion() {
        scanQR.setVisibility(View.GONE);
        scanqrIcon.setVisibility(View.GONE);

        if (tepIds == null || tepIds.isEmpty()) {
            Toast.makeText(this, "No TEP bags to process", Toast.LENGTH_SHORT).show();
            return;
        }
        updatePhotoStatus();
        photosCompleted = true;

        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.CASE_STORE_OFFICER) {
            doneButton.setText("Next");
        } else if (isInvestigatorDepositRedeposit()) {
            doneButton.setText("Open Cell");
        } else {
            doneButton.setText("Complete");
        }
        doneButton.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.green_success)));
        title.setText("Please press \"" + doneButton.getText().toString() + "\" to proceed");
    }

    private void navigateToCompletion() {
        Intent intent = new Intent(this, CompletionActivity.class);
        startActivity(intent);
        finish();
    }

    // Use ApiManager to open the locker cell.
    void handleOpenCell(int lockerId) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("action", TempDataHolder.getCurrentAction());
            jsonBody.put("transaction_id", String.valueOf(TempDataHolder.getTransactionId()));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        RequestBody requestBody = RequestBody.create(jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8"));

        ApiManager.getInstance().openLocker(
                TempDataHolder.getAuthToken(),
                lockerId,
                requestBody,
                new ApiManager.ApiObjectCallback() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        runOnUiThread(() -> {
                            Toast.makeText(PhotoCaptureTabletActivity.this, "Locker " + lockerId + " is now open", Toast.LENGTH_LONG).show();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> showLockerDialog(lockerId), 100);
                        });
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() ->
                                Toast.makeText(PhotoCaptureTabletActivity.this, "API call failed: " + errorMessage, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    // Randomly select an available locker from TempDataHolder and call handleOpenCell.
    private void handleOpenCellRandom() {
        List<String> availableIds = TempDataHolder.getAvailableLockersId();
        if (availableIds == null || availableIds.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(PhotoCaptureTabletActivity.this, "No available lockers", Toast.LENGTH_SHORT).show());
            return;
        }
        Random random = new Random();
        String randomIdStr = availableIds.get(random.nextInt(availableIds.size()));
        int lockerId;
        try {
            lockerId = Integer.parseInt(randomIdStr);
        } catch (NumberFormatException e) {
            Toast.makeText(PhotoCaptureTabletActivity.this, "Invalid locker ID format", Toast.LENGTH_SHORT).show();
            return;
        }
        handleOpenCell(lockerId);
    }

    private void showLockerDialog(int lockerNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_locker_assignment, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(dialog.getWindow().getAttributes());
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        Button reopenButton = dialogView.findViewById(R.id.reopenButton);
        TextView chooseDifferentSize = dialogView.findViewById(R.id.chooseDifferentSize);

        String text = "Please place the TEP bag in \nLocker " + lockerNumber + " and close the locker";
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        int startIndex = text.indexOf("Locker " + lockerNumber);
        int endIndex = startIndex + ("Locker " + lockerNumber).length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(spannable);

        confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PhotoCaptureTabletActivity.this, CompletionActivity.class);
            startActivity(intent);
            finish();
        });
        reopenButton.setOnClickListener(v -> {
            dialog.dismiss();
            handleOpenCellRandom();
        });
        chooseDifferentSize.setOnClickListener(v -> {
            TempDataHolder.setChangeLockerSize(true);
            Intent intent = new Intent(PhotoCaptureTabletActivity.this, LockerSelectionActivity.class);
            startActivity(intent);
        });

        dialog.show();
    }

    private void initializeViews() {
        tepListContainer = findViewById(R.id.tepListContainer);
        scanQR = findViewById(R.id.scanqr);
        scanqrIcon = findViewById(R.id.scanqrIcon);
        title = findViewById(R.id.title);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button doneButton = findViewById(R.id.nextButton);

        scanQR.setOnClickListener(v -> showQRDialog());
        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(PhotoCaptureTabletActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(PhotoCaptureTabletActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Set up buttons and their actions
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(PhotoCaptureTabletActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });
        doneButton.setOnClickListener(v -> {
            if (isInvestigatorWithdraw() || isOfficerWithdraw()) {
                if (!photosCompleted) {
                    handlePhotoCompletion();
                } else {
                    navigateToCompletion();
                }
            } else {
                if (!photosCompleted) {
                    handlePhotoCompletion();
                } else {
                    // For deposit/redeposit, open a cell
                    handleOpenCellRandom();
                }
            }
        });
    }

    private boolean isInvestigatorWithdraw() {
        return TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR
                && TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW;
    }

    private boolean isInvestigatorDepositRedeposit() {
        return TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR
                && TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT;
    }

    private boolean isOfficerWithdraw() {
        return TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.CASE_STORE_OFFICER
                && TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW;
    }

    private void showQRDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_scan_qr, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = (int) (displayMetrics.widthPixels * 0.80);
            params.height = (int) (displayMetrics.heightPixels * 0.80);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
        Button btnBack = dialogView.findViewById(R.id.btnBack);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
                Intent intent = new Intent(PhotoCaptureTabletActivity.this, SignatureRemarksActivity.class);
                startActivity(intent);
            }
        });

        btnBack.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void populateTepList() {
        tepListContainer.removeAllViews();
        for (int i = 0; i < tepIds.size(); i++) {
            addTepItem(i + 1, tepIds.get(i));
        }
    }

    private void addTepItem(int position, String tepId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.tep_photo_item, tepListContainer, false);

        TextView numberText = itemView.findViewById(R.id.numberText);
        TextView idText = itemView.findViewById(R.id.idText);
        TextView statusText = itemView.findViewById(R.id.statusText);

        numberText.setText(String.valueOf(position));
        idText.setText(tepId);
        statusText.setText("No photo taken");
        statusText.setTextColor(getResources().getColor(R.color.orange));

        tepListContainer.addView(itemView);
    }

    private void updatePhotoStatus() {
        tepListContainer.removeAllViews();
        for (int i = 0; i < tepIds.size(); i++) {
            addCompletedTepItem(i + 1, tepIds.get(i));
        }
        Toast.makeText(this, "Photos marked as completed!", Toast.LENGTH_SHORT).show();
    }

    private void addCompletedTepItem(int position, String tepId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.tep_photo_done_item, tepListContainer, false);

        TextView numberText = itemView.findViewById(R.id.numberText);
        TextView idText = itemView.findViewById(R.id.idText);
        ImageView frontCheck = itemView.findViewById(R.id.frontCheck);
        ImageView backCheck = itemView.findViewById(R.id.backCheck);

        numberText.setText(String.valueOf(position));
        idText.setText(tepId);
        frontCheck.setImageResource(R.drawable.greentick);
        backCheck.setImageResource(R.drawable.greentick);

        tepListContainer.addView(itemView);
    }
}
