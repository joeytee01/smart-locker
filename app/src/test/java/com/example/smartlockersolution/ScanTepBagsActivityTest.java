package com.example.smartlockersolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.annotation.Config;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {32})
public class ScanTepBagsActivityTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<ScanTepBagsActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Set up TempDataHolder with valid values.
        TempDataHolder.setAuthToken("dummyAuthToken");
        TempDataHolder.setCurrentRole(TempDataHolder.UserRole.INVESTIGATOR);
        TempDataHolder.setCurrentAction(TempDataHolder.UserAction.WITHDRAW);
        TempDataHolder.setAfterSignature(false);
        TempDataHolder.setTransactionId(123);
        // Initialize scanned TEP IDs to an empty list, not null.
        TempDataHolder.setScannedTepIds(new ArrayList<>());

        // Inject the mock ApiManager.
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
        scenario = ActivityScenario.launch(ScanTepBagsActivity.class);
        scenario.onActivity(activity -> {
            TextView headerTitle = activity.findViewById(R.id.headerTitle);
            TextView profileName = activity.findViewById(R.id.profileNameTextView);
            assertNotNull(headerTitle);
            assertNotNull(profileName);

            // For INVESTIGATOR and WITHDRAW, header should be "Withdraw"
            assertEquals("Withdraw", headerTitle.getText().toString());
            // Investigator's profile should display "John Doe, Investigation Officer"
            assertEquals("John Doe, Investigation Officer", profileName.getText().toString());
        });
    }

    @Test
    public void testCancelButtonShowsDialogAndNavigatesToMainActivity() {
        scenario = ActivityScenario.launch(ScanTepBagsActivity.class);
        scenario.onActivity(activity -> {
            Button cancelButton = activity.findViewById(R.id.cancelButton);
            assertNotNull("Cancel button should not be null", cancelButton);
            cancelButton.performClick();

            // Retrieve the latest AlertDialog (custom dialog).
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("AlertDialog should be displayed on cancel click", dialog);
            assertTrue("Dialog should be an AlertDialog", dialog instanceof AlertDialog);
            AlertDialog alertDialog = (AlertDialog) dialog;

            // Find the confirm button in the dialog layout.
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
    public void testFetchTepBagsAddsItems() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);

        // Set up the mock behavior for fetchTepCodes BEFORE launching the activity.
        doAnswer(invocation -> {
            ApiManager.ApiCallback callback = invocation.getArgument(2);
            // Create a fake JSON array with two TEP items.
            JSONArray jsonArray = new JSONArray("["
                    + "{\"tep_no\":\"TEP001\"},"
                    + "{\"tep_no\":\"TEP002\"}"
                    + "]");
            callback.onSuccess(jsonArray);
            latch.countDown();
            return null;
        }).when(mockApiManager).fetchTepCodes(anyString(), anyString(), any());

        scenario = ActivityScenario.launch(ScanTepBagsActivity.class);

        // Simulate clicking the scanner image to trigger fetchTepBags().
        scenario.onActivity(activity -> {
            activity.findViewById(R.id.scannerImage).performClick();
        });

        // Wait up to 5 seconds for the API callback.
        assertTrue("API callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));

        // Verify that TEP bag items were added to the container.
        scenario.onActivity(activity -> {
            LinearLayout tepListContainer = activity.findViewById(R.id.tepListContainer);
            // Since each TEP item adds a row plus a divider, check that the scannedTepIds list has two items.
            assertEquals("Two TEP bag items should be scanned", 2, activity.scannedTepIds.size());
            // Optionally, also check that the container has children.
            assertTrue("Tep list container should have children", tepListContainer.getChildCount() > 0);
        });
    }

    @Test
    public void testNextButtonWithoutScannedItemsShowsToast() {
        scenario = ActivityScenario.launch(ScanTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Ensure the scanned TEP list is empty.
            activity.scannedTepIds.clear();
            Button nextButton = activity.findViewById(R.id.nextButton);
            assertNotNull("Next button should not be null", nextButton);
            nextButton.performClick();

            // Verify that a toast message is displayed.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Scan at least one TEP bag", toastText);
        });
    }

    @Test
    public void testNextButtonWithScannedItemsNavigatesToConfirmationDialog() {
        scenario = ActivityScenario.launch(ScanTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Simulate that one TEP bag has been scanned.
            activity.scannedTepIds.add("TEP001");
            // Click the next button.
            Button nextButton = activity.findViewById(R.id.nextButton);
            nextButton.performClick();

            // Since validateAndProceed() should show the confirmation dialog,
            // verify that the dialog is displayed.
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("Confirmation dialog should be displayed", dialog);

        });
    }

    @Test
    public void testCallJourneyApiNavigatesOnSuccess() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);

        // Set up mock behavior for callJourney API BEFORE launching the activity.
        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(2);
            // Simulate a JSON response with a transaction_id.
            JSONObject jsonResponse = new JSONObject("{\"data\":{\"transaction_id\":456}}");
            callback.onSuccess(jsonResponse);
            latch.countDown();
            return null;
        }).when(mockApiManager).callJourney(anyString(), any(), any());

        scenario = ActivityScenario.launch(ScanTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Add a scanned TEP so that validateAndProceed() does not show a toast.
            activity.scannedTepIds.add("TEP001");
            // Show the confirmation dialog.
            activity.showConfirmationDialog();

            // Retrieve the confirmation dialog.

            Dialog confirmationDialog = ShadowDialog.getLatestDialog();
            assertNotNull("Confirmation dialog should be displayed", confirmationDialog);

            // Retrieve and click the confirm button in the dialog.
            Button btnConfirm = confirmationDialog.findViewById(R.id.btnConfirmScan);
            assertNotNull("Confirm button should be available", btnConfirm);
            btnConfirm.performClick();
        });

        // Wait for the API callback.
        assertTrue("CallJourney API callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));

        scenario.onActivity(activity -> {
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Next activity intent should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("SignatureRemarksActivity should be started",
                    SignatureRemarksActivity.class.getName(), startedActivityName);
        });
    }

}
