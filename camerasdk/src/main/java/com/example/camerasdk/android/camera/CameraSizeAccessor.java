package com.example.camerasdk.android.camera;

import android.hardware.Camera;

import com.example.camerasdk.interfaces.CameraListItemAccessor;

public class CameraSizeAccessor implements CameraListItemAccessor {
    @Override
    public int getWidth(Object obj) {
        Camera.Size size = (Camera.Size) obj;
        return size.width;
    }

    @Override
    public int getHeight(Object obj) {
        Camera.Size size = (Camera.Size) obj;
        return size.height;
    }
}
