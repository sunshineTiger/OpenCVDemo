package com.example.opencvdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mCameraView;
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    //mCameraView.enableView();
                    Log.v(Constant.TAG, "成功加载:" + status);
                    if (null != mCameraView) {
                        mCameraView.enableView();
                    }
                    break;

                default:
                    super.onManagerConnected(status);
                    Log.v(Constant.TAG, "加载失败:" + status);
                    break;

            }
        }

        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {
            super.onPackageInstall(operation, callback);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.myJavaCameraView);

        CameraInit();
    }

    private void CameraInit() {
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        mCameraView.setCvCameraViewListener(this);
        mCameraView.enableFpsMeter();
    }


    @Override
    protected void onResume() {
        super.onResume();
        //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        if (!OpenCVLoader.initDebug()) {
            Log.d(Constant.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(Constant.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mCameraView) {
            mCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mCameraView) {
            mCameraView.disableView();
            mCameraView = null;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {
        Log.v(Constant.TAG, "------->>>>>>>onCameraViewStopped");

    }

    /**
     * 图像处理都写在此处
     */

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Log.v(Constant.TAG, "------->>>>>>>onCameraFrame");
        //返回处理后的结果数据
        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
//        Core.transpose(mRgba, mRgbaT);
//        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
//        Core.flip(mRgbaF, mRgba, 1 );

        return mRgba; // This function must return
    }


}
