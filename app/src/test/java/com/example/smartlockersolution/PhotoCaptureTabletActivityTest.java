package com.example.smartlockersolution;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {32})
public class PhotoCaptureTabletActivityTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<PhotoCaptureTabletActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Set up TempDataHolder with required values.
        TempDataHolder.setAuthToken("dummyAuthToken");
        // Set role and action. Adjust as needed.
        TempDataHolder.setCurrentRole(TempDataHolder.UserRole.INVESTIGATOR);
        // For this test, we simulate a WITHDRAW scenario.
        TempDataHolder.setCurrentAction(TempDataHolder.UserAction.WITHDRAW);
        TempDataHolder.setTransactionId(123);
        // Set scanned TEP IDs to a non-empty list so that populateTepList() runs.
        TempDataHolder.setScannedTepIds(Arrays.asList("TEP001", "TEP002"));
        // For navigation tests, assume changeLockerSize is false.
        TempDataHolder.setChangeLockerSize(false);

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
    public void testHeaderProfileAndTepListPopulation() {
        scenario = ActivityScenario.launch(PhotoCaptureTabletActivity.class);
        scenario.onActivity(activity -> {
            TextView headerTitle = activity.findViewById(R.id.headerTitle);
            TextView profileName = activity.findViewById(R.id.profileNameTextView);
            LinearLayout tepListContainer = activity.findViewById(R.id.tepListContainer);

            assertNotNull("Header title should not be null", headerTitle);
            assertNotNull("Profile name should not be null", profileName);
            assertNotNull("TEP list container should not be null", tepListContainer);

            // For INVESTIGATOR and WITHDRAW, header should be "Withdraw"
            assertEquals("Withdraw", headerTitle.getText().toString());
            // Profile should be "John Doe, Investigation Officer"
            assertEquals("John Doe, Investigation Officer", profileName.getText().toString());

            // populateTepList() is called in onCreate.
            // Each TEP item is added via addTepItem; check that container has children.
            assertTrue("TEP list container should have items", tepListContainer.getChildCount() > 0);
        });
    }

    @Test
    public void testCancelButtonShowsDialogAndNavigatesToMainActivity() {
        scenario = ActivityScenario.launch(PhotoCaptureTabletActivity.class);
        scenario.onActivity(activity -> {
            Button cancelButton = activity.findViewById(R.id.cancelButton);
            assertNotNull("Cancel button should not be null", cancelButton);
            cancelButton.performClick();

            Dialog cancelDialog = ShadowDialog.getLatestDialog();
            assertNotNull("Locker confirmation dialog should be displayed", cancelDialog);
            assertTrue("Dialog should be an AlertDialog", cancelDialog instanceof AlertDialog);

            // In the cancel dialog layout, simulate clicking the confirm button.
            Button btnConfirm = cancelDialog.findViewById(R.id.btnConfirmScan);
            assertNotNull("Confirm button in cancel dialog should not be null", btnConfirm);
            btnConfirm.performClick();

            // Verify that MainActivity is launched.
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start MainActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("MainActivity should be started",
                    MainActivity.class.getName(), startedActivityName);
        });
    }

    @Test
    public void testScanQRDialogNavigatesToSignatureRemarks() {
        scenario = ActivityScenario.launch(PhotoCaptureTabletActivity.class);
        scenario.onActivity(activity -> {
            // Simulate clicking the scanQR view.
            View scanQR = activity.findViewById(R.id.scanqr);
            assertNotNull("Scan QR view should not be null", scanQR);
            scanQR.performClick();

            Dialog qrDialog = ShadowDialog.getLatestDialog();
            assertNotNull("Locker confirmation dialog should be displayed", qrDialog);
            assertTrue("Dialog should be an AlertDialog", qrDialog instanceof AlertDialog);

            // In the dialog, clicking confirm should navigate to SignatureRemarksActivity.
            Button btnConfirm = qrDialog.findViewById(R.id.btnConfirmScan);
            assertNotNull("Confirm button in QR dialog should not be null", btnConfirm);
            btnConfirm.performClick();

            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start SignatureRemarksActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("SignatureRemarksActivity should be started",
                    SignatureRemarksActivity.class.getName(), startedActivityName);
        });
    }

    @Test
    public void testHandleOpenCellFlowShowsLockerDialogAndNavigates() throws Exception {
        final CountDownLatch openLatch = new CountDownLatch(1);

        // Set up the mock for openLocker before launching the activity.
        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(3);
            JSONObject jsonResponse = new JSONObject("{\"data\":{}}");
            callback.onSuccess(jsonResponse);
            openLatch.countDown();
            return null;
        }).when(mockApiManager).openLocker(anyString(), anyInt(), any(), any());

        scenario = ActivityScenario.launch(PhotoCaptureTabletActivity.class);
        scenario.onActivity(activity -> {
            // Directly call handleOpenCell with a test locker ID.
            int testLockerId = 101;
            activity.handleOpenCell(testLockerId);
        });

        // Wait for openLocker callback.
        assertTrue("openLocker callback not invoked in time", openLatch.await(5, TimeUnit.SECONDS));

        scenario.onActivity(activity -> {
            // Advance the main looper so that delayed tasks are executed.
            Handler mainHandler = activity.getMainLooper() != null
                    ? new Handler(activity.getMainLooper())
                    : new Handler();
            // Use Robolectric's shadow to advance time.
            shadowOf(activity.getMainLooper()).idleFor(1000, TimeUnit.MILLISECONDS);

            // Verify that the locker confirmation dialog is shown.
            Dialog lockerDialog = ShadowDialog.getLatestDialog();
            assertNotNull("Locker confirmation dialog should be displayed", lockerDialog);
            assertTrue("Dialog should be an AlertDialog", lockerDialog instanceof AlertDialog);

            // Simulate clicking the confirm button in the locker dialog.
            Button btnConfirm = lockerDialog.findViewById(R.id.confirmButton);
            assertNotNull("Confirm button in locker dialog should not be null", btnConfirm);
            btnConfirm.performClick();

            // Verify that CompletionActivity is launched.
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start CompletionActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("CompletionActivity should be started",
                    CompletionActivity.class.getName(), startedActivityName);
        });
    }

    @Test
    public void testDoneButtonForWithdraw_NotCompletedUpdatesUI() {
        // For INVESTIGATOR WITHDRAW, when photos are not completed,
        // clicking doneButton should mark photos as completed.
        scenario = ActivityScenario.launch(PhotoCaptureTabletActivity.class);
        scenario.onActivity(activity -> {
            // Ensure photosCompleted is false.
            activity.photosCompleted = false;
            // Simulate initial UI state (scanQR and scanqrIcon visible).
            View scanQR = activity.findViewById(R.id.scanqr);
            View scanqrIcon = activity.findViewById(R.id.scanqrIcon);
            assertEquals(View.VISIBLE, scanQR.getVisibility());
            assertEquals(View.VISIBLE, scanqrIcon.getVisibility());

            Button doneButton = activity.findViewById(R.id.nextButton);
            assertNotNull("Done button should not be null", doneButton);
            doneButton.performClick();

            // After handlePhotoCompletion(), photosCompleted should be true.
            assertTrue("Photos should be marked as completed", activity.photosCompleted);
            // Also, doneButton text should update.
            // For INVESTIGATOR WITHDRAW (and not deposit/redeposit), doneButton text is set to "Complete".
            assertEquals("Complete", doneButton.getText().toString());
            // A toast "Photos marked as completed!" should be shown.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Photos marked as completed!", toastText);
        });
    }

    @Test
    public void testDoneButtonForWithdraw_CompletedNavigatesToCompletion() {
        // For INVESTIGATOR WITHDRAW, if photos are already completed,
        // clicking doneButton should navigate to CompletionActivity.
        scenario = ActivityScenario.launch(PhotoCaptureTabletActivity.class);
        scenario.onActivity(activity -> {
            activity.photosCompleted = true;
            Button doneButton = activity.findViewById(R.id.nextButton);
            doneButton.performClick();

            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start CompletionActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("CompletionActivity should be started",
                    CompletionActivity.class.getName(), startedActivityName);
        });
    }
}
