package com.example.smartlockersolution;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmailEnrollmentActivity extends AppCompatActivity {

    private TextView headerTitle, dateTimeTextView, selectionPrompt, timerTextView;
    private EditText emailEditText;
    private Button cancelButton, nextButton;

    // Handler to update the time every second.
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_email);

        headerTitle = findViewById(R.id.headerTitle);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        selectionPrompt = findViewById(R.id.selectionPrompt);
        emailEditText = findViewById(R.id.emailEditText);
        cancelButton = findViewById(R.id.cancelButton);
        nextButton = findViewById(R.id.nextButton);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView,this);
        TempDataHolder.startTimer();


        headerTitle.setText("Self Enrollment");


        handler.post(updateTimeRunnable);

        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(EmailEnrollmentActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(EmailEnrollmentActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {

                Intent intent = new Intent(EmailEnrollmentActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });

        // Next button: validate email and then navigate to QrEmailActivity.
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                if (email.isEmpty()) {
                    showErrorToast("Please enter your email ID");
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showErrorToast("Please enter a valid email address");
                    return;
                }

                Intent intent = new Intent(EmailEnrollmentActivity.this, QrEmailActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
    }


    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        dateTimeTextView.setText(currentTime);
    }

    void showErrorToast(String message) {
        View layout = getLayoutInflater().inflate(R.layout.error_toast, null);

        TextView text = layout.findViewById(R.id.error_toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);

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
