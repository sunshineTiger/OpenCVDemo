package com.example.camerasdk.android.camera;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;

import com.example.camerasdk.DoubleBufferQueue;
import com.example.camerasdk.FrameImageBean;
import com.example.camerasdk.interfaces.CameraListItemAccessor;
import com.example.camerasdk.interfaces.FrameImage;

import org.opencv.BuildConfig;
import org.opencv.android.FpsMeter;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.Deque;
import java.util.List;

public class CameraPreview implements Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";
    private static final int MAGIC_TEXTURE_ID = 10;//默认TEXTURE_ID
    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    protected FpsMeter mFpsMeter = null;
    private Thread mThread;//每帧处理线程
    private boolean mStopThread;//停止标志
    protected Camera mCamera;
    protected CameraFrameBean[] mCameraFrame;
    private SurfaceTexture mSurfaceTexture;
    private int cameraId;
    private boolean mCameraFrameReady = false;
    private int width;
    private int height;
    public static final int CAMERA_ID_ANY = -1;
    public static final int CAMERA_ID_BACK = 99;
    public static final int CAMERA_ID_FRONT = 98;
    public static final int RGBA = 1;
    public static final int GRAY = 2;
    protected int mCameraIndex = CAMERA_ID_ANY;
    protected int mFrameWidth;
    protected int mFrameHeight;
    protected int mMaxHeight;
    protected int mMaxWidth;
    private Bitmap mCacheBitmap;
    private static final int MAX_UNSPECIFIED = -1;
    private FrameImage frameImageListener;

    public CameraPreview(int width, int height, int cameraId) {
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;
        connectCamera(width, height);
    }

    public CameraPreview(int width, int height, int cameraId, FrameImage frameImageListener) {
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;
        this.frameImageListener = frameImageListener;
        connectCamera(width, height);
    }

    private boolean connectCamera(int width, int height) {
        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height))
            return false;
        mCameraFrameReady = false;
        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mThread = new Thread(new CameraWorker());
        mThread.start();

        return true;
    }

    public void destoryCamera() {
        disconnectCamera();
    }

    private void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Wating for thread");
            if (mThread != null)
                mThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread = null;

        }

        /* Now release camera */
        releaseCamera();

        mCameraFrameReady = false;
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
    }


    /**
     * 初始化相机
     *
     * @param width
     * @param height
     * @return
     */
    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            mCamera = null;

            if (mCameraIndex == CAMERA_ID_ANY) {
                Log.d(TAG, "Trying to open camera with old open()");
                try {
                    mCamera = Camera.open();
                } catch (Exception e) {
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
                }

                if (mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    boolean connected = false;
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                        try {
                            mCamera = Camera.open(camIdx);
                            connected = true;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                        }
                        if (connected) break;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    int localCameraIndex = mCameraIndex;
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo(camIdx, cameraInfo);
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo(camIdx, cameraInfo);
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!");
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!");
                    } else {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(localCameraIndex) + ")");
                        try {
                            mCamera = Camera.open(localCameraIndex);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                        }
                    }
                }
            }

            if (mCamera == null)
                return false;

            /* Now set camera parameters */
            try {
                Camera.Parameters params = mCamera.getParameters();
                Log.d(TAG, "getSupportedPreviewSizes()");
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();

                if (sizes != null) {
                    /* Select the size that fits surface considering maximum size allowed */
                    Size frameSize = calculateCameraFrameSize(sizes, new CameraSizeAccessor(), width, height);

                    params.setPreviewFormat(ImageFormat.NV21);
                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int) frameSize.width) + "x" + Integer.valueOf((int) frameSize.height));
                    params.setPreviewSize((int) frameSize.width, (int) frameSize.height);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !Build.MODEL.equals("GT-I9100"))
                        params.setRecordingHint(true);

                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }

                    mCamera.setParameters(params);
                    params = mCamera.getParameters();
                    Log.v("--->>>>", "" + params.getPreviewSize().width);
                    mFrameWidth = params.getPreviewSize().width;
                    Log.v("--->>>>", "" + params.getPreviewSize().height);
                    mFrameHeight = params.getPreviewSize().height;
                    mFpsMeter = new FpsMeter();
                    mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                    mFpsMeter.measure();
                    int size = mFrameWidth * mFrameHeight;
                    size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                    mBuffer = new byte[size];

                    mCamera.addCallbackBuffer(mBuffer);
                    mCamera.setPreviewCallbackWithBuffer(this);

                    mFrameChain = new Mat[2];
                    mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);
                    mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);

                    AllocateCache();

                    mCameraFrame = new CameraFrameBean[2];
                    mCameraFrame[0] = new CameraFrameBean(mFrameChain[0], mFrameWidth, mFrameHeight);
                    mCameraFrame[1] = new CameraFrameBean(mFrameChain[1], mFrameWidth, mFrameHeight);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } else
                        mCamera.setPreviewDisplay(null);

                    /* Finally we are ready to start the preview */
                    Log.d(TAG, "startPreview");


                    mCamera.startPreview();
                } else
                    result = false;
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 释放相机
     */
    protected void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
            }
            mCamera = null;
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].release();
                mCameraFrame[1].release();
            }
        }
    }

    protected void AllocateCache() {
        mCacheBitmap = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888);
    }

    /**
     * This helper method can be called by subclasses to select camera preview size.
     * It goes over the list of the supported preview sizes and selects the maximum one which
     * fits both values set via setMaxFrameSize() and surface frame allocated for this view
     *
     * @param supportedSizes
     * @param surfaceWidth
     * @param surfaceHeight
     * @return optimal frame size
     */
    protected Size calculateCameraFrameSize(List<?> supportedSizes, CameraListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        int maxAllowedWidth = (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth) ? mMaxWidth : surfaceWidth;
        int maxAllowedHeight = (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight) ? mMaxHeight : surfaceHeight;

        for (Object size : supportedSizes) {
            int width = accessor.getWidth(size);
            int height = accessor.getHeight(size);

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = (int) width;
                    calcHeight = (int) height;
                }
            }
        }
        return new Size(calcWidth, calcHeight);
    }


    //底层相机回调帧画面

    @Override
    public void onPreviewFrame(byte[] frame, Camera camera) {
        //处理每帧画面
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
        synchronized (this) {
            mFrameChain[mChainIdx].put(0, 0, frame);
            mCameraFrameReady = true;
            this.notify();
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (CameraPreview.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            CameraPreview.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady) {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain[1 - mChainIdx].empty()) {

                        //deliverAndDrawFrame(mCameraFrame[1 - mChainIdx]);
                        //处理每帧
                        Mat modified;
                        CameraFrameBean frame = mCameraFrame[1 - mChainIdx];
                        modified = frame.rgba();
                        boolean bmpValid = true;
                        if (modified != null) {
                            try {
                                Utils.matToBitmap(modified, mCacheBitmap);
                            } catch (Exception e) {
                                Log.e(TAG, "Mat type: " + modified);
                                Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                                bmpValid = false;
                            }
                        }

                        if (bmpValid && mCacheBitmap != null) {
                            String fps = "";
                            if (mFpsMeter != null) {
                                 mFpsMeter.measure();
                            }
                           // if (frameImageListener != null) {
//                                //获取当前Texture
                                int[] renderTextures = new int[1];
                                GLES20.glGenTextures(1, renderTextures, 0);
                                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTextures[0]);
                                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mCacheBitmap, 0);
                                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//                                //Log.i("------->>>>>>","创建帧mCacheBitmap:"+frame.rgba());
//                                //放入队列当中
//                                DoubleBufferQueue.getInst().push(new FrameImageBean(mCacheBitmap,renderTextures[0], mCacheBitmap.getWidth(), mCacheBitmap.getHeight(), fps));
                                frameImageListener.onCameraFrame(frame, mCacheBitmap);
                          //  }
                        }
                    }

                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }

    public Camera getCamera() {
        return mCamera;
    }
}
