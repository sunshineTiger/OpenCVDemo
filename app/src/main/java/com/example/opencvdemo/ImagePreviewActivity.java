package com.example.opencvdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.example.camerasdk.DoubleBufferQueue;
import com.example.camerasdk.FrameImageBean;
import com.example.camerasdk.android.camera.CameraFrameBean;
import com.example.camerasdk.android.camera.CameraPreview;
import com.example.camerasdk.interfaces.FrameImage;

import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("opencv_java");
    }

    private ImageView previewImage;
    CameraPreview cameraPreview;
    private DoubleBufferQueue queue = DoubleBufferQueue.getInst();
    private List<FrameImageBean> readList = queue.getReadList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewImage = findViewById(R.id.previewImage);
        cameraPreview = new CameraPreview(640, 480, 0, new FrameImage() {

            @Override
            public void onCameraFrame(CameraFrameBean inputFrame, final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        previewImage.setImageBitmap(bitmap);  //预览
                    }
                });

            }
        });
        //cameraPreview = new CameraPreview(640, 480, 0);
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            queue.swap();
//                            readList = queue.getReadList();
//                            if (readList.size() != 0) {
//                                previewImage.setImageBitmap(readList.get(0).getBitmap());  //预览
//                                readList.clear();
//                            }
//                        }
//                    });
//
//
//                }
//
//            }
//        });
//        thread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraPreview.destoryCamera();
    }
}
