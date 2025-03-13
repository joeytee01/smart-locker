package com.example.smartlockersolution;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PhoneTepBagsActivity extends AppCompatActivity {
    private RecyclerView tepBagsList;
    Button completeButton;
    private TepBagAdapter adapter;
    List<TepBag> tepBags = new ArrayList<>();
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    File currentPhotoFile;
    private int currentPosition;
    private boolean isFrontPhoto;
    private final OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_tep_bags);

        tepBagsList = findViewById(R.id.tepBagsList);
        completeButton = findViewById(R.id.completeButton);

        adapter = new TepBagAdapter(tepBags, this::updateCompleteButtonState);
        tepBagsList.setLayoutManager(new LinearLayoutManager(this));
        tepBagsList.setAdapter(adapter);
        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(PhoneTepBagsActivity.this, PhoneScanQRActivity.class);
            startActivity(intent);
            finish();
        });

        completeButton.setOnClickListener(v -> callTepImagesApi());


        Log.d("API_CALL", "fetchTepBags() is being called");
        fetchTepBags();
    }

    // In PhoneTepBagsActivity
    void fetchTepBags() {
        String transactionId = String.valueOf(TempDataHolder.getTransactionId());
        ApiManager.getInstance().fetchTepCodes(TempDataHolder.getAuthToken(), transactionId, "http://192.168.0.197:3000/",new ApiManager.ApiCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                List<TepBag> fetchedBags = new ArrayList<>();
                for (int i = 0; i < data.length(); i++) {
                    try {
                        JSONObject item = data.getJSONObject(i);
                        String tepNo = item.getString("tep_no");
                        boolean frontTaken = item.getBoolean("front");
                        boolean backTaken = item.getBoolean("back");
                        fetchedBags.add(new TepBag(String.valueOf(i + 1), tepNo, frontTaken, backTaken));
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(PhoneTepBagsActivity.this, "Parsing error", Toast.LENGTH_SHORT).show());
                    }
                }
                runOnUiThread(() -> {
                    tepBags.clear();
                    tepBags.addAll(fetchedBags);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(PhoneTepBagsActivity.this, "Network Error: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    void updateCompleteButtonState() {
        boolean allPhotosTaken = true;
        for (TepBag bag : tepBags) {
            if (bag.frontPhoto == null || bag.backPhoto == null) {
                allPhotosTaken = false;
                break;
            }
        }

        completeButton.setEnabled(allPhotosTaken);
        completeButton.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(allPhotosTaken ? R.color.green_success : R.color.gray)
        ));
    }

    public static class TepBag {
        public String number;
        public String id;
        public Bitmap frontPhoto;
        public Bitmap backPhoto;
        public boolean frontTaken;
        public boolean backTaken;

        public TepBag(String number, String id, boolean frontTaken, boolean backTaken) {
            this.number = number;
            this.id = id;
            this.frontTaken = frontTaken;
            this.backTaken = backTaken;
        }
    }

    private class TepBagAdapter extends RecyclerView.Adapter<TepBagAdapter.ViewHolder> {
        private List<TepBag> bags;
        private Runnable updateCallback;

        public TepBagAdapter(List<TepBag> bags, Runnable updateCallback) {
            this.bags = bags;
            this.updateCallback = updateCallback;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_phone_tep_bag, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TepBag bag = bags.get(position);
            holder.bagNumber.setText(bag.number + ".");
            holder.bagId.setText(bag.id);

            // Update buttons with correct images or "+" if null
            updateButton(holder.frontButton, bag.frontPhoto);
            updateButton(holder.backButton, bag.backPhoto);

            holder.frontButton.setOnClickListener(v -> capturePhoto(position, true));
            holder.backButton.setOnClickListener(v -> capturePhoto(position, false));
        }



        // Modify capturePhoto method
        private void capturePhoto(int position, boolean isFront) {
            currentPosition = position;
            isFrontPhoto = isFront;

            // Check permissions
            if (ContextCompat.checkSelfPermission(PhoneTepBagsActivity.this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PhoneTepBagsActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CODE);
                return;
            }

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    currentPhotoFile = createImageFile();
                    Uri photoURI = FileProvider.getUriForFile(PhoneTepBagsActivity.this,
                            getPackageName() + ".fileprovider",
                            currentPhotoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (IOException ex) {
                    Toast.makeText(PhoneTepBagsActivity.this, "Error creating file", Toast.LENGTH_SHORT).show();
                }
            }
        }


        private File createImageFile() throws IOException {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "TEP_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        }


        private int createRequestCode(int position, boolean isFront) {
            return (position << 1) | (isFront ? 1 : 0);
        }

        @Override
        public int getItemCount() {
            return bags.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView bagNumber, bagId;
            ImageButton frontButton, backButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                bagNumber = itemView.findViewById(R.id.bagNumber);
                bagId = itemView.findViewById(R.id.bagId);
                frontButton = itemView.findViewById(R.id.frontButton);
                backButton = itemView.findViewById(R.id.backButton);
            }
        }
    }

    void callTepImagesApi() {
        String transactionId = String.valueOf(TempDataHolder.getTransactionId());
        String firebaseId = "dhekioqi2y7ZA"; // Or retrieve dynamically if needed.
        String authToken = TempDataHolder.getAuthToken();

        ApiManager.getInstance().callTepImages(authToken, transactionId, firebaseId, tepBags, "http://192.168.0.197:3000/", new ApiManager.ApiObjectCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                runOnUiThread(() -> {
                    try {
                        boolean status = data.getBoolean("status");
                        String message = data.getString("message");
                        Toast.makeText(PhoneTepBagsActivity.this, message, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PhoneTepBagsActivity.this, PhonePhotoCheckActivity.class));
                    } catch (Exception e) {
                        Toast.makeText(PhoneTepBagsActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(PhoneTepBagsActivity.this, "Failed to submit images: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (currentPhotoFile != null) {
                Log.d("ImageCapture", "Photo file path: " + currentPhotoFile.getAbsolutePath());

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // Reduce memory usage
                final Bitmap originalBitmap = BitmapFactory.decodeFile(currentPhotoFile.getAbsolutePath(), options);

                if (originalBitmap == null) {
                    Log.e("ImageCapture", "Failed to load image from file");
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Fix rotation
                final Bitmap rotatedBitmap = rotateImageIfRequired(originalBitmap, currentPhotoFile.getAbsolutePath());
                Log.d("ImageCapture", "Bitmap loaded successfully");

                // Assign to the correct bag and update UI on the main thread
                runOnUiThread(() -> {
                    TepBag bag = tepBags.get(currentPosition);
                    if (isFrontPhoto) {
                        bag.frontPhoto = rotatedBitmap;
                        Log.d("ImageCapture", "Assigned to FRONT photo of bag " + bag.id);
                    } else {
                        bag.backPhoto = rotatedBitmap;
                        Log.d("ImageCapture", "Assigned to BACK photo of bag " + bag.id);
                    }

                    adapter.notifyItemChanged(currentPosition); // Ensure UI update
                    updateCompleteButtonState();
                });
            }
        }
    }


    // Add image rotation fix
    private Bitmap rotateImageIfRequired(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }
    private void updateButton(ImageButton button, Bitmap photo) {
        if (photo != null) {
            button.setImageBitmap(photo);
        } else {
            button.setImageResource(R.drawable.ic_add); // Default "+" icon
        }
        button.invalidate();  // Force redraw
        button.requestLayout(); // Ensure layout update
    }





}