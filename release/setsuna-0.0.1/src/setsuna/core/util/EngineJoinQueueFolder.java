package setsuna.core.util;

import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.locks.*;

import setsuna.core.event.*;
import setsuna.core.query.*;
import setsuna.core.adapter.*;


/**
 * 全てのCoreEngine間を接続するデータキュー管理クラス<br>
 *
 * @author T.Okuyama
 */
public class EngineJoinQueueFolder {

    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock r = rwl.readLock();
    private static final Lock w = rwl.writeLock();

    private static ConcurrentHashMap adapterOutputQueueMap = new ConcurrentHashMap(1000,900, 32);
    private static ConcurrentHashMap eventInputQueueMap = new ConcurrentHashMap(1000,900, 32);



    public static void createaAapterOutputQueue(String name) {
        w.lock();
        try {
            if (!adapterOutputQueueMap.containsKey(name)) {
                adapterOutputQueueMap.put(name, new ArrayBlockingQueue(100000));
            }
        } finally {
            w.unlock();
        }
    }

    public static void removeAapterOutputQueue(String name) {
        w.lock();
        try {
            if (!adapterOutputQueueMap.containsKey(name)) {
                adapterOutputQueueMap.remove(name);
            }
        } finally {
            w.unlock();
        }
    }


    public static void putAapterOutputQueue(String name, Object value) throws InterruptedException {
        r.lock();
        try {
            if (adapterOutputQueueMap.containsKey(name))
                ((ArrayBlockingQueue)adapterOutputQueueMap.get(name)).put(value);
        } finally {
            r.unlock();
        }
    }

    public static void offerAapterOutputQueue(String name, Object value) throws InterruptedException {
        r.lock();
        try {
            if (adapterOutputQueueMap.containsKey(name))
                ((ArrayBlockingQueue)adapterOutputQueueMap.get(name)).offer(value);
        } finally {
            r.unlock();
        }
    }

    public static void offerAapterOutputQueue(String name, Object value, long timeout, TimeUnit unit) throws InterruptedException {
        r.lock();
        try {
            if (adapterOutputQueueMap.containsKey(name))
                ((ArrayBlockingQueue)adapterOutputQueueMap.get(name)).offer(value, timeout, unit);
        } finally {
            r.unlock();
        }
    }


    public static Object pollAapterOutputQueue(String name) throws InterruptedException {
        r.lock();
        try {
            if (adapterOutputQueueMap.containsKey(name))
                return ((ArrayBlockingQueue)adapterOutputQueueMap.get(name)).poll();
        } finally {
            r.unlock();
        }
        return null;
    }

    public static Object pollAapterOutputQueue(String name, long timeout, TimeUnit unit) throws InterruptedException {
        r.lock();
        try {
            if (adapterOutputQueueMap.containsKey(name))
                return ((ArrayBlockingQueue)adapterOutputQueueMap.get(name)).poll(timeout, unit);
        } finally {
            r.unlock();
        }
        return null;
    }

    public static Object takeAapterOutputQueue(String name) throws InterruptedException {
        r.lock();
        try {
            if (adapterOutputQueueMap.containsKey(name))
                return ((ArrayBlockingQueue)adapterOutputQueueMap.get(name)).take();
        } finally {
            r.unlock();
        }
        return null;
    }





    public static void createaEventInputQueue(String name) {
        w.lock();
        try {
            if (!eventInputQueueMap.containsKey(name)) {
                eventInputQueueMap.put(name, new ArrayBlockingQueue(100000));
            }
        } finally {
            w.unlock();
        }
    }

    public static void removeEventInputQueue(String name) {
        w.lock();
        try {
            if (!eventInputQueueMap.containsKey(name)) {
                eventInputQueueMap.remove(name);
            }
        } finally {
            w.unlock();
        }
    }


    // Put
    public static void putEventInputQueue(String name, Object value) throws InterruptedException {
        r.lock();
        try {
            if (eventInputQueueMap.containsKey(name))
                ((ArrayBlockingQueue)eventInputQueueMap.get(name)).put(value);
        } finally {
            r.unlock();
        }
    }

    // Put
    public static void offerEventInputQueue(String name, Object value) throws InterruptedException {
        r.lock();
        try {
            if (eventInputQueueMap.containsKey(name))
                ((ArrayBlockingQueue)eventInputQueueMap.get(name)).offer(value);
        } finally {
            r.unlock();
        }
    }

    // Put
    public static void offerEventInputQueue(String name, Object value, long timeout, TimeUnit unit) throws InterruptedException {
        r.lock();
        try {
            if (eventInputQueueMap.containsKey(name))
                ((ArrayBlockingQueue)eventInputQueueMap.get(name)).offer(value, timeout, unit);
        } finally {
            r.unlock();
        }
    }


    // Get
    public static Object pollEventInputQueue(String name) throws InterruptedException {
        r.lock();
        try {
            if (eventInputQueueMap.containsKey(name))
                return ((ArrayBlockingQueue)eventInputQueueMap.get(name)).poll();
        } finally {
            r.unlock();
        }
        return null;
    }

    // Get
    public static Object pollEventInputQueue(String name, long timeout, TimeUnit unit) throws InterruptedException {
        r.lock();
        try {
            if (eventInputQueueMap.containsKey(name))
                return ((ArrayBlockingQueue)eventInputQueueMap.get(name)).poll(timeout, unit);
        } finally {
            r.unlock();
        }
        return null;
    }

    // Get
    public static Object takeEventInputQueue(String name) throws InterruptedException {
        r.lock();
        try {
            if (eventInputQueueMap.containsKey(name))
                return ((ArrayBlockingQueue)eventInputQueueMap.get(name)).take();
        } finally {
            r.unlock();
        }
        return null;
    }
}