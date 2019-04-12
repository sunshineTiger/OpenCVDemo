package com.example.camerasdk.interfaces;

import android.graphics.Bitmap;

import com.example.camerasdk.android.camera.CameraFrameBean;

public interface FrameImage {

    public void onCameraFrame(CameraFrameBean inputFrame, Bitmap bitmap);

}
