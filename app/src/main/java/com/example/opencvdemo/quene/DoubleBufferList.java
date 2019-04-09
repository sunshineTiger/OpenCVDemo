package com.example.opencvdemo.quene;

import java.util.List;

public class DoubleBufferList {

    private List<Object> lP;
    private List<Object> lT;
    private int gap;

    /**
     * 构造方法
     *
     * @param lP  用来存放对象的队列
     * @param lT  用来取对象的队列
     * @param gap 交换的间隔
     */
    public DoubleBufferList(List lP, List lT, int gap) {
        this.lP = lP;
        this.lT = lT;
        this.gap = gap;
    }


    public void check() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (lT.size() == 0) {
                        synchronized (lT) {
                            synchronized (lP) {
                                lT.addAll(lP);
                            }
                            lP.clear();
                        }
                    }
                    try {
                        Thread.sleep(gap);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

}
