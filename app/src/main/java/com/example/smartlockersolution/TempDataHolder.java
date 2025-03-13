package com.example.smartlockersolution;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TempDataHolder {
    public enum UserRole {
        INVESTIGATOR,
        CASE_STORE_OFFICER,
        SELF_ENROLL
    }

    public enum UserAction {
        DEPOSIT_REDEPOSIT,
        WITHDRAW,
        SELF_ENROLLMENT
    }

    private static List<String> scannedTepIds = new ArrayList<>();
    private static String selectedLockerSize = "";
    private static UserRole currentRole;
    private static UserAction currentAction;
    private static String authToken = "";
    private static boolean changeLockerSize = false;
    private static boolean afterSignature = false;
    private static List<String> availableLockersId = new ArrayList<>();
    private static int transactionId = 0;

    // Timer fields (shared across activities)
    private static long timerMillis = 20 * 60 * 1000; // 20 minutes in milliseconds
    private static CountDownTimer countDownTimer;
    private static TextView currentTimerTextView;
    private static WeakReference<Activity> currentActivityRef;
    private static AlertDialog expirationDialog;

    public static void setScannedTepIds(List<String> ids) {
        scannedTepIds = new ArrayList<>(ids);
    }

    public static List<String> getScannedTepIds() {
        return new ArrayList<>(scannedTepIds);
    }

    public static void setSelectedLockerSize(String size) {
        selectedLockerSize = size;
    }

    public static String getSelectedLockerSize() {
        return selectedLockerSize;
    }

    public static void setCurrentRole(UserRole role) {
        currentRole = role;
    }

    public static UserRole getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentAction(UserAction action) {
        currentAction = action;
    }

    public static UserAction getCurrentAction() {
        return currentAction;
    }

    public static void setAuthToken(String token) {
        authToken = "Bearer " + token;
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static void setChangeLockerSize(boolean flag) {
        changeLockerSize = flag;
    }

    public static boolean getChangeLockerSize() {
        return changeLockerSize;
    }

    public static void setAfterSignature(boolean flag) {
        afterSignature = flag;
    }

    public static boolean getAfterSignature() {
        return afterSignature;
    }

    public static void setAvailableLockersId(List<String> ids) {
        availableLockersId = new ArrayList<>(ids);
    }

    public static List<String> getAvailableLockersId() {
        return new ArrayList<>(availableLockersId);
    }

    public static void setTransactionId(int id) {
        transactionId = id;
    }

    public static int getTransactionId() {
        return transactionId;
    }

    // Timer Methods

    public static void setTimerMillis(long millis) {
        timerMillis = millis;
    }

    public static long getTimerMillis() {
        return timerMillis;
    }

    /**
     * Attach the current activity's timer TextView.
     * Call this in onResume of any activity that shows the timer.
     */
    public static void attachTimerTextView(TextView tv, Activity activity) {
        currentTimerTextView = tv;
        currentActivityRef = new WeakReference<>(activity);
        updateTimerText();
    }

    /**
     * Updates the attached timer TextView with the current remaining time.
     */
    private static void updateTimerText() {
        if (currentTimerTextView != null) {
            int minutes = (int) (timerMillis / 60000);
            int seconds = (int) ((timerMillis % 60000) / 1000);
            currentTimerTextView.setText(String.format("%02d:%02d remaining", minutes, seconds));
        }
    }

    /**
     * Starts the timer if it is not already running.
     * All activities share the same timer.
     */
    public static void startTimer() {
        if (countDownTimer == null) {
            countDownTimer = new CountDownTimer(timerMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerMillis = millisUntilFinished;
                    updateTimerText();
                }

                @Override
                public void onFinish() {
                    timerMillis = 0;
                    if (currentTimerTextView != null) {
                        currentTimerTextView.setText("Time Expired");
                    }
                    showExpirationDialog();
                }
            }.start();
        }
    }

    private static void showExpirationDialog() {
        Activity activity = currentActivityRef != null ? currentActivityRef.get() : null;
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                if (expirationDialog != null && expirationDialog.isShowing()) {
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomDialogTheme);
                View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_cancel, null);
                builder.setView(dialogView);
                expirationDialog = builder.create();
                expirationDialog.setCancelable(false);

                TextView titleText = dialogView.findViewById(R.id.titleText);
                TextView messageText = dialogView.findViewById(R.id.messageText);

                titleText.setText("Are you still here?");
                messageText.setText("");

                Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
                Button btnBack = dialogView.findViewById(R.id.btnBack);

                btnBack.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                    expirationDialog.dismiss();
                });

                btnConfirm.setOnClickListener(v -> {
                    resetTimer();
                    startTimer();
                    expirationDialog.dismiss();
                });

                try {
                    expirationDialog.show();
                } catch (WindowManager.BadTokenException e) {
                    // Handle exception if needed
                }
            });
        }
    }

    /**
     * Resets the timer to the default 20 minutes.
     * Cancels any running timer and starts a new one.
     */
    public static void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerMillis = 20 * 60 * 1000;
        if (expirationDialog != null && expirationDialog.isShowing()) {
            expirationDialog.dismiss();
        }
    }
}
