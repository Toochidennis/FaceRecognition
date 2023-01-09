package com.toochi.facerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.splashscreen.SplashScreen;

import com.squareup.picasso.Picasso;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final static int INPUT_SIZE = 96;
    private static final String TAG = "MainActivity";


    private CardView mDetectImage;
    private ImageView mImageView;
    private TextView mTextView;

    private ActivityResultLauncher<String> mResultLauncher;
    private Recognition mRecognition;


    boolean showContent = false;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, " loaded");
        } else {
            Log.d("response ", "!load");
        }
    }


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
        CardView chooseImage = findViewById(R.id.upload_button);
        mDetectImage = findViewById(R.id.detect_button);
        mImageView = findViewById(R.id.image);
        mTextView = findViewById(R.id.face_name);


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

        try {

            mRecognition = new Recognition(
                    getApplicationContext(), getAssets(), "model.tflite",
                    INPUT_SIZE);

        } catch (IOException sE) {
            sE.printStackTrace();
        }

        mResultLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null && !result.getPath().isEmpty()) {

                        Picasso.get().load(result).into(mImageView);
                        mDetectImage.setVisibility(View.VISIBLE);

                    } else {

                        mDetectImage.setVisibility(View.GONE);
                    }

                });


        button.setOnClickListener(sView -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        chooseImage.setOnClickListener(sView -> {

            mResultLauncher.launch("image/*");

            mDetectImage.setVisibility(View.GONE);
            mTextView.setVisibility(View.GONE);
            mImageView.setImageBitmap(null);
            mTextView.setText("");
        });

        mDetectImage.setOnClickListener(sView -> detectFace());

    }

    private void showContentAfterSomeTime() {
        new Handler().postDelayed(() -> showContent = true, 4000);
    }


    private void detectFace() {

        Bitmap bitmap =
                ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
        Mat RGBA = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC4);
        Mat gray = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, RGBA);
        int absoluteFaceSize = (int) (bitmap.getHeight() * 0.2);

        Utils.matToBitmap(mRecognition.detectFaceFromGallery(RGBA, gray
                , absoluteFaceSize), bitmap);

        new Handler().postDelayed(() -> {
            //Convert to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
            byte[] byteArray = stream.toByteArray();
            Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0,
                    byteArray.length);
            mImageView.setImageBitmap(bmp);
            String name = "Name: " + mRecognition.getFaceName();
            mTextView.setText(name);
            mTextView.setVisibility(View.VISIBLE);

        }, 1000);


    }


}