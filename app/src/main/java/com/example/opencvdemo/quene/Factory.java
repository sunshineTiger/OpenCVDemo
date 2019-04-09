package com.example.opencvdemo.quene;

public class Factory extends Thread {

    private int index;

    @Override
    public void run() {
        while (true) {
            Toy t = new Toy();
            t.setName("玩具" + index);
            ++index;
            synchronized (Tools.lT){
                Tools.lT.add(t);
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
