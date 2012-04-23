package setsuna.core;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

import setsuna.core.util.*;
import setsuna.core.event.*;
import setsuna.core.query.*;
import setsuna.core.adapter.*;

/**
 * Setsuna本体.<br>
 *
 * @author T.Okuyama
 */
public class SetsunaCore {

    private Map executeAdapterEngineMap = new ConcurrentHashMap(100, 80, 64);

    private Map executeQueryEngineMap = new ConcurrentHashMap(100, 80, 64);

    private Map executeEventEngineMap = new ConcurrentHashMap(100, 80, 64);

    private String setsunaServerInfomation = null;


    public SetsunaCore() {
    }


    /**
     * Adapterの実行を行う.<br>
     * クラスの名前指定による動的ロードにて行う<br>
     *
     * @param adapterClassPath 実行するAdapterクラス名
     * @return AbstractCoreEngine 実行済みAdapterエンジンのインスタンス
     * @throws Exception
     */
    public AbstractCoreEngine executeAdapterEngine(String adapterClassPath) throws Exception {
        try {
            IAdapter adapter = (IAdapter)Class.forName(adapterClassPath).newInstance();
            return executeAdapterEngine(adapter);

        } catch (ClassNotFoundException ce) {
            throw new SetsunaException(adapterClassPath + ":Class not found", ce);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Adapterの実行を行う.<br>
     * Adapterのインスタンスを渡すことで実行を委譲する<br>
     *
     * @param adapter 実行するAdapterクラインスタンス
     * @return AbstractCoreEngine 実行済みAdapterエンジンのインスタンス
     * @throws Exception
     */
    public AbstractCoreEngine executeAdapterEngine(IAdapter adapter) throws Exception {
        AbstractCoreEngine setsunaCoreAdapterEngine = null;
        try {

            String engineName = adapter.getName() + "_Engine";

            if (this.executeAdapterEngineMap.containsKey(engineName)) {

                setsunaCoreAdapterEngine = (AbstractCoreEngine)this.executeAdapterEngineMap.get(engineName);
                setsunaCoreAdapterEngine.stopEvent();
                setsunaCoreAdapterEngine.join();
                setsunaCoreAdapterEngine = null;

                StreamDbUtil.w.lock();
                try {

                    if(!StreamDbUtil.dropTable(adapter.getName())) throw new SetsunaException("Failed in a change of Adapter 1.");

                    if (!StreamDbUtil.createTable(adapter.getName(), adapter.getDataColumnNames(), true)) throw new SetsunaException("Failed in a change of Adapter 2.");
                    setsunaCoreAdapterEngine = new SetsunaCoreAdapterEngine(adapter);
                    setsunaCoreAdapterEngine.setEngineName(engineName);
                    setsunaCoreAdapterEngine.start();
                    this.executeAdapterEngineMap.put(engineName, setsunaCoreAdapterEngine);

                } catch (Exception ee) {
                    throw ee;
                } finally {
                    StreamDbUtil.w.unlock();
                }
            } else {

                if (!StreamDbUtil.createTable(adapter.getName(), adapter.getDataColumnNames(), false)) throw new SetsunaException("Failed in a change of Adapter 3.");

                setsunaCoreAdapterEngine = new SetsunaCoreAdapterEngine(adapter);
                setsunaCoreAdapterEngine.setEngineName(engineName);
                setsunaCoreAdapterEngine.start();
                this.executeAdapterEngineMap.put(engineName, setsunaCoreAdapterEngine);
            }
        } catch (Exception e) {
            throw e;
        }
        return setsunaCoreAdapterEngine;
    }


    public AbstractCoreEngine executeQueryEngine(String queryName, String checkAdapterName, CauseContainer causeContainer, ConditionContainer conditionContainer) throws Exception {
        AbstractCoreEngine setsunaCoreQueryEngine = null;
        try {


            String engineName = queryName + "_Engine";

            if (this.executeQueryEngineMap.containsKey(engineName)) {

                setsunaCoreQueryEngine = (AbstractCoreEngine)this.executeQueryEngineMap.get(engineName);
                setsunaCoreQueryEngine.stopEvent();
                setsunaCoreQueryEngine.join(60000);
                setsunaCoreQueryEngine = null;

                conditionContainer.buildStreamDatabaseStatement(StreamDatabaseConnectManager.getConnection(false));
                setsunaCoreQueryEngine = new SetsunaCoreQueryEngine(causeContainer, conditionContainer, checkAdapterName);
                setsunaCoreQueryEngine.setEngineName(queryName);

                setsunaCoreQueryEngine.start();
                this.executeQueryEngineMap.put(engineName, setsunaCoreQueryEngine);
            } else {

                if (conditionContainer != null) {
                    conditionContainer.buildStreamDatabaseStatement(StreamDatabaseConnectManager.getConnection(false));
                }
                setsunaCoreQueryEngine = new SetsunaCoreQueryEngine(causeContainer, conditionContainer, checkAdapterName);
                setsunaCoreQueryEngine.setEngineName(queryName);

                setsunaCoreQueryEngine.start();
                this.executeQueryEngineMap.put(engineName, setsunaCoreQueryEngine);
            }
        } catch (Exception e) {
            throw e;
        }
        return setsunaCoreQueryEngine;
    }

    // イベントと処理対象のフッククエリの名前を指定
    public AbstractCoreEngine executeEventEngine(EventContainer eventContainer, String queryName) throws Exception {
        AbstractCoreEngine setsunaCoreEventEngine = null;
        try {

            String engineName = eventContainer.getEventName() + "_Engine";

            if (this.executeEventEngineMap.containsKey(engineName)) {

                setsunaCoreEventEngine = (AbstractCoreEngine)this.executeEventEngineMap.get(engineName);
                setsunaCoreEventEngine.stopEvent();
                setsunaCoreEventEngine.join(60000);
                setsunaCoreEventEngine = null;

                setsunaCoreEventEngine = new SetsunaCoreEventEngine(eventContainer, queryName);
                setsunaCoreEventEngine.setEngineName(engineName);
                setsunaCoreEventEngine.start();
                this.executeEventEngineMap.put(engineName, setsunaCoreEventEngine);
            } else {

                setsunaCoreEventEngine = new SetsunaCoreEventEngine(eventContainer, queryName);
                setsunaCoreEventEngine.setEngineName(engineName);
                setsunaCoreEventEngine.start();
                this.executeEventEngineMap.put(engineName, setsunaCoreEventEngine);
            }
        } catch (Exception e) {
            throw e;
        }
        return setsunaCoreEventEngine;
    }

}