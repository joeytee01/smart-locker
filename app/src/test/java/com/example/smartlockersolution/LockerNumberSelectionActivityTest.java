package com.example.smartlockersolution;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {32})
public class LockerNumberSelectionActivityTest {

    @Mock
    ApiManager mockApiManager;

    private ActivityScenario<LockerNumberSelectionActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ApiManager.setInstance(mockApiManager);
        scenario = ActivityScenario.launch(LockerNumberSelectionActivity.class);
    }

    @Test
    public void test_UIElementsInitialized() {
        scenario.onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.dateTimeTextView));
            assertNotNull(activity.findViewById(R.id.layoutLockers));
            assertNotNull(activity.findViewById(R.id.btnCancel));
            assertNotNull(activity.findViewById(R.id.btnOpenLocker));
        });
    }

    @Test
    public void test_FetchLockers_UpdatesUIForOccupiedLockers() throws JSONException {
        JSONObject mockResponse = new JSONObject();
        JSONArray dataArray = new JSONArray();

        JSONObject occupiedLocker = new JSONObject();
        occupiedLocker.put("id", 5);
        occupiedLocker.put("status", "occupied");
        dataArray.put(occupiedLocker);

        JSONObject expiredLocker = new JSONObject();
        expiredLocker.put("id", 10);
        expiredLocker.put("status", "expired");
        dataArray.put(expiredLocker);

        mockResponse.put("data", dataArray);

        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(1);
            callback.onSuccess(mockResponse);
            return null;
        }).when(mockApiManager).fetchAllLockers(any(), any());

        scenario.onActivity(activity -> {
            activity.fetchLockers();

            TextView locker5 = activity.lockerMap.get(5);
            TextView locker10 = activity.lockerMap.get(10);

            assertNotNull(locker5);
            assertNotNull(locker10);

            // Retrieve the expected drawables
            Drawable expectedOccupied = ContextCompat.getDrawable(activity, R.drawable.background_occupied);
            Drawable expectedExpired = ContextCompat.getDrawable(activity, R.drawable.background_expired);

            // Compare the background drawables. If they're ColorDrawables, you can compare their color values.
            assertTrue("Locker 5 should have the occupied background",
                    areDrawablesIdentical(expectedOccupied, locker5.getBackground()));
            assertTrue("Locker 10 should have the expired background",
                    areDrawablesIdentical(expectedExpired, locker10.getBackground()));
        });
    }

    // Helper method to compare two drawables
    private boolean areDrawablesIdentical(Drawable drawableA, Drawable drawableB) {
        if (drawableA instanceof ColorDrawable && drawableB instanceof ColorDrawable) {
            return ((ColorDrawable) drawableA).getColor() == ((ColorDrawable) drawableB).getColor();
        }
        // Fallback: Compare constant state if available.
        return drawableA.getConstantState() != null
                && drawableA.getConstantState().equals(drawableB.getConstantState());
    }

    @Test
    public void test_SelectLocker_UpdatesUI() {
        scenario.onActivity(activity -> {
            activity.setLockerOccupied(5);
            activity.selectLocker(5);

            TextView selectedLocker = activity.lockerMap.get(5);
            assertNotNull(selectedLocker);
            assertEquals(activity.getResources().getColor(R.color.white), selectedLocker.getCurrentTextColor());
        });
    }

    @Test
    public void test_OpenLocker_API_Call() throws JSONException {
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("success", true);

        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(3);
            callback.onSuccess(mockResponse);
            return null;
        }).when(mockApiManager).openLocker(any(), anyInt(), any(), any());

        scenario.onActivity(activity -> {
            activity.handleOpenCell(5);
            verify(mockApiManager).openLocker(any(), eq(5), any(), any());
        });
    }

    @Test
    public void test_CancelButton_ShowsDialog() {
        scenario.onActivity(activity -> {
            Button cancelButton = activity.findViewById(R.id.btnCancel);
            cancelButton.performClick();

            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("Locker confirmation dialog should be displayed", dialog);
            assertTrue("Dialog should be an AlertDialog", dialog instanceof AlertDialog);
        });
    }
}
