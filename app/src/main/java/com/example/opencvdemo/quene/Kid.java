package com.example.opencvdemo.quene;

import android.util.Log;

public class Kid extends Thread {
    long time1 = System.currentTimeMillis();
    int count = 0;
    boolean flag = true;

    @Override
    public void run() {
        while (flag) {
            synchronized (Tools.lT) {
                if (Tools.lT.size() != 0) {
                    Tools.lT.remove(0);
                }
            }
            count++;
            Log.v("--->>>>>>", "用时间: "+count);
            if (count == 9000000) {
                flag = false;
                Log.v("--->>>>>>", "用时间: " + (System.currentTimeMillis() - time1));
            }
        }
    }
}
