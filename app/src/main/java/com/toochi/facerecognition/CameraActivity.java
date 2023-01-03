package com.toochi.facerecognition;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";
    int PERMISSION_REQUEST_CODE = 0;
    private Mat mRGBA;
    private Mat mGray;
    private CameraBridgeViewBase mCameraBridgeViewBase;
    private Recognition mRecognition;

    private final BaseLoaderCallback mBaseLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    if (status == LoaderCallbackInterface.SUCCESS) {
                        mCameraBridgeViewBase.enableView();

                    }
                    super.onManagerConnected(status);
                }
            };

    public CameraActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        }

        setContentView(R.layout.activity_camera);

        mCameraBridgeViewBase = findViewById(R.id.camera);
        mCameraBridgeViewBase.setCvCameraViewListener(this);
        mCameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);

        try {
            int INPUT_SIZE = 96;
            mRecognition = new Recognition(getApplicationContext(), getAssets(),
                    "best_model" +
                            ".tflite", INPUT_SIZE);
        } catch (IOException sE) {
            sE.printStackTrace();
            Log.d("response", "!loaded");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "loaded");
            mBaseLoaderCallback.onManagerConnected(
                    LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "!loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this,
                    mBaseLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraBridgeViewBase != null) {
            mCameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraBridgeViewBase != null) {
            mCameraBridgeViewBase.disableView();
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        mGray = new Mat(width, height, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mGray = inputFrame.gray();

        mRGBA = mRecognition.detectFace(mRGBA);
        return mRGBA;
    }
}