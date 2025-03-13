package com.example.smartlockersolution;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DivisionPass extends AppCompatActivity {

    private ImageButton scanImageButton;
    private Button backButton;
    private TextView dateTimeTextView, headerTitle, timerTextView;
    ApiManager apiManager = ApiManager.getInstance();

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
        setContentView(R.layout.activity_division_pass);

        scanImageButton = findViewById(R.id.scanImageButton);
        backButton = findViewById(R.id.backButton);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        headerTitle = findViewById(R.id.headerTitle);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView, this);
        TempDataHolder.startTimer();
        setupHeaderTitle();
        initializeDateTime();

        scanImageButton.setOnClickListener(v -> handleLogin());
        backButton.setOnClickListener(v -> finish());
    }


    private void setupHeaderTitle() {
        if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {
            headerTitle.setText("Deposit/Redeposit");
        } else if (TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
            headerTitle.setText("Withdraw");
        } else {
            headerTitle.setText("Self Enrollment");
        }
    }

    private void initializeDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        dateTimeTextView.setText(sdf.format(new Date()));
    }

    private void handleLogin() {
        String role = TempDataHolder.getCurrentRole().toString();
        String action = TempDataHolder.getCurrentAction().toString();
        String divisionPassNo = "DEF8765123";
        String password = "123456";

        apiManager.performLogin(
                role,
                action,
                divisionPassNo,
                password,
                new ApiManager.ApiObjectCallback() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        handleLoginSuccess(data);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showErrorToast(errorMessage);
                    }
                });
    }

    private void handleLoginSuccess(JSONObject data) {
        try {
            String apiToken = data.getJSONObject("data").getString("api_token");
            TempDataHolder.setAuthToken(apiToken);
            runOnUiThread(() -> {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                navigateBasedOnRole();
            });
        } catch (JSONException e) {
            showErrorToast("Error parsing login response");
        }
    }

    private void navigateBasedOnRole() {
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.SELF_ENROLL) {
            performEnrollment();
            return;
        }

        Class<?> targetActivity = determineTargetActivity();
        if (targetActivity != null) {
            startActivity(new Intent(this, targetActivity));
        }
    }

    private Class<?> determineTargetActivity() {
        if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR) {
            return TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT
                    ? ExhibitRestrictionActivity.class
                    : QrEmailActivity.class;
        }
        return TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.CASE_STORE_OFFICER
                ? PinEntryActivity.class
                : null;
    }

    private void performEnrollment() {
        apiManager.performEnrollment(
                TempDataHolder.getAuthToken(),
                "oLZyZRSfJc",
                "DEF8765123",
                getIntent().getStringExtra("email"),
                new ApiManager.ApiObjectCallback() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        runOnUiThread(() -> {
                            Toast.makeText(DivisionPass.this, "User enrolled successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(DivisionPass.this, CompletionActivity.class));
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showErrorToast(errorMessage);
                    }
                });
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

        // Custom animation
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
            // Fallback to default animation
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
