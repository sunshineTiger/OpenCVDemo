package com.example.opencvdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mCameraView;

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
        mCameraView.setCameraIndex(0);//0前置 1后置
        mCameraView.setCvCameraViewListener(this);
        mCameraView.enableFpsMeter();
//        if (mCameraView != null) {
//            mCameraView.disableView();
//        }
//        mCameraView.enableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mCameraView.enableView();
//        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        if (!OpenCVLoader.initDebug()) {
            Log.d(Constant.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(Constant.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }



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

    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.v(Constant.TAG, "------->>>>>>>onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        Log.v(Constant.TAG, "------->>>>>>>onCameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.v(Constant.TAG, "------->>>>>>>onCameraFrame");
        return inputFrame.rgba();
    }
}