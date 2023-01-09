package com.toochi.facerecognition;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class Recognition {

    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private final Interpreter mInterpreter;
    private final int INPUT_SIZE;
    private CascadeClassifier mCascadeClassifier;
    private String mFaceName;


    public Recognition(Context sContext, AssetManager sAssetManager,
                       String sPath, int sINPUT_SIZE) throws IOException {
        INPUT_SIZE = sINPUT_SIZE;

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        //load model
        mInterpreter = new Interpreter(loadModel(sAssetManager,
                sPath), options);

        Log.d("response", "Model is loaded");

        try {
            InputStream inputStream =
                    sContext.getResources().openRawResource(
                            R.raw.haarcascade_frontalface_alt);
            File cascadePath = sContext.getDir("cascade",
                    Context.MODE_PRIVATE);

            File newCascade = new File(cascadePath,
                    "haarcascade_frontalface_alt");

            FileOutputStream outputStream = new FileOutputStream(newCascade);

            byte[] buffer = new byte[4096];
            int byteRead;

            while ((byteRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            outputStream.close();

            mCascadeClassifier =
                    new CascadeClassifier(newCascade.getAbsolutePath());


        } catch (Resources.NotFoundException sE) {
            sE.printStackTrace();
        }

    }

    //load model method
    private MappedByteBuffer loadModel(AssetManager sAssetManager,
                                       String sPath) throws IOException {

        AssetFileDescriptor assetFileDescriptor = sAssetManager.openFd(sPath);
        FileInputStream inputStream =
                new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel channel = inputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();

        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset,
                declaredLength);

    }

    public Mat detectRealTimeFace(Mat sImage) {

        // flip image to 90 degrees

        Core.flip(sImage.t(), sImage, 1);

        Mat grayScale = new Mat();
        Imgproc.cvtColor(sImage, grayScale, Imgproc.COLOR_RGBA2GRAY);
        int height = grayScale.height();
        int width = grayScale.width();

        int absoluteFaceSize = (int) (height * 0.1);
        MatOfRect ofRectFaces = new MatOfRect();

        if (mCascadeClassifier != null) {
            mCascadeClassifier.detectMultiScale(grayScale, ofRectFaces, 1.1,
                    2, 2, new Size(absoluteFaceSize, absoluteFaceSize));

        }


        Rect[] faceArray = ofRectFaces.toArray();
        for (Rect sRect : faceArray) {

            //do something
            Imgproc.rectangle(sImage, sRect.tl(), sRect.br(),
                    new Scalar(0, 255, 0, 255), 2);

            Rect ofRect = new Rect((int) sRect.tl().x, (int) sRect.tl().y,
                    ((int) sRect.br().x) - ((int) sRect.tl().x),
                    ((int) sRect.br().y) - ((int) sRect.tl().y));

            Mat cropped_rgb = new Mat(sImage, ofRect);

            Bitmap bitmap, scaledBitmap;
            bitmap = Bitmap.createBitmap(cropped_rgb.cols(),
                    cropped_rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgb, bitmap);

            scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE,
                    INPUT_SIZE, false);
            ByteBuffer buffer = convertBitmapToByteBuffer(scaledBitmap);

            float[][] faceValue = new float[1][1];
            mInterpreter.run(buffer, faceValue);

            float readFace = (float) Array.get(
                    Objects.requireNonNull(Array.get(faceValue, 0)), 0);

            String faceName = getFaceName(readFace);

            Imgproc.putText(sImage, "" + faceName,
                    new Point((int) sRect.tl().x + 10, (int) sRect.tl().y + 20),
                    1, 1.5, new Scalar(255, 255, 255, 150), 2);

        }

        Core.flip(sImage.t(), sImage, 0);

        return sImage;
    }

    public Mat detectFaceFromGallery(Mat sRgba, Mat sGray,
                                     int sAbsoluteFaceSize) {

        Imgproc.cvtColor(sRgba, sGray, Imgproc.COLOR_RGBA2GRAY);

        if (sAbsoluteFaceSize == 0) {
            int height = sGray.rows();
            float relativeFaceSize = 0.2f;
            if (Math.round(height * relativeFaceSize) > 0) {
                sAbsoluteFaceSize = Math.round(height * relativeFaceSize);
            }
        }

        if (mCascadeClassifier != null) {

            MatOfRect matOfRect = new MatOfRect();
            mCascadeClassifier.detectMultiScale(sRgba, matOfRect, 1.1, 7, 2,
                    new Size(sAbsoluteFaceSize, sAbsoluteFaceSize));

            for (Rect rect : matOfRect.toArray()) {
                Imgproc.rectangle(sRgba, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width,
                                rect.y + rect.height),
                        FACE_RECT_COLOR, 3, 2);

                Rect ofRect = new Rect((int) rect.tl().x, (int) rect.tl().y,
                        ((int) rect.br().x) - ((int) rect.tl().x),
                        ((int) rect.br().y) - ((int) rect.tl().y));

                Mat cropped_rgb = new Mat(sRgba, ofRect);

                Bitmap bitmap, scaledBitmap;
                bitmap = Bitmap.createBitmap(cropped_rgb.cols(),
                        cropped_rgb.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped_rgb, bitmap);

                scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE,
                        INPUT_SIZE, false);
                ByteBuffer buffer = convertBitmapToByteBuffer(scaledBitmap);

                float[][] faceValue = new float[1][1];
                mInterpreter.run(buffer, faceValue);

                float readFace = (float) Array.get(
                        Objects.requireNonNull(Array.get(faceValue, 0)), 0);

                mFaceName = getFaceName(readFace);

                Imgproc.putText(sRgba, "" + mFaceName,
                        new Point((int) rect.tl().x + 10,
                                (int) rect.tl().y + 20),
                        1, 1.5, new Scalar(255, 255, 255, 150), 2);
            }
        }
        return sRgba;
    }

    private String getFaceName(float sReadFace) {
        String name = "";
        if (sReadFace >= 0 & sReadFace < 0.5) {
            name = "Hardik Pandya";
        } else if (sReadFace >= 0.5 & sReadFace < 1.5) {
            name = "Simon Hedberg";
        } else if (sReadFace >= 1.5 & sReadFace < 2.5) {
            name = "Celestine CSC";
        } else if (sReadFace >= 2.5 & sReadFace < 3.5) {
            name = "Scarlett Johansson";
        } else if (sReadFace >= 3.5 & sReadFace < 4.5) {
            name = "Sylvester Stallone";
        } else if (sReadFace >= 4.5 & sReadFace < 5.5) {
            name = "Lionel Messi";
        } else if (sReadFace >= 5.5 & sReadFace < 6.5) {
            name = "Jim Parsons";
        } else if (sReadFace >= 6.5 & sReadFace < 7.5) {
            name = "Doe";
        } else if (sReadFace >= 7.5 & sReadFace < 8.5) {
            name = "Samson CSC";
        } else if (sReadFace >= 8.5 & sReadFace < 9.5) {
            name = "Mohammed Ali";
        } else if (sReadFace >= 9.5 & sReadFace < 10.5) {
            name = "Brad Pitt";
        } else if (sReadFace >= 10.5 & sReadFace < 11.5) {
            name = "Christiano Ronaldo";
        } else if (sReadFace >= 11.5 & sReadFace < 12.5) {
            name = "Jennifer Anniston";
        } else if (sReadFace >= 12.5 & sReadFace < 13.5) {
            name = "Victor CSC";
        } else if (sReadFace >= 13.5 & sReadFace < 14.5) {
            name = "Dhoni";
        } else if (sReadFace >= 14.5 & sReadFace < 15.5) {
            name = "Pewdiepie";
        } else if (sReadFace >= 15.5 & sReadFace < 16.5) {
            name = "Blessing CSC";
        } else if (sReadFace >= 16.5 & sReadFace < 17.5) {
            name = "Godswill CSC";
        } else if (sReadFace >= 17.5 & sReadFace < 18.5) {
            name = "Johnny Galeck";
        } else {
            name = "Suresh Raina";
        }
        return name;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap sScaledBitmap) {
        ByteBuffer buffer =
                ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());
        int[] values = new int[INPUT_SIZE * INPUT_SIZE];
        sScaledBitmap.getPixels(values, 0, sScaledBitmap.getWidth(), 0, 0,
                sScaledBitmap.getWidth(), sScaledBitmap.getHeight());
        int pixels = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int pi = values[pixels++];
                buffer.putFloat((((pi >> 16) & 0xFF)) / 255.0f);
                buffer.putFloat((((pi >> 8) & 0xFF)) / 255.0f);
                buffer.putFloat(((pi & 0xFF)) / 255.0f);

            }
        }

        return buffer;
    }

    public String getFaceName() {
        return mFaceName;
    }


}
