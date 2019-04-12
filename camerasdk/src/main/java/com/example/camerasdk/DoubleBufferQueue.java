package com.example.camerasdk;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DoubleBufferQueue {
    private List<FrameImageBean> readList = new ArrayList<FrameImageBean>();
    private List<FrameImageBean> writeList = new ArrayList<FrameImageBean>();
    private static DoubleBufferQueue queue = new DoubleBufferQueue();

    private DoubleBufferQueue() {

    }

    public static DoubleBufferQueue getInst() {
        return queue;
    }

    /**
     * 添加
     *
     * @param value
     */
    public void push(FrameImageBean value) {
        synchronized (writeList) {
           // Log.i("------->>>>>>","添加新帧图片");
            writeList.add(0, value);
        }
    }

    /**
     * 获取大小
     *
     * @return
     */
    public int getWriteListSize() {
        synchronized (writeList) {
            return writeList.size();
        }
    }

    /**
     * 获取队列
     *
     * @return
     */
    public List<FrameImageBean> getReadList() {
        return readList;
    }

    /**
     * 交换
     */
    public void swap() {
        synchronized (writeList) {
            if (null != writeList && writeList.size() != 0) {
               // Log.i("------->>>>>>","交换队列");
                List<FrameImageBean> temp = readList;
                readList = writeList;
                writeList = temp;
                writeList.clear();
            }

        }
    }
}
