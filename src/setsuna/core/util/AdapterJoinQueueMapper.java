package setsuna.core.util;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.*;



/**
 * AdapterとQueryを繋ぐデータ連携クラス.<br>
 *
 * @author T.Okuyama
 */
public class AdapterJoinQueueMapper {

    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock r = rwl.readLock();
    private static final Lock w = rwl.writeLock();


    private static Map mappingInfomationMap = null;

    static {
        mappingInfomationMap = new ConcurrentHashMap(1000, 800, 128);
    }

    public static void createMappingInfomation(String adapterName) {
        w.lock();
        try {
            if (!mappingInfomationMap.containsKey(adapterName)) {
                mappingInfomationMap.put(adapterName, new CopyOnWriteArrayList());
            }
        } finally {
            w.unlock();
        }
    }


    public static void addMappingInfomation(String adapterName, String queryName) {
        w.lock();
        try {
            if (!mappingInfomationMap.containsKey(adapterName)) {
                List queryNameList = new CopyOnWriteArrayList();
                queryNameList.add(queryName);
                mappingInfomationMap.put(adapterName, queryNameList);
            } else {
                executeRemoveMappingInfo(adapterName, queryName);
                List queryNameList = (List)mappingInfomationMap.get(adapterName);
                queryNameList.add(queryName);
            }
        } finally {
            w.unlock();
        }
    }


    public static void removeMappingInfomation(String adapterName, String queryName) {
        w.lock();
        try {
            executeRemoveMappingInfo(adapterName, queryName);
        } finally {
            w.unlock();
        }
    }


    public static void removeMappingAdapter(String adapterName, String queryName) {
        w.lock();
        try {
            mappingInfomationMap.remove(adapterName);
        } finally {
            w.unlock();
        }
    }

    private static void executeRemoveMappingInfo(String adapterName, String queryName) {
        if (mappingInfomationMap.containsKey(adapterName)) {

            List queryNameList = (List)mappingInfomationMap.get(adapterName);
            int removePoint = -1;
            for (int idx = 0; idx < queryNameList.size(); idx++) {
                String name = (String)queryNameList.get(idx);
                if (name.equals(queryName)) removePoint = idx;
            }

            if (removePoint != -1) queryNameList.remove(removePoint);
        }
    }

    public static void mappingAdapterData(String adapterName, Map data) {
        r.lock();
        try {
            if (mappingInfomationMap.containsKey(adapterName)) {

                List queryNameList = (List)mappingInfomationMap.get(adapterName);
                int removePoint = -1;
                for (int idx = 0; idx < queryNameList.size(); idx++) {
                    String name = (String)queryNameList.get(idx);
                    EngineJoinQueueFolder.putAapterOutputQueue(name, data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            r.unlock();
        }
    }
}