package com.example.smartlockersolution;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {32})
public class EmailSelectionActivityTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<EmailSelectionActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ApiManager.setInstance(mockApiManager); // Inject mock API manager
    }

    @Test
    public void testFetchEmails_Success() throws Exception {
        JSONObject mockResponse = new JSONObject();
        JSONObject data = new JSONObject();
        JSONArray emailArray = new JSONArray();
        List<String> mockEmails = Arrays.asList("test1@example.com", "test2@example.com");

        for (String email : mockEmails) {
            emailArray.put(email);
        }
        data.put("emails", emailArray);
        mockResponse.put("status", true);
        mockResponse.put("data", data);

        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(2);
            callback.onSuccess(mockResponse);
            return null;
        }).when(mockApiManager).fetchEmails(anyString(), anyString(), any());

        scenario = ActivityScenario.launch(EmailSelectionActivity.class);
        scenario.onActivity(activity -> {
            Spinner emailSpinner = activity.findViewById(R.id.emailSpinner);
            assertNotNull(emailSpinner);
            assertEquals(2, emailSpinner.getAdapter().getCount());
        });
    }

    @Test
    public void testFetchEmails_Failure() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(2);
            callback.onFailure("API error");
            latch.countDown();
            return null;
        }).when(mockApiManager).fetchEmails(anyString(), anyString(), any());

        scenario = ActivityScenario.launch(EmailSelectionActivity.class);

        // Wait until the failure callback has been executed.
        assertTrue("API failure callback not invoked", latch.await(6, TimeUnit.SECONDS));

        scenario.onActivity(activity -> {
            Spinner emailSpinner = activity.findViewById(R.id.emailSpinner);
            assertNotNull("Email spinner should not be null", emailSpinner);

            // Advance the main looper to let runOnUiThread tasks complete.
            Shadows.shadowOf(activity.getMainLooper()).idleFor(3000, TimeUnit.MILLISECONDS);

            // Now, the adapter should be set—even if it's empty.
            assertNotNull("Spinner adapter should not be null", emailSpinner.getAdapter());
            assertEquals("Email spinner should have 0 items", 0, emailSpinner.getAdapter().getCount());
        });
    }


    @Test
    public void testCancelButton_ShowsDialogAndNavigates() {
        scenario = ActivityScenario.launch(EmailSelectionActivity.class);
        scenario.onActivity(activity -> {
            // Click the cancel button to show the dialog.
            Button cancelButton = activity.findViewById(R.id.cancelButton);
            assertNotNull("Cancel button should not be null", cancelButton);
            cancelButton.performClick();

            // Retrieve the dialog using ShadowAlertDialog.
            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("Cancel confirmation dialog should be displayed", dialog);

            // Now get the confirm button from the dialog's view.
            Button btnConfirm = dialog.findViewById(R.id.btnConfirmScan);
            assertNotNull("Confirm button in cancel dialog should not be null", btnConfirm);
            btnConfirm.performClick();

            // Verify that the activity finishes (or navigates to MainActivity).
            assertTrue("Activity should finish after confirming cancel", activity.isFinishing());
        });
    }


    @Test
    public void testNextButton_WithSelection() {
        scenario = ActivityScenario.launch(EmailSelectionActivity.class);
        scenario.onActivity(activity -> {
            // Create a dummy adapter for the spinner.
            List<String> emails = Arrays.asList("test1@example.com", "test2@example.com");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, emails);
            Spinner emailSpinner = activity.findViewById(R.id.emailSpinner);
            emailSpinner.setAdapter(adapter);

            // Manually set the selected email.
            activity.selectedEmail = "test2@example.com";

            Button nextButton = activity.findViewById(R.id.nextButton);
            nextButton.performClick();

            Intent actualIntent = shadowOf(activity).getNextStartedActivity();
            assertNotNull("Next activity intent should not be null", actualIntent);
            assertEquals(LockerSelectionActivity.class.getName(), actualIntent.getComponent().getClassName());
        });
    }

}
