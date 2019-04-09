package com.example.opencvdemo.quene;

import java.util.ArrayList;
import java.util.List;

public class DoubleBufferedQueue {

    private List<String> readList = new ArrayList<String>();
    private List<String> writeList = new ArrayList<String>();
    private static DoubleBufferedQueue queue = new DoubleBufferedQueue();

    private DoubleBufferedQueue() {

    }

    public static DoubleBufferedQueue getInst() {
        return queue;
    }

    public void push(String value) {
        synchronized (writeList) {
            writeList.add(value);
        }
    }

    public int getWriteListSize() {
        synchronized (writeList) {
            return writeList.size();
        }
    }

    public List<String> getReadList() {
        return readList;
    }

    public void swap() {
        synchronized (writeList) {
            List<String> tString = readList;
            readList = writeList;
            writeList = tString;

            writeList.clear();
        }
    }

}
