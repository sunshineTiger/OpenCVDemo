package com.example.camerasdk;

import android.graphics.Bitmap;

public class FrameImageBean {
    private Bitmap bitmap;
    private int UnityRenderTextureID;
    private int UnityRenderTextureWidth;
    private int UnityRenderTextureHeight;
    private String fps;

    public FrameImageBean(int unityRenderTextureID, int unityRenderTextureWidth, int unityRenderTextureHeight, String fps) {
        UnityRenderTextureID = unityRenderTextureID;
        UnityRenderTextureWidth = unityRenderTextureWidth;
        UnityRenderTextureHeight = unityRenderTextureHeight;
        fps = fps;
    }

    public FrameImageBean(Bitmap bitmap, int unityRenderTextureID, int unityRenderTextureWidth, int unityRenderTextureHeight, String fps) {
        this.bitmap = bitmap;
        UnityRenderTextureID = unityRenderTextureID;
        UnityRenderTextureWidth = unityRenderTextureWidth;
        UnityRenderTextureHeight = unityRenderTextureHeight;
        this.fps = fps;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getFps() {
        return fps;
    }

    public int getUnityRenderTextureID() {
        return UnityRenderTextureID;
    }

    public int getUnityRenderTextureWidth() {
        return UnityRenderTextureWidth;
    }

    public int getUnityRenderTextureHeight() {
        return UnityRenderTextureHeight;
    }
}
