package com.example.opencvdemo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class CameraActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        Camera open = Camera.open();    //初始化 Camera对象
        MyCameraPreview mPreview = new MyCameraPreview(this, open);
        FrameLayout camera_preview = (FrameLayout) findViewById(R.id.camera_preview);

        camera_preview.addView(mPreview);
        ReadThread reader = new ReadThread();
        Thread t1 = new Thread(reader);
        t1.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
