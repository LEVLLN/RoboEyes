package lk.simplecode.kz.mycvapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView textView;
    long time = System.currentTimeMillis();
    float execTime;
    Random rnd = new Random();
    Mat mRgba;
    Mat mRgbaT;
    final int TIME_TO_COUNT = 500000; //ms
    //Update interval in ms. Consider that the screen cannot be updated as often as you want.
//17ms (about 60FPS) sound reasonable
    final int UPDATE_INTERVAL = 17000;
    final int number = 500001; //Can be any number between 0 and Integer.MAX_VALUE;

    int numOfBitmapFile;


    static {
        System.loadLibrary("hello-jni");
    }

    private native String getStringFromNative();

    private BaseLoaderCallback mBaseLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            Log.i("INFO", "OpenCV loaded succesfully");

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        Log.i("HELLO FROM JNI", getStringFromNative());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initDebug();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mBaseLoaderCallback);
        mOpenCvCameraView.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, inputFrame.rgba().size());

        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    numOfBitmapFile = rnd.nextInt(300 + 111);
                    Bitmap original = toBitmap(mRgbaT);
                    Bitmap changed = findPeople(original);

                    saveToJpg(changed, numOfBitmapFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        Log.i("HELLO FROM C++", getStringFromNative());
        return findPeopleMat(inputFrame.gray());

    }

    public Bitmap toBitmap(Mat inputFrame) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(inputFrame.cols(), inputFrame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputFrame, bitmap);

        return bitmap;
    }

    public void saveToJpg(Bitmap bitmap, int num) throws IOException {
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        File file = new File(path, "Finded people" + num + ".png"); // the File to save to jpg
        fOut = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        fOut.flush();
        fOut.close(); // do not forget to close the stream
        MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
    }

    public Bitmap findPeople(Bitmap bitmap) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY, 4);
        HOGDescriptor hog = new HOGDescriptor();
        MatOfFloat descriptors = HOGDescriptor.getDefaultPeopleDetector();
        hog.setSVMDetector(descriptors);
        MatOfRect locations = new MatOfRect();
        MatOfDouble weights = new MatOfDouble();
        hog.detectMultiScale(mat, locations, weights);
        execTime = ((float) (System.currentTimeMillis() - time)) / 1000f;
        Point rectPoint1 = new Point();
        Point rectPoint2 = new Point();
        Scalar fontColor = new Scalar(0, 0, 0);
        Point fontPoint = new Point();
        if (locations.rows() > 0) {
            List<Rect> rectangles = locations.toList();
            int i = 0;
            List<Double> weightList = weights.toList();
            for (Rect rect : rectangles) {
                float weigh = weightList.get(i++).floatValue();

                rectPoint1.x = rect.x;
                rectPoint1.y = rect.y;
                fontPoint.x = rect.x;
                fontPoint.y = rect.y - 4;
                rectPoint2.x = rect.x + rect.width;
                rectPoint2.y = rect.y + rect.height;
                final Scalar rectColor = new Scalar(124, 252, 0);
                // Добавляем на изображения найденную информацию
                Imgproc.rectangle(mat, rectPoint1, rectPoint2, rectColor, 2);
                Imgproc.putText(mat,
                        String.format("%1.2f", weigh),
                        fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
                        2, Core.LINE_AA, false);

            }
        }
        fontPoint.x = 15;
        fontPoint.y = bitmap.getHeight() - 20;
        Imgproc.putText(mat,
                "Processing time:" + execTime + " width:" + bitmap.getWidth() + " height:" + bitmap.getHeight(),
                fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
                2, Core.LINE_AA, false);

        Utils.matToBitmap(mat, bitmap);
        return bitmap;

    }

    public Mat findPeopleMat(Mat mat) {
     //   BitmapFactory.Options opts = new BitmapFactory.Options();
    //    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        //Mat mat = new Mat();
     //   Utils.bitmapToMat(bitmap, mat);
//        Bitmap bitmap;
//        Utils.matToBitmap(mat, bitmap);
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY, 4);
        HOGDescriptor hog = new HOGDescriptor();
        MatOfFloat descriptors = HOGDescriptor.getDefaultPeopleDetector();
        hog.setSVMDetector(descriptors);
        MatOfRect locations = new MatOfRect();
        MatOfDouble weights = new MatOfDouble();
        hog.detectMultiScale(mat, locations, weights);
        execTime = ((float) (System.currentTimeMillis() - time)) / 1000f;
        Point rectPoint1 = new Point();
        Point rectPoint2 = new Point();
        Scalar fontColor = new Scalar(0, 0, 0);
        Point fontPoint = new Point();
        if (locations.rows() > 0) {
            List<Rect> rectangles = locations.toList();
            int i = 0;
            List<Double> weightList = weights.toList();
            for (Rect rect : rectangles) {
                float weigh = weightList.get(i++).floatValue();

                rectPoint1.x = rect.x;
                rectPoint1.y = rect.y;
                fontPoint.x = rect.x;
                fontPoint.y = rect.y - 4;
                rectPoint2.x = rect.x + rect.width;
                rectPoint2.y = rect.y + rect.height;
                final Scalar rectColor = new Scalar(124, 252, 0);
                // Добавляем на изображения найденную информацию
                Imgproc.rectangle(mat, rectPoint1, rectPoint2, rectColor, 2);

            }
        }
//        fontPoint.x = 15;
//        fontPoint.y = bitmap.getHeight() - 20;
//        Imgproc.putText(mat,
//                "Processing time:" + execTime + " width:" + bitmap.getWidth() + " height:" + bitmap.getHeight(),
//                fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
//                2, Core.LINE_AA, false);
//

        return mat;

    }
}