package com.example.camerasdk.interfaces;

import android.graphics.Bitmap;

import com.example.camerasdk.android.camera.CameraFrameBean;

import org.opencv.core.Mat;

public interface FrameImage {

    public void onCameraFrame(CameraFrameBean inputFrame, Bitmap bitmap);

    public void onCameraFramebitmap(Mat rgba, Mat gray, Bitmap bitmap);
}
