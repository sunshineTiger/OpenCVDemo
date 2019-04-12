package com.example.camerasdk.android.camera;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraFrameBean implements CameraBridgeViewBase.CvCameraViewFrame {
    @Override
    public Mat gray() {
        return mYuvFrameData.submat(0, mHeight, 0, mWidth);
    }

    @Override
    public Mat rgba() {
        Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
        return mRgba;
    }

    public CameraFrameBean(Mat Yuv420sp, int width, int height) {
        super();
        mWidth = width;
        mHeight = height;
        mYuvFrameData = Yuv420sp;
        mRgba = new Mat();
    }

    public void release() {
        mRgba.release();
    }

    private Mat mYuvFrameData;
    private Mat mRgba;
    private int mWidth;
    private int mHeight;
}
