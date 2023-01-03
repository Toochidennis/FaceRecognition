package com.toochi.facerecognition;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.splashscreen.SplashScreen;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d("response ", " loaded");
        } else {
            Log.d("response ", "!load");
        }
    }

    boolean showContent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Face Recognition");
        //actionBar.setDisplayHomeAsUpEnabled(true);

        CardView button = findViewById(R.id.camera_btn);
        button.setOnClickListener(sView -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (showContent) {
                            content.getViewTreeObserver().removeOnPreDrawListener(
                                    this);
                        }
                        showContentAfterSomeTime();
                        return false;
                    }
                });
    }

    private void showContentAfterSomeTime() {
        new Handler().postDelayed(() -> showContent = true, 4000);
    }

}