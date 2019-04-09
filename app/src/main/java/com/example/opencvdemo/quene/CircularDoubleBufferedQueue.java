package com.example.opencvdemo.quene;

import android.util.Log;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class CircularDoubleBufferedQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private Logger logger = Logger.getLogger(CircularDoubleBufferedQueue.class.getName());

    /**
     * The queued items
     */
    private E[] itemsA;
    private E[] itemsB;

    private ReentrantLock readLock, writeLock;
    private Condition notEmpty;
    private Condition notFull;
    private Condition awake;


    private E[] writeArray, readArray;
    private volatile int writeCount, readCount;
    private int writeArrayHP, writeArrayTP, readArrayHP, readArrayTP;

    public CircularDoubleBufferedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue initial capacity can't less than 0!");
        }
        itemsA = (E[]) new Object[capacity];
        itemsB = (E[]) new Object[capacity];
        readLock = new ReentrantLock();
        writeLock = new ReentrantLock();
        notEmpty = readLock.newCondition();
        notFull = writeLock.newCondition();
        awake = writeLock.newCondition();

        readArray = itemsA;
        writeArray = itemsB;
    }

    private void insert(E e) {
        writeArray[writeArrayTP] = e;
        ++writeArrayTP;
        ++writeCount;
    }

    private E extract() {
        E e = readArray[readArrayHP];
        readArray[readArrayHP] = null;
        ++readArrayHP;
        --readCount;
        return e;
    }


    /**
     * switch condition:
     * read queue is empty && write queue is not empty
     * <p>
     * Notice:This function can only be invoked after readLock is
     * grabbed,or may cause dead lock
     *
     * @param timeout
     * @param isInfinite: whether need to wait forever until some other
     *                    thread awake it
     * @return
     * @throws InterruptedException
     */
    private long queueSwitch(long timeout, boolean isInfinite) throws InterruptedException {
        writeLock.lock();
        try {
            if (writeCount <= 0) {
                Log.v("-->>", "Write Count:" + writeCount + ", Write Queue is empty, do not switch!");
                try {
                    Log.v("-->>", "Queue is empty, need wait....");
                    if (isInfinite && timeout <= 0) {
                        awake.await();
                        return -1;
                    } else {
                        return awake.awaitNanos(timeout);
                    }
                } catch (InterruptedException ie) {
                    awake.signal();
                    throw ie;
                }
            } else {
                E[] tmpArray = readArray;
                readArray = writeArray;
                writeArray = tmpArray;

                readCount = writeCount;
                readArrayHP = 0;
                readArrayTP = writeArrayTP;

                writeCount = 0;
                writeArrayHP = readArrayHP;
                writeArrayTP = 0;

                notFull.signal();
                Log.v("-->>", "Queue switch successfully!");
                return -1;
            }
        } finally {
            writeLock.unlock();
        }
    }


    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void put(E e) throws InterruptedException {

    }

    /**
     * 添加
     * @param e
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }

        long nanoTime = unit.toNanos(timeout);
        writeLock.lockInterruptibly();
        try {
            for (; ; ) {
                if (writeCount < writeArray.length) {
                    insert(e);
                    if (writeCount == 1) {
                        awake.signal();
                    }
                    return true;
                }

                //Time out
                if (nanoTime <= 0) {
                    Log.v("-->>", "offer wait time out!");
                    return false;
                }
                //keep waiting
                try {
                    Log.v("-->>", "Queue is full, need wait....");
                    nanoTime = notFull.awaitNanos(nanoTime);
                } catch (InterruptedException ie) {
                    notFull.signal();
                    throw ie;
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        return null;
    }

    /**
     * 移除
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanoTime = unit.toNanos(timeout);
        readLock.lockInterruptibly();

        try {
            for (; ; ) {
                if (readCount > 0) {
                    return extract();
                }

                if (nanoTime <= 0) {
                    Log.v("-->>", "poll time out!");
                    return null;
                }
                nanoTime = queueSwitch(nanoTime, false);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }
}
