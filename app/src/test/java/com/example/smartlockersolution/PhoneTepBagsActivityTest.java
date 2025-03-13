package com.example.smartlockersolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@org.robolectric.annotation.Config(sdk = {32})
public class PhoneTepBagsActivityTest {

    @Mock
    private ApiManager mockApiManager;

    private ActivityScenario<PhoneTepBagsActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Inject the mock ApiManager before launching the activity.
        ApiManager.setInstance(mockApiManager);
        // Set up dummy TempDataHolder values.
        TempDataHolder.setTransactionId(123);
        TempDataHolder.setAuthToken("dummyAuthToken");
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    // Test that fetchTepBags() updates the RecyclerView and internal list properly.
    @Test
    public void testFetchTepBagsUpdatesRecyclerView() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        // Set up the mock for fetchTepCodes to simulate a successful API call.
        doAnswer(invocation -> {
            ApiManager.ApiCallback callback = invocation.getArgument(3);
            // Create a JSON array with two TEP bag items.
            JSONArray jsonArray = new JSONArray("[" +
                    "{\"tep_no\":\"TEP001\", \"front\":false, \"back\":false}," +
                    "{\"tep_no\":\"TEP002\", \"front\":true, \"back\":false}" +
                    "]");
            callback.onSuccess(jsonArray);
            latch.countDown();
            return null;
        }).when(mockApiManager).fetchTepCodes(anyString(), anyString(), anyString(), any());

        scenario = ActivityScenario.launch(PhoneTepBagsActivity.class);
        // Wait for the API callback.
        assertTrue("API callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.tepBagsList);
            assertNotNull("RecyclerView should not be null", recyclerView);
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            assertNotNull("Adapter should not be null", adapter);
            // The JSON response had two items.
            assertEquals("There should be 2 TepBags", 2, activity.tepBags.size());
            assertEquals("RecyclerView should display 2 items", 2, adapter.getItemCount());
        });
    }

    // Test that updateCompleteButtonState() disables the button when photos are missing
    // and enables it when all photos are provided.
    @Test
    public void testUpdateCompleteButtonState() {
        scenario = ActivityScenario.launch(PhoneTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Create a dummy bitmap.
            Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);

            // Clear any pre-existing bags and add a new one.
            activity.tepBags.clear();
            PhoneTepBagsActivity.TepBag bag = new PhoneTepBagsActivity.TepBag("1", "TEP001", false, false);
            activity.tepBags.add(bag);

            // Initially, with no photos, completeButton should be disabled.
            activity.updateCompleteButtonState();
            Button completeButton = activity.findViewById(R.id.completeButton);
            assertFalse("Complete button should be disabled when photos are missing", completeButton.isEnabled());

            // Assign dummy photos for both front and back.
            bag.frontPhoto = dummyBitmap;
            bag.backPhoto = dummyBitmap;
            activity.updateCompleteButtonState();
            assertTrue("Complete button should be enabled when all photos are taken", completeButton.isEnabled());
        });
    }

    // Test that clicking the back button navigates to PhoneScanQRActivity.
    @Test
    public void testBackButtonNavigatesToPhoneScanQRActivity() {
        scenario = ActivityScenario.launch(PhoneTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Simulate a click on the back button.
            activity.findViewById(R.id.backButton).performClick();
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Intent should be fired on back button click", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("PhoneScanQRActivity should be started",
                    PhoneScanQRActivity.class.getName(), startedActivityName);
        });
    }

    // Test that callTepImagesApi() navigates to PhonePhotoCheckActivity on a successful API call.
    @Test
    public void testCallTepImagesApiNavigatesOnSuccess() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        // Set up the mock for callTepImages to simulate a successful API response.
        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(5);
            JSONObject jsonResponse = new JSONObject("{\"status\":true, \"message\":\"Success\"}");
            callback.onSuccess(jsonResponse);
            latch.countDown();
            return null;
        }).when(mockApiManager).callTepImages(anyString(), anyInt(), anyString(), any(), anyString(), any());

        scenario = ActivityScenario.launch(PhoneTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Ensure that there is at least one TepBag with photos so that the complete button is enabled.
            Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
            activity.tepBags.clear();
            PhoneTepBagsActivity.TepBag bag = new PhoneTepBagsActivity.TepBag("1", "TEP001", false, false);
            bag.frontPhoto = dummyBitmap;
            bag.backPhoto = dummyBitmap;
            activity.tepBags.add(bag);
            activity.updateCompleteButtonState();

            // Simulate clicking the complete button.
            activity.findViewById(R.id.completeButton).performClick();
        });

        // Wait for the API callback.
        assertTrue("CallTepImages API callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));

        scenario.onActivity(activity -> {
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertNotNull("Next activity intent should be fired", startedIntent);
            String startedActivityName = startedIntent.getComponent().getClassName();
            assertEquals("PhonePhotoCheckActivity should be started",
                    PhonePhotoCheckActivity.class.getName(), startedActivityName);
            // Verify that a Toast with message "Success" is displayed.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Success", toastText);
        });
    }

    @Test
    public void testCallTepImagesApiNavigatesOnFailure() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        // Set up the mock for callTepImages to simulate a failed API response.
        doAnswer(invocation -> {
            ApiManager.ApiObjectCallback callback = invocation.getArgument(5);
            callback.onFailure("Network Error");
            latch.countDown();
            return null;
        }).when(mockApiManager).callTepImages(anyString(), anyInt(), anyString(), any(), anyString(), any());

        scenario = ActivityScenario.launch(PhoneTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Ensure that there is at least one TepBag with photos so that the complete button is enabled.
            Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
            activity.tepBags.clear();
            PhoneTepBagsActivity.TepBag bag = new PhoneTepBagsActivity.TepBag("1", "TEP001", false, false);
            bag.frontPhoto = dummyBitmap;
            bag.backPhoto = dummyBitmap;
            activity.tepBags.add(bag);
            activity.updateCompleteButtonState();

            // Simulate clicking the complete button.
            activity.findViewById(R.id.completeButton).performClick();
        });

        // Wait for the API callback.
        assertTrue("CallTepImages API callback was not invoked in time", latch.await(5, TimeUnit.SECONDS));

        scenario.onActivity(activity -> {
            // Verify that no navigation occurs.
            ShadowActivity shadowActivity = shadowOf(activity);
            Intent startedIntent = shadowActivity.getNextStartedActivity();
            assertTrue("No navigation should occur on API failure", startedIntent == null);
            // Verify that a Toast with the error message is displayed.
            String toastText = ShadowToast.getTextOfLatestToast();
            assertEquals("Failed to submit images: Network Error", toastText);
        });
    }

    @Test
    public void testCompleteButtonDisabledWhenOnePhotoIsMissing() {
        scenario = ActivityScenario.launch(PhoneTepBagsActivity.class);
        scenario.onActivity(activity -> {
            // Create a dummy bitmap.
            Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);

            // Clear existing TepBags and add a new one with only the front photo set.
            activity.tepBags.clear();
            PhoneTepBagsActivity.TepBag bag = new PhoneTepBagsActivity.TepBag("1", "TEP001", false, false);
            bag.frontPhoto = dummyBitmap; // Only front photo is provided; back photo remains null.
            activity.tepBags.add(bag);

            // Update the state of the complete button.
            activity.updateCompleteButtonState();
            Button completeButton = activity.findViewById(R.id.completeButton);

            // Assert that the complete button is disabled when not all photos are taken.
            assertFalse("Complete button should be disabled if not all photos are taken", completeButton.isEnabled());
        });
    }

}
