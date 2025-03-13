package com.example.smartlockersolution;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {32})
public class LockerSelectionActivityTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<LockerSelectionActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Set up TempDataHolder with valid values for header/profile updates.
        TempDataHolder.setAuthToken("dummyAuthToken");
        TempDataHolder.setCurrentRole(TempDataHolder.UserRole.INVESTIGATOR);
        TempDataHolder.setCurrentAction(TempDataHolder.UserAction.DEPOSIT_REDEPOSIT);
        TempDataHolder.setChangeLockerSize(false);

        // Inject our mock ApiManager.
        ApiManager.setInstance(mockApiManager);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void testHeaderAndProfileUpdate() {
        scenario = ActivityScenario.launch(LockerSelectionActivity.class);
        scenario.onActivity(activity -> {
            TextView headerTitle = activity.findViewById(R.id.headerTitle);
            TextView profileName = activity.findViewById(R.id.profileNameTextView);
            assertNotNull(headerTitle);
            assertNotNull(profileName);

            // With INVESTIGATOR and DEPOSIT_REDEPOSIT, header should be "Deposit/Redeposit"
            assertEquals("Deposit/Redeposit", headerTitle.getText().toString());
            // Investigator's profile name should be "John Doe, Investigation Officer"
            assertEquals("John Doe, Investigation Officer", profileName.getText().toString());
        });
    }

    @Test
    public void testFetchLockerAvailabilitySuccess() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);

        // Set up the mock behavior BEFORE launching the activity.
        doAnswer(invocation -> {
            ApiManager.ApiCallback callback = invocation.getArgument(1);
            // Simulated response: "small" has an available locker; "medium" has none.
            JSONArray jsonResponse = new JSONArray("["
                    + "{\"type\":\"small\",\"cells\":["
                    + "  {\"id\":\"locker1\",\"status\":\"available\"},"
                    + "  {\"id\":\"locker2\",\"status\":\"occupied\"}"
                    + "]},"
                    + "{\"type\":\"medium\",\"cells\":["
                    + "  {\"id\":\"locker3\",\"status\":\"occupied\"}"
                    + "]}"
                    + "]");
            callback.onSuccess(jsonResponse);
            latch.countDown();
            return null;
        }).when(mockApiManager).fetchLockerAvailability(anyString(), any());

        // Now launch the activity.
        scenario = ActivityScenario.launch(LockerSelectionActivity.class);

        // Wait up to 5 seconds for the callback.
        assertTrue("API callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));

        // Verify that the UI has updated accordingly.
        scenario.onActivity(activity -> {
            Button smallLocker = activity.findViewById(R.id.smallLockerButton);
            Button mediumLocker = activity.findViewById(R.id.mediumLockerButton);
            Button largeLocker = activity.findViewById(R.id.largeLockerButton);
            Button extraLargeLocker = activity.findViewById(R.id.extraLargeLockerButton);
            // "small" should be enabled; others remain disabled.
            assertTrue("Small locker button should be enabled", smallLocker.isEnabled());
            assertFalse("Medium locker button should be disabled", mediumLocker.isEnabled());
            assertFalse("Large locker button should be disabled", largeLocker.isEnabled());
            assertFalse("Extra-large locker button should be disabled", extraLargeLocker.isEnabled());
        });
    }

    @Test
    public void testCancelButtonShowsDialogAndNavigatesToMainActivity() {
        scenario = ActivityScenario.launch(LockerSelectionActivity.class);
        scenario.onActivity(activity -> {
            Button cancelButton = activity.findViewById(R.id.cancelButton);
            assertNotNull("Cancel button should not be null", cancelButton);
            cancelButton.performClick();

            // Retrieve the custom AlertDialog shown after cancel click.
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("AlertDialog should be displayed on cancel click", dialog);
            assertTrue("Dialog should be an AlertDialog", dialog instanceof AlertDialog);
            AlertDialog alertDialog = (AlertDialog) dialog;

            // Find the confirm button in the dialog's layout.
            Button confirmButton = alertDialog.findViewById(R.id.btnConfirmScan);
            assertNotNull("Confirm button in dialog should not be null", confirmButton);
            confirmButton.performClick();

            // Verify that MainActivity is started.
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start MainActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("MainActivity should be started",
                    MainActivity.class.getName(), startedActivityName);
        });
    }

    @Test
    public void testNextButtonWithoutSelectionShowsToast() {
        scenario = ActivityScenario.launch(LockerSelectionActivity.class);
        scenario.onActivity(activity -> {
            // Ensure no locker is selected and the seizure report is empty.
            EditText seizureReportEditText = activity.findViewById(R.id.seizureReportEditText);
            seizureReportEditText.setText("");

            Button nextButton = activity.findViewById(R.id.nextButton);
            assertNotNull("Next button should not be null", nextButton);
            nextButton.performClick();

            // Verify that a toast message is displayed.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Please select a locker size first", toastText);
        });
    }

    @Test
    public void testNextButtonWithSelectionAndSeizureReportNavigates() {
        scenario = ActivityScenario.launch(LockerSelectionActivity.class);
        scenario.onActivity(activity -> {
            // Simulate selecting a locker (e.g., small locker).
            Button smallLocker = activity.findViewById(R.id.smallLockerButton);
            // Ensure the button is enabled for testing.
            smallLocker.setEnabled(true);
            smallLocker.performClick();

            // Provide a valid seizure report.
            EditText seizureReportEditText = activity.findViewById(R.id.seizureReportEditText);
            seizureReportEditText.setText("SR123456");

            // Click the next button.
            Button nextButton = activity.findViewById(R.id.nextButton);
            nextButton.performClick();

            // Verify that an intent to the next activity is fired.
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Next activity intent should be fired", startedIntent);
            // Since TempDataHolder.getChangeLockerSize() is false, expect ScanTepBagsActivity.
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("ScanTepBagsActivity should be started",
                    ScanTepBagsActivity.class.getName(), startedActivityName);
        });
    }
}
