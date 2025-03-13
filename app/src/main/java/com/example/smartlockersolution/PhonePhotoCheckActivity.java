package com.example.smartlockersolution;

import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class PhonePhotoCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_photo_check);

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();  // Closes the activity and returns to the previous screen
            }
        });

        // Start a timer of 2 seconds, then launch the next activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent nextIntent = new Intent(PhonePhotoCheckActivity.this, PhoneFinishedActivity.class);
                startActivity(nextIntent);
                finish();
            }
        }, 2000); // 2000 milliseconds = 2 seconds
    }
}
