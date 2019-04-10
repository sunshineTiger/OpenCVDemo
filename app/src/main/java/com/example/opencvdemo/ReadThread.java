package com.example.opencvdemo;

import android.util.Log;

import java.util.List;

public class ReadThread implements Runnable {
    DoubleBufferQueue queue = DoubleBufferQueue.getInst();
    List<PreviewFrameBean> readList = queue.getReadList();
    int mFramesCouner;
    int STEP = 20;

    @Override
    public void run() {

        read2();
    }

    private void read2() {
        while (true) {
            queue.swap();
            readList = queue.getReadList();
            if (readList.size() != 0) {
                try {
                    Log.v("----->>>>>>", "消费者消费事件:" + readList.get(0).index + "    data:" + readList.get(0).data);
                    Thread.sleep(1000 * 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            readList.clear();
        }
    }

    private void read1() {
        while (true) {
            mFramesCouner++;
            while (readList.size() != 0) {
                if (mFramesCouner % STEP == 0) {
                    queue.swap();
                    readList = queue.getReadList();
                }
            }
            try {
                Log.v("----->>>>>>", "消费者消费事件:" + readList.get(0).index + "    data:" + readList.get(0).data);
                Thread.sleep(1000 * 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            readList.clear();
        }
    }
}
