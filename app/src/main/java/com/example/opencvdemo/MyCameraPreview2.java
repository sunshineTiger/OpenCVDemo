package com.example.opencvdemo;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.FpsMeter;
import org.opencv.core.Mat;

import java.io.IOException;
import java.text.DecimalFormat;

public class MyCameraPreview2 extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {
    private SurfaceHolder mHolder;
    long index;
    private static final DecimalFormat FPS_FORMAT = new DecimalFormat("0.00");
    int mFramesCouner;
    private String mStrfps;
    int STEP = 20;
    private double mFrequency;
    private long mprevFrameTime;
    private Camera mCamera;
    Camera.Parameters parameters;
    DoubleBufferQueue queue = DoubleBufferQueue.getInst();

    public MyCameraPreview2(Context context, Camera camera) {
        super(context);
        //初始化Camera对象
        mCamera = camera;

        // mCamera.autoFocus(this);
        //得到照相机的参数
        parameters = mCamera.getParameters();
        //预览的大小是多少
        // parameters.setPreviewSize(1080, 1920);
        //设置对焦模式，自动对焦

        parameters.setFocusMode(Camera.Parameters.ANTIBANDING_AUTO);
        //得到SurfaceHolder对象
        mHolder = getHolder();
        //添加回调，得到Surface的三个声明周期方法
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {

            //设置预览方向
            mCamera.setDisplayOrientation(90);
            //把这个预览效果展示在SurfaceView上面
            mCamera.setPreviewDisplay(holder);
            //开启预览效果
            mCamera.startPreview();
        } catch (IOException e) {
//            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
        //停止预览效果
        mCamera.stopPreview();
        //重新设置预览效果
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦
                    doAutoFocus();
                }
            }
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }


    private void doAutoFocus() {
        parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(parameters);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    if (!Build.MODEL.equals("KORIDY H30")) {
                        parameters = camera.getParameters();
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                        camera.setParameters(parameters);
                    } else {
                        parameters = camera.getParameters();
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        camera.setParameters(parameters);
                    }
                }
            }
        });
    }

    FpsMeter mFpsMeter = new FpsMeter();
    private Mat[] mFrameChain;
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // Log.v("----->>>>>>", "onPreviewFrame:" + data.length);
        mFramesCouner++;
        if (mFramesCouner % STEP == 0) {
            //Log.v("----->>>>>>", "onPreviewFrame:" + data.length);

            queue.push(new PreviewFrameBean(data, index));
            ++index;
        }

    }
}
