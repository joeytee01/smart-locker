package com.example.smartlockersolution;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoTutorialsActivity extends AppCompatActivity {

    private TextView timerTextView;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        LinearLayout scrollContent = findViewById(R.id.scrollContent);

        for (int i = 0; i < scrollContent.getChildCount(); i++) {
            View item = scrollContent.getChildAt(i);
            if (item instanceof LinearLayout) {
                // This is the vertical layout
                LinearLayout verticalLayout = (LinearLayout) item;

                View row = verticalLayout.getChildAt(0);
                if (row instanceof LinearLayout) {
                    LinearLayout rowLayout = (LinearLayout) row;

                    Button playButton = (Button) rowLayout.getChildAt(1);

                    final int position = i + 1; // example: tutorial # based on index
                    playButton.setOnClickListener(v -> playVideo(position));
                }
            }
        }

        TempDataHolder.attachTimerTextView(timerTextView, this);
        TempDataHolder.startTimer();
    }

    private void playVideo(int tutorialNumber) {
        // Open YouTube video in default app or browser
        Uri youtubeUri = Uri.parse("https://www.youtube.com/watch?v=a3ICNMQW7Ok");
        Intent intent = new Intent(Intent.ACTION_VIEW, youtubeUri);

        // Verify there's an app to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback behavior if no app can handle the intent
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("No app found to open YouTube links")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TempDataHolder.attachTimerTextView(timerTextView, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TempDataHolder.attachTimerTextView(null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TempDataHolder.resetTimer();
    }
}
