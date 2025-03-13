package com.example.smartlockersolution;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class LockerNumberSelectionActivity extends AppCompatActivity {

    // Define locker groups (each group is a set of columns that stick together)
    private final int[][][] LOCKER_GROUPS = {
            { {1} },
            {
                    {2, 3, 4, 5, 6, 7, 8, 9},
                    {10, 11, 12, 13, 14, 15, 16, 17}
            },
            {
                    {18, 19, 20, 21, 22, 23, 24, 25},
                    {26, 27, 28, 29, 30, 31, 32, 33}
            },
            { {34, 35, 36} },
            {
                    {37, 38, 39},
                    {40, 41, 42}
            },
            {
                    {43, 44, 45},
                    {46, 47, 48}
            },
            {
                    {49, 50, 51},
                    {52, 53, 54}
            }
    };

    // Map to keep references to locker views
    Map<Integer, TextView> lockerMap = new HashMap<>();

    // Base dimensions (in dp)
    private final int BASE_HEIGHT_DP = 50;
    private final int BASE_WIDTH_DP = 80;
    private float density;

    // To track the currently selected locker.
    int selectedLockerId = -1;

    // MediaType for JSON (used for PUT calls)
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TextView dateTimeTextView, timerTextView;
    private Button cancelButton;
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
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM   hh:mm a", java.util.Locale.getDefault());
        dateTimeTextView.setText(sdf.format(new java.util.Date()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_selection);

        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        LinearLayout layoutLockers = findViewById(R.id.layoutLockers);
        cancelButton = findViewById(R.id.btnCancel);
        timerTextView = findViewById(R.id.timerTextView);
        TempDataHolder.attachTimerTextView(timerTextView, this);

        density = getResources().getDisplayMetrics().density;
        int baseHeightPx = (int) (BASE_HEIGHT_DP * density + 0.5f);
        int baseWidthPx = (int) (BASE_WIDTH_DP * density + 0.5f);

        // Build locker UI based on LOCKER_GROUPS definition.
        for (int groupIndex = 0; groupIndex < LOCKER_GROUPS.length; groupIndex++) {
            int[][] group = LOCKER_GROUPS[groupIndex];
            LinearLayout groupContainer = new LinearLayout(this);
            groupContainer.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            groupParams.setMargins(0, 0, (groupIndex == LOCKER_GROUPS.length - 1) ? 0 : (int) (16 * density), 0);
            groupContainer.setLayoutParams(groupParams);

            for (int[] column : group) {
                LinearLayout columnLayout = new LinearLayout(this);
                columnLayout.setOrientation(LinearLayout.VERTICAL);
                columnLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                for (int lockerNum : column) {
                    TextView lockerView = createLockerView(lockerNum, baseWidthPx, baseHeightPx, density);
                    columnLayout.addView(lockerView);

                    if (lockerNum == 34) {
                        addSpacer(columnLayout, 20 * density);
                        TextView screen = new TextView(this);

                        // Create rounded background programmatically
                        GradientDrawable gradientDrawable = new GradientDrawable();
                        gradientDrawable.setColor(Color.GRAY);
                        gradientDrawable.setCornerRadius(8 * density); // 8dp radius

                        screen.setBackground(gradientDrawable);
                        screen.setText("Screen");
                        screen.setTextColor(Color.WHITE);
                        screen.setGravity(Gravity.CENTER);

                        // Optional: Add padding to prevent text from touching edges
                        screen.setPadding(
                                (int) (8 * density),  // left
                                (int) (4 * density), // top
                                (int) (8 * density),  // right
                                (int) (4 * density)   // bottom
                        );

                        LinearLayout.LayoutParams screenParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                (int) (50 * density)
                        );
                        screenParams.setMargins(0, (int) (4 * density), 0, (int) (4 * density));
                        screen.setLayoutParams(screenParams);
                        columnLayout.addView(screen);
                        addSpacer(columnLayout, 220 * density);
                    }
                }
                groupContainer.addView(columnLayout);
            }
            layoutLockers.addView(groupContainer);
        }

        // Wire up the "Open Locker" button.
        Button btnOpenCell = findViewById(R.id.btnOpenLocker);
        btnOpenCell.setOnClickListener(v -> {
            if (selectedLockerId != -1) {
                handleOpenCell(selectedLockerId);
            } else {
                showErrorToast("Please select an occupied locker");
            }
        });

        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(LockerNumberSelectionActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(LockerNumberSelectionActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Set up buttons and their actions
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {
                Intent intent = new Intent(LockerNumberSelectionActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });

        // Fetch locker states from API.
        fetchLockers();
    }

    // Helper method to add a spacer view.
    private void addSpacer(LinearLayout layout, float heightPx) {
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) heightPx
        );
        spacer.setLayoutParams(spacerParams);
        layout.addView(spacer);
    }

    // Helper method to create a locker view.
    private TextView createLockerView(int lockerNum, int baseWidthPx, int baseHeightPx, float density) {
        TextView lockerView = new TextView(this);
        lockerView.setText(String.format("%02d", lockerNum));
        lockerView.setGravity(Gravity.CENTER);
        lockerView.setTextSize(lockerNum == 1 ? 20f : 16f);
        lockerView.setTextColor(Color.BLACK);
        lockerView.setBackgroundResource(R.drawable.background_not_occupied);

        int multiplier = getHeightMultiplier(lockerNum);
        int lockerHeightPx = baseHeightPx * multiplier;
        int lockerWidthPx = (lockerNum == 1) ? baseWidthPx * 2 : baseWidthPx;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(lockerWidthPx, lockerHeightPx);
        lockerView.setLayoutParams(params);

        lockerMap.put(lockerNum, lockerView);
        return lockerView;
    }

    /**
     * Returns the locker size label based on its number.
     */
    private String getLockerSize(int lockerNum) {
        if (lockerNum == 1) {
            return "Extra-Large";
        } else if (lockerNum == 42 || lockerNum == 45 || lockerNum == 48 ||
                lockerNum == 51 || lockerNum == 54) {
            return "Large";
        } else if (lockerNum == 2 || lockerNum == 3 ||
                lockerNum == 10 || lockerNum == 11 ||
                lockerNum == 18 || lockerNum == 19 ||
                lockerNum == 26 || lockerNum == 27 ||
                lockerNum == 34 || lockerNum == 37 || lockerNum == 38 ||
                lockerNum == 40 || lockerNum == 41 ||
                lockerNum == 43 || lockerNum == 44 ||
                lockerNum == 46 || lockerNum == 47 ||
                lockerNum == 49 || lockerNum == 50 ||
                lockerNum == 52 || lockerNum == 53) {
            return "Medium";
        }
        return "Small";
    }

    /**
     * Returns the height multiplier based on the locker size.
     */
    private int getHeightMultiplier(int lockerNum) {
        String size = getLockerSize(lockerNum);
        switch (size) {
            case "Extra-Large": return 10;
            case "Large": return 6;
            case "Medium": return 2;
            case "Small":
            default: return 1;
        }
    }


    // API Call: fetch all lockers. For each occupied locker, update its UI.
    void fetchLockers() {
        ApiManager.getInstance().fetchAllLockers(TempDataHolder.getAuthToken(), new ApiManager.ApiObjectCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    JSONArray data = json.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject lockerObj = data.getJSONObject(i);
                        int id = lockerObj.getInt("id");
                        String status = lockerObj.getString("status");
                        if (status.equalsIgnoreCase("occupied")) {
                            runOnUiThread(() -> {
                                setLockerOccupied(id);
                                TextView lockerView = lockerMap.get(id);
                                if (lockerView != null) {
                                    // Only occupied lockers are selectable.
                                    lockerView.setOnClickListener(v -> selectLocker(id));
                                }
                            });
                        }
                        if (status.equalsIgnoreCase("expired")) {
                            runOnUiThread(() -> {
                                setLockerExpired(id);
                            });
                        }

                    }
                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(LockerNumberSelectionActivity.this, "Parsing Error", Toast.LENGTH_SHORT).show());
                }
            }
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(LockerNumberSelectionActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Called when an occupied locker is tapped to mark it as selected.
    void selectLocker(int lockerId) {
        if (selectedLockerId != -1 && selectedLockerId != lockerId) {
            TextView prevLocker = lockerMap.get(selectedLockerId);
            if (prevLocker != null) {
                setLockerOccupied(selectedLockerId);
            }
        }

        TextView currLocker = lockerMap.get(lockerId);
        if (currLocker != null) {
            // Create a gradient drawable with border and rounded corners
            GradientDrawable selectedDrawable = new GradientDrawable();
            selectedDrawable.setColor(Color.parseColor("#92b2e0")); // Light blue background
            selectedDrawable.setStroke((int) (4 * density), Color.YELLOW); // Yellow border
            selectedDrawable.setCornerRadius(8 * density); // Rounded corners (matches previous radius)

            currLocker.setBackground(selectedDrawable);
            currLocker.setTextColor(Color.WHITE);
        }
        selectedLockerId = lockerId;
    }


    // Open Locker API call using ApiManager.
    void handleOpenCell(int lockerId) {
        String endpoint = "lockers/" + lockerId;
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("action", TempDataHolder.getCurrentAction());
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
                            Toast.makeText(LockerNumberSelectionActivity.this,
                                    "Locker " + lockerId + " is now open", Toast.LENGTH_LONG).show();
                            new Handler(Looper.getMainLooper()).postDelayed(
                                    () -> showLockerDialog(lockerId), 100);
                        });
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() ->
                                Toast.makeText(LockerNumberSelectionActivity.this,
                                        "API Error: " + errorMessage, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    // Displays a dialog that indicates the locker is open.
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
            Intent intent = new Intent(LockerNumberSelectionActivity.this, ScanTepBagsActivity.class);
            intent.putExtra("isLockerOpened", true);
            startActivity(intent);
        });

        btnReopen.setOnClickListener(v -> {
            dialog.dismiss();
            // Optionally reattempt opening the locker.
            // handleOpenCell(lockerId);
        });

        dialog.show();
    }

    // Updates an occupied locker: set background and text color.
    void setLockerOccupied(int lockerId) {
        TextView locker = lockerMap.get(lockerId);
        if (locker != null) {
            locker.setBackgroundResource(R.drawable.background_occupied);
            locker.setTextColor(Color.WHITE);
        }
    }

    // (Optional) Updates an expired locker.
    private void setLockerExpired(int lockerId) {
        TextView locker = lockerMap.get(lockerId);
        if (locker != null) {
            locker.setBackgroundResource(R.drawable.background_expired);
            locker.setTextColor(Color.WHITE);
        }
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
