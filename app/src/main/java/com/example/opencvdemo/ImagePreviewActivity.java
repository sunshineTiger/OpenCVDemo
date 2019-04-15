package com.example.opencvdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.example.camerasdk.android.camera.CameraFrameBean;
import com.example.camerasdk.android.camera.CameraPreview;
import com.example.camerasdk.android.camera.CameraPreview2;
import com.example.camerasdk.interfaces.FrameImage;

import org.opencv.core.Mat;

public class ImagePreviewActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
        //System.loadLibrary("native-lib");
    }

    private ImageView previewImage;
    CameraPreview2 cameraPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewImage = findViewById(R.id.previewImage);
        //interfaceCallback();
        queueList();
    }

    private void interfaceCallback() {
        cameraPreview = new CameraPreview2(640, 480, 0, new FrameImage() {

            @Override
            public void onCameraFrame(CameraFrameBean inputFrame, final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        previewImage.setImageBitmap(bitmap);  //预览
                    }
                });

            }

            @Override
            public void onCameraFramebitmap(final Mat rgba, final Mat gray, final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.v("----------->>>>>>>>>", "rgba size:" + rgba.size());
                        Log.v("----------->>>>>>>>>", "gray size:" + gray.size());
                        previewImage.setImageBitmap(bitmap);  //预览
                    }
                });
            }
        });
    }

    private void queueList() {
        cameraPreview = new CameraPreview2(640, 480, 0);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final Bitmap bimtap = (Bitmap) cameraPreview.getQueue().poll();
                    if (null != bimtap) {
                        Log.v(">----------->>>>>>>>>", cameraPreview.getQueue().hashCode() + cameraPreview.getQueue().size() + "  cameraPreview.bimtap()" + bimtap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                previewImage.setImageBitmap(bimtap);
                            }
                        });
                    }
                }

            }
        });
        thread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraPreview.destoryCamera();
    }


}
