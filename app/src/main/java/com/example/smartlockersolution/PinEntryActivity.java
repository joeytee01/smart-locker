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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PinEntryActivity extends AppCompatActivity {

    private TextView pinDisplay;
    private StringBuilder enteredPin = new StringBuilder();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_entry);

        // Initialize header views
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        profileName = findViewById(R.id.profileNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView, this);

        // Set user role information
        if(TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            profileName.setText("John Doe, Investigation Officer");
        } else {
            profileName.setText("Agatha, Case Store Officer");
        }

        if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {
            headerTitle.setText("Deposit/Redeposit");
        }

        else{
            headerTitle.setText("Withdraw");
        }

        // Initialize PIN display
        pinDisplay = findViewById(R.id.pinDisplay);
        updatePinDisplay();

        // Initialize number buttons
        int[] numberButtons = {
                R.id.btn0, R.id.btn1, R.id.btn2,
                R.id.btn3, R.id.btn4, R.id.btn5,
                R.id.btn6, R.id.btn7, R.id.btn8,
                R.id.btn9
        };

        // Set click listeners for number buttons
        for (int buttonId : numberButtons) {
            findViewById(buttonId).setOnClickListener(v -> {
                Button button = (Button) v;
                addDigit(button.getText().toString());
            });
        }

        // Clear button
        findViewById(R.id.btnClear).setOnClickListener(v -> clearPin());

        // Backspace button
        findViewById(R.id.btnBack).setOnClickListener(v -> removeLastDigit());

        // Cancel button
        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(PinEntryActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(PinEntryActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Set up buttons and their actions
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(PinEntryActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });

        findViewById(R.id.nextButton).setOnClickListener(v -> validatePin());
        updateDateTime();
    }

    private void addDigit(String digit) {
        if (enteredPin.length() < 6) {
            enteredPin.append(digit);
            updatePinDisplay();
        }
    }

    private void removeLastDigit() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updatePinDisplay();
        }
    }

    private void clearPin() {
        enteredPin.setLength(0);
        updatePinDisplay();
    }

    private void updatePinDisplay() {
        pinDisplay.setText(enteredPin.toString());
    }

    private void validatePin() {
        if (enteredPin.length() != 6) {
            showErrorToast("Please enter 6-digit PIN");
            return;
        }

        if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
            Intent intent = new Intent(this, LockerNumberSelectionActivity.class);
            startActivity(intent);
        }
        else{
            Intent intent = new Intent(this, EmailSelectionActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
    }

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

    void showErrorToast(String message) {
        View layout = getLayoutInflater().inflate(R.layout.error_toast, null);

        TextView text = layout.findViewById(R.id.error_toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);

        // Position at top with margin
        int yOffset = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                56,  // 56dp from top
                getResources().getDisplayMetrics()
        );
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, yOffset);

        applyErrorAnimation(toast);

        toast.show();
    }

    private void applyErrorAnimation(Toast toast) {
        try {
            Object mTN = getField(toast, "mTN");
            if (mTN != null) {
                Object mParams = getField(mTN, "mParams");
                if (mParams instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) mParams;
                    params.windowAnimations = R.style.ErrorToastAnimation;
                }
            }
        } catch (Exception e) {
            Log.e("ToastAnim", "Error applying animation", e);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 56);
        }
    }

    private Object getField(Object object, String fieldName) throws Exception {
        Class<?> clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
