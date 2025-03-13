package com.example.smartlockersolution;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import org.robolectric.android.controller.ActivityController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30) // or use an SDK level supported by Robolectric
public class CameraCaptureActivityTest {

    @Test
    public void testCancelButton_ShowsDialogAndNavigatesToMainActivity() {
        // Launch the activity.
        ActivityController<CameraCaptureActivity> controller =
                Robolectric.buildActivity(CameraCaptureActivity.class).create().start().resume();
        CameraCaptureActivity activity = controller.get();

        // Simulate clicking the cancel button.
        Button cancelButton = activity.findViewById(R.id.cancelButton);
        assertNotNull("Cancel button should not be null", cancelButton);
        cancelButton.performClick();

        // Verify that an AlertDialog is shown.
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("AlertDialog should be displayed on cancel click", dialog);
        assertTrue("Dialog should be an AlertDialog", dialog instanceof AlertDialog);
        AlertDialog alertDialog = (AlertDialog) dialog;

        // Simulate clicking the confirm button in the dialog.
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
    }

    @Test
    public void testOnRequestPermissionsResult_Granted_StartsCamera() {
        // Launch the activity.
        ActivityController<CameraCaptureActivity> controller =
                Robolectric.buildActivity(CameraCaptureActivity.class).create().start().resume();
        CameraCaptureActivity activity = controller.get();

        // Simulate the permission result as granted.
        int requestCode = 100;
        String[] permissions = { Manifest.permission.CAMERA };
        int[] grantResults = { PackageManager.PERMISSION_GRANTED };
        activity.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Verify that the preview view is still present (indicating that camera setup was triggered).
        assertNotNull("PreviewView should exist when permission is granted",
                activity.findViewById(R.id.previewView));
    }

    @Test
    public void testOnRequestPermissionsResult_Denied_FinishesActivity() {
        // Launch the activity.
        ActivityController<CameraCaptureActivity> controller =
                Robolectric.buildActivity(CameraCaptureActivity.class).create().start().resume();
        CameraCaptureActivity activity = controller.get();

        // Simulate the permission result as denied.
        int requestCode = 100;
        String[] permissions = { Manifest.permission.CAMERA };
        int[] grantResults = { PackageManager.PERMISSION_DENIED };
        activity.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Verify that the activity is finishing.
        assertTrue("Activity should finish if camera permission is denied", activity.isFinishing());
    }
}
