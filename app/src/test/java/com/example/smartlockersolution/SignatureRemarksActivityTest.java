package com.example.smartlockersolution;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;

import org.json.JSONObject;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@org.robolectric.annotation.Config(sdk = {32})
public class SignatureRemarksActivityTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<SignatureRemarksActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Set up TempDataHolder with valid values.
        TempDataHolder.setAuthToken("dummyAuthToken");
        TempDataHolder.setCurrentRole(TempDataHolder.UserRole.INVESTIGATOR);
        TempDataHolder.setCurrentAction(TempDataHolder.UserAction.WITHDRAW);
        // For testing successful submission, set a valid transaction id.
        TempDataHolder.setTransactionId(123);
        // Also, initialize the scanned TEP IDs list if used elsewhere.
        TempDataHolder.setScannedTepIds(new java.util.ArrayList<>());

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
    public void testCancelButtonShowsDialogAndNavigatesToMainActivity() {
        scenario = ActivityScenario.launch(SignatureRemarksActivity.class);
        scenario.onActivity(activity -> {
            Button btnCancel = activity.findViewById(R.id.btnCancel);
            assertNotNull("Cancel button should not be null", btnCancel);
            btnCancel.performClick();

            // Retrieve the custom cancel dialog.
            Dialog cancelDialog = ShadowDialog.getLatestDialog();
            assertNotNull("AlertDialog should be displayed on cancel click", cancelDialog);
            assertTrue("Dialog should be an AlertDialog", cancelDialog instanceof AlertDialog);


            // Find the confirm button within the dialog layout.
            Button btnConfirm = cancelDialog.findViewById(R.id.btnConfirmScan);
            assertNotNull("Confirm button in cancel dialog should not be null", btnConfirm);
            btnConfirm.performClick();

            // Verify navigation to MainActivity.
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start MainActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("MainActivity should be started",
                    MainActivity.class.getName(), startedActivityName);
        });
    }

    @Test
    public void testNextButtonWithoutSignatureShowsToast() {
        scenario = ActivityScenario.launch(SignatureRemarksActivity.class);
        scenario.onActivity(activity -> {
            // Ensure signature is empty.
            // (Assuming signatureView.isEmpty() returns true if no signature is provided.)
            // Also provide non-empty remarks to avoid that toast.
            EditText etRemarks = activity.findViewById(R.id.etRemarks);
            etRemarks.setText("Some remarks");

            Button btnNext = activity.findViewById(R.id.btnNext);
            btnNext.performClick();

            // Expect a toast indicating that signature is required.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Please provide a signature", toastText);
        });
    }

    @Test
    public void testNextButtonWithoutRemarksShowsToast() {
        scenario = ActivityScenario.launch(SignatureRemarksActivity.class);
        scenario.onActivity(activity -> {
            // Create a dummy signature bitmap.
            Bitmap dummySignature = Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888);
            dummySignature.eraseColor(Color.BLACK);
            // Set the dummy signature into the SignatureView.
            activity.signatureView.setSignature(dummySignature);

            // Ensure the remarks field is empty.
            EditText etRemarks = activity.findViewById(R.id.etRemarks);
            etRemarks.setText("");

            // Click the Next button.
            Button btnNext = activity.findViewById(R.id.btnNext);
            btnNext.performClick();

            // Expect a toast indicating that remarks are required.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Please enter remarks", toastText);
        });
    }



    @Test
    public void testNextButtonSubmissionForWithdrawNavigates() throws Exception {
        final CountDownLatch updateLatch = new CountDownLatch(1);
        final CountDownLatch openLatch = new CountDownLatch(1);

        // Set up mock for updateTransaction.
        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(3);
            JSONObject jsonResponse = new JSONObject("{\"data\":{\"transaction_id\":789}}");
            callback.onSuccess(jsonResponse);
            updateLatch.countDown();
            return null;
        }).when(mockApiManager).updateTransaction(anyString(), anyString(), any(), any());

        // Set up mock for openLocker.
        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(3);
            JSONObject jsonResponse = new JSONObject("{\"data\":{}}");
            callback.onSuccess(jsonResponse);
            openLatch.countDown();
            return null;
        }).when(mockApiManager).openLocker(anyString(), anyInt(), any(), any());

        scenario = ActivityScenario.launch(SignatureRemarksActivity.class);
        scenario.onActivity(activity -> {
            // Create a dummy signature bitmap and fill it with a non-blank color.
            Bitmap dummySignature = Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888);
            dummySignature.eraseColor(Color.BLACK); // Fill with RED so it differs from a blank bitmap.
            activity.signatureView.setSignature(dummySignature);

            // Assert that the signature is now non-empty.
            assertFalse("Signature should not be empty", activity.signatureView.isEmpty());

            // Provide non-empty remarks.
            EditText etRemarks = activity.findViewById(R.id.etRemarks);
            etRemarks.setText("Test remarks");

            // Click the next button.
            Button btnNext = activity.findViewById(R.id.btnNext);
            btnNext.performClick();
        });

        // Wait for updateTransaction and openLocker callbacks.
        assertTrue("updateTransaction callback not invoked in time", updateLatch.await(5, TimeUnit.SECONDS));
        assertTrue("openLocker callback not invoked in time", openLatch.await(5, TimeUnit.SECONDS));

        // Now simulate the confirmation dialog from openLocker.
        scenario.onActivity(activity -> {
            shadowOf(activity.getMainLooper()).idleFor(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            Dialog lockerDialog = ShadowDialog.getLatestDialog();
            assertNotNull("Locker confirmation dialog should be displayed", lockerDialog);
            assertTrue("Dialog should be an AlertDialog", lockerDialog instanceof AlertDialog);

            // Simulate clicking the confirm button in the locker dialog.
            Button btnConfirm = lockerDialog.findViewById(R.id.btnConfirm);
            assertNotNull("Confirm button in locker dialog should not be null", btnConfirm);
            btnConfirm.performClick();
        });

        // Finally, verify that ScanTepBagsActivity is started.
        scenario.onActivity(activity -> {
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent to start ScanTepBagsActivity should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("ScanTepBagsActivity should be started",
                    ScanTepBagsActivity.class.getName(), startedActivityName);
        });
    }
}
