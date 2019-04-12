package com.example.camerasdk.unity;

import android.content.Context;
import android.util.Log;

import com.example.camerasdk.DoubleBufferQueue;
import com.example.camerasdk.FrameImageBean;
import com.example.camerasdk.android.camera.CameraPreview;

import java.util.List;

public class UnityManger {
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("opencv_java");
    }

    private static UnityManger manger = new UnityManger();

    public static UnityManger GetInstance() {
        return manger;
    }


    public void Init(Context context, int width, int height) {
        new CameraPreview(width, height, 0);
    }


    private DoubleBufferQueue queue = DoubleBufferQueue.getInst();
    private List<FrameImageBean> readList = queue.getReadList();
    private int textureID;
    private int width;
    private int height;
    private String fps;

    public int GetUnityRenderTexture() {
        Log.i("<<<<<<<<", "获取队列图片textureID:" + textureID + "  width:" + width + "  height:" + height);
        queue.swap();
        readList = queue.getReadList();
        if (readList.size() != 0) {

            FrameImageBean bean = readList.get(0);
            textureID = bean.getUnityRenderTextureID();
            width = bean.getUnityRenderTextureWidth();
            height = bean.getUnityRenderTextureHeight();
            fps = bean.getFps();
            readList.clear();
            Log.i("<<<<<<<<", "获取队列图片textureID:" + textureID + "  width:" + width + "  height:" + height);
            return textureID;
        }
        return -1;
    }

    public int GetUnityRenderTextureWidth() {
        return width;
    }

    public int GetUnityRenderTextureHeight() {
        return height;
    }

    public String GetCameraFrameFps() {
        return fps;
    }

}
