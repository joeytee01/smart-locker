package com.example.smartlockersolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {32})
public class DivisionPassTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<DivisionPass> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Set a valid role and action for testing.
        TempDataHolder.setCurrentRole(TempDataHolder.UserRole.INVESTIGATOR);
        TempDataHolder.setCurrentAction(TempDataHolder.UserAction.DEPOSIT_REDEPOSIT);
        ApiManager.setInstance(mockApiManager);
    }

    @Test
    public void testUpdateDateTime() {
        scenario = ActivityScenario.launch(DivisionPass.class);
        scenario.onActivity(activity -> {
            TextView dateTimeTextView = activity.findViewById(R.id.dateTimeTextView);
            assert (dateTimeTextView.getText().toString().matches(".*\\d{2}:\\d{2} [AP]M"));
        });
    }

    @Test
    public void testScanImageButtonClick_Success() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        scenario = ActivityScenario.launch(DivisionPass.class);
        scenario.onActivity(activity -> {
            ImageButton scanImageButton = activity.findViewById(R.id.scanImageButton);

            // Prepare the mock JSON response.
            JSONObject mockResponse = new JSONObject();
            try {
                JSONObject data = new JSONObject();
                data.put("api_token", "mockToken123");
                mockResponse.put("data", data);
            } catch (JSONException e) {
                throw new RuntimeException("JSON construction failed", e);
            }

            doAnswer(invocation -> {
                // Call the success callback
                ((ApiManager.ApiObjectCallback) invocation.getArgument(4)).onSuccess(mockResponse);
                latch.countDown(); // Signal that the callback has been invoked.
                return null;
            }).when(mockApiManager).performLogin(any(), any(), any(), any(), any());

            // Perform the click that triggers the API call.
            scanImageButton.performClick();
        });

        // Wait for the API callback to complete.
        assertTrue("Callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));
        // Now verify that the token was stored correctly.
        assertEquals("Bearer mockToken123", TempDataHolder.getAuthToken());
    }


    @Test
    public void testScanImageButtonClick_Failure() {
        scenario = ActivityScenario.launch(DivisionPass.class);
        scenario.onActivity(activity -> {
            ImageButton scanImageButton = activity.findViewById(R.id.scanImageButton);

            doAnswer(invocation -> {
                ((ApiManager.ApiObjectCallback) invocation.getArgument(4)).onFailure("Login Failed");
                return null;
            }).when(mockApiManager).performLogin(any(), any(), any(), any(), any());

            scanImageButton.performClick();

            verify(mockApiManager).performLogin(any(), any(), any(), any(), any());
        });
    }

    @Test
    public void testBackButtonFinishesActivity() {
        scenario = ActivityScenario.launch(DivisionPass.class);
        scenario.onActivity(activity -> {
            Button backButton = activity.findViewById(R.id.backButton);
            backButton.performClick();
            assert (activity.isFinishing());
        });
    }

    @Test
    public void testScanImageButtonClick_InvalidToken() {
        scenario = ActivityScenario.launch(DivisionPass.class);
        scenario.onActivity(activity -> {
            ImageButton scanImageButton = activity.findViewById(R.id.scanImageButton);

            JSONObject mockResponse = new JSONObject();
            try {
                JSONObject data = new JSONObject();
                data.put("api_token", ""); // Invalid token
                mockResponse.put("data", data);
            } catch (JSONException e) {
                throw new RuntimeException("JSON construction failed", e);
            }

            doAnswer(invocation -> {
                ((ApiManager.ApiObjectCallback) invocation.getArgument(4)).onSuccess(mockResponse);
                return null;
            }).when(mockApiManager).performLogin(any(), any(), any(), any(), any());

            scanImageButton.performClick();

            // The token should not be set if the response contains an empty token.
            assertEquals("Bearer ", TempDataHolder.getAuthToken());

        });
    }

}
