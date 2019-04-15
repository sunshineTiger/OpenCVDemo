package com.example.camerasdk.android.camera;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.example.camerasdk.interfaces.CameraListItemAccessor;
import com.example.camerasdk.interfaces.FrameImage;

import org.opencv.BuildConfig;
import org.opencv.android.FpsMeter;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraPreview2 implements Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";
    private static final int MAGIC_TEXTURE_ID = 10;//默认TEXTURE_ID
    private byte mBuffer[];
    protected FpsMeter mFpsMeter = null;
    protected Camera mCamera;
    private SurfaceTexture mSurfaceTexture;
    private int cameraId;
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
    private Mat frameMat;
    private Mat mRgba;
    private Bitmap mCacheBitmap;
    private static final int MAX_UNSPECIFIED = -1;
    private FrameImage frameImageListener;
    ExecutorService fixedThreadPool;
    final long awaitTime = 5 * 1000;

    private BlockingQueue<Bitmap> queueList = new ArrayBlockingQueue(1024);
    private int MaxThradSize = 3;

    public CameraPreview2(int width, int height, int cameraId) {
        this.cameraId = cameraId;
        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;
        connectCamera(width, height);
        fixedThreadPool = Executors.newFixedThreadPool(MaxThradSize);
    }

    public CameraPreview2(int width, int height, int cameraId, FrameImage frameImageListener) {
        this.cameraId = cameraId;
        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;
        this.frameImageListener = frameImageListener;
        connectCamera(width, height);
        fixedThreadPool = Executors.newFixedThreadPool(MaxThradSize);
    }

    public BlockingQueue getQueue() {
        return queueList;
    }

    private boolean connectCamera(int width, int height) {
        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height))
            return false;
        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        return true;
    }

    public void destoryCamera() {
        disconnectCamera();
    }

    private void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        /* Now release camera */
        releaseCamera();

        try {
            if (fixedThreadPool == null)
                return;
            fixedThreadPool.shutdown();
            if (fixedThreadPool.awaitTermination(awaitTime, TimeUnit.MILLISECONDS)) {
                fixedThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            fixedThreadPool.shutdownNow();
        } finally {
            fixedThreadPool = null;
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

                    frameMat = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);
                    mRgba = new Mat();
                    AllocateCache();//创建位图


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
            if (null != frameImageListener) {
                frameImageListener = null;
            }
            mCamera = null;
            if (mCacheBitmap != null) {
                mCacheBitmap.recycle();
            }
            if (null != frameMat) {
                frameMat.release();
            }
            if (null != mRgba) {
                mRgba.release();
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
        //Log.v("xxxxxxxxxxxxxxxxxx", "处理每帧画面");
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
        if (fixedThreadPool != null) {
            fixedThreadPool.execute(new CameraWorker(frame));
        }else{
            Log.v("xxxxxxxxxxxxxxxxxx", "fixedThreadPool is null");
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }


    private class CameraWorker implements Runnable {
        private byte[] frameData;

        public CameraWorker(byte[] frame) {
            this.frameData = frame;
        }

        @Override
        public void run() {
            frameMat.put(0, 0, frameData);
            Mat gray = frameMat.submat(0, mFrameHeight, 0, mFrameWidth);
            Imgproc.cvtColor(frameMat, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            boolean bmpValid = true;
            if (mRgba != null) {
                try {
                    Utils.matToBitmap(mRgba, mCacheBitmap);
                } catch (Exception e) {
                    Log.e(TAG, "Mat type: " + mRgba);
                    Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                    Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                    bmpValid = false;
                }
                if (bmpValid && mCacheBitmap != null) {
                    if (mFpsMeter != null)
                        mFpsMeter.measure();
//                    if (null != frameImageListener) {
//                        frameImageListener.onCameraFramebitmap(mRgba, gray, mCacheBitmap);
//                    } else {

                    if (queueList.offer(mCacheBitmap)) {
                        //向队列添加数据成功
                        Log.v("xxxxxxxxxxxxxxxxxx", "向队列大小:"+queueList.size());
                        Log.v("xxxxxxxxxxxxxxxxxx", "向队列添加数据成功:");
                    } else {
                        //向队列添加数据成功
                        Log.v("xxxxxxxxxxxxxxxxxx", "向队列添加数据失败");
                    }
//                    }


                }
                Log.d(TAG, "Finish processing thread");
            }
        }
    }

    public Camera getCamera() {
        return mCamera;
    }
}
