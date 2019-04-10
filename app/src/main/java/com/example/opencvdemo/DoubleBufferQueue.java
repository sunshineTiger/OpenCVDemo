package com.example.opencvdemo;

import java.util.ArrayList;
import java.util.List;

public class DoubleBufferQueue {
    private List<PreviewFrameBean> readList = new ArrayList<PreviewFrameBean>();
    private List<PreviewFrameBean> writeList = new ArrayList<PreviewFrameBean>();
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
    public void push(PreviewFrameBean value) {
        synchronized (writeList) {
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
    public List<PreviewFrameBean> getReadList() {
        return readList;
    }

    /**
     * 交换
     */
    public void swap() {
        synchronized (writeList) {
            if (null != writeList && writeList.size() != 0) {
                List<PreviewFrameBean> temp = readList;
                readList = writeList;
                writeList = temp;
                writeList.clear();
            }

        }
    }
}
