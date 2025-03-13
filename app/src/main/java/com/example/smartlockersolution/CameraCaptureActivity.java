package com.example.smartlockersolution;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraCaptureActivity extends AppCompatActivity {

    private static final String TAG = "CameraCaptureActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private PreviewView previewView;
    private Button cancelButton, captureButton;

    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture);

        previewView = findViewById(R.id.previewView);
        cancelButton = findViewById(R.id.cancelButton);
        captureButton = findViewById(R.id.captureButton);

        // Check permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        cancelButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(CameraCaptureActivity.this, R.style.CustomDialogTheme);
            View dialogView = LayoutInflater.from(CameraCaptureActivity.this).inflate(R.layout.dialog_cancel, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            Button btnConfirm = dialogView.findViewById(R.id.btnConfirmScan);
            Button btnBack = dialogView.findViewById(R.id.btnBack);
            btnConfirm.setOnClickListener(v1 -> {

                Intent intent = new Intent(CameraCaptureActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            btnBack.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });


        // Capture: take a photo
        captureButton.setOnClickListener(view -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        Executor executor = getSafeExecutor();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, executor);
    }

    private void bindCameraUseCases() {
        // Unbind any existing use cases before rebinding
        cameraProvider.unbindAll();

        // Preview use case
        Preview preview = new Preview.Builder().build();
        // Image capture use case
        imageCapture = new ImageCapture.Builder().build();

        // Select front camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (IllegalArgumentException e) {
            // Fallback to the back camera if front camera is not available
            Log.e(TAG, "Front camera not found, switching to back camera.");
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        }


        // Attach preview to previewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Bind use cases to lifecycle
        Camera camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
        );
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // Create a file to store the image (for demo, storing in cache dir)
        File photoFile = new File(getCacheDir(), "captured_image.jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        Executor executor = getSafeExecutor();

        imageCapture.takePicture(
                outputOptions,
                executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(CameraCaptureActivity.this,
                                "Photo captured: " + photoFile.getAbsolutePath(),
                                Toast.LENGTH_SHORT).show();
                        if(TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR && TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.DEPOSIT_REDEPOSIT) {

                            Intent intent = new Intent(CameraCaptureActivity.this, LockerSelectionActivity.class);
                            startActivity(intent);
                        } else if (TempDataHolder.getCurrentRole() == TempDataHolder.UserRole.INVESTIGATOR && TempDataHolder.getCurrentAction() == TempDataHolder.UserAction.WITHDRAW) {
                            Intent intent = new Intent(CameraCaptureActivity.this, ScanTepBagsActivity.class);
                            intent.putExtra("isLockerOpened", false);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraCaptureActivity.this,
                                "Capture failed: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onError: ", exception);
                    }
                }
        );
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Get a safe executor for API 24+
    private Executor getSafeExecutor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getMainExecutor();
        } else {
            return command -> new Handler(Looper.getMainLooper()).post(command);
        }
    }
}
