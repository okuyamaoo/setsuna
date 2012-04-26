package setsuna.core.adapter;

import java.sql.*;
import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import setsuna.core.AbstractCoreEngine;
import setsuna.core.util.*;


/**
 * Adapterクラスからデータを取り出して、QueryEngineが監視する<br>
 * Queueにデータを投入する(複数Queue(QueryEngine)が存在する可能性があるので、<br>
 * AdapterJoinQueueMapper.mappingAdapterDataに移譲する)。その後、<br>
 * DBにデータをインサートする。<br>
 * 上記の処理を繰り返す<br>
 *
 * @author T.Okuyama
 */
public class SetsunaCoreAdapterEngine extends AbstractCoreEngine {


    protected IAdapter userAdapter = null;

    protected String[] dataColumnNames = null;

    private DataCleanWorker cleanWorker = null;

    protected boolean stopFlg = false;



    public SetsunaCoreAdapterEngine(IAdapter userAdapter) {
        this.userAdapter = userAdapter;
        AdapterJoinQueueMapper.createMappingInfomation(userAdapter.getName());
    }


    public Object preEvent(Object param) throws Exception {

        this.cleanWorker = new DataCleanWorker(userAdapter.getName(), userAdapter.getArrivalTime(), this);
        try {
            this.cleanWorker.start();
        } catch (Exception e) {
            throw e;
        }
        return null;
    } 


    public Object doEvent(Object preEventResult) throws Exception {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        long dataIndex = 0L;

        try {
            conn = StreamDatabaseConnectManager.getConnection(true);
        
            long readCount = 0L;
            boolean skipMode = false;
            if (SetsunaStaticConfig.DATA_INPUT_OFFSET > 0) skipMode = true;

            while (true) {

                Map readData = userAdapter.read();

                if (readData == null) break;

                // Skip設定
                if (skipMode == true) {
                    readCount++;
                    if (readCount < SetsunaStaticConfig.DATA_INPUT_OFFSET) {
                        continue;
                    } else {
                        skipMode = false;
                    }
                }

                // データの項目とカラム定義の整合性チェック
                if (!this.checkDataColumn(readData, this.userAdapter.getDataColumnNames())) {
                    if (!SetsunaStaticConfig.ERROR_DATA_SKIP)
                        throw new SetsunaException("The read data differs from definition information. Adapter name is " + userAdapter.getName());
                } else {

                    AdapterJoinQueueMapper.mappingAdapterData(userAdapter.getName(), readData);
                    if (preparedStatement == null) preparedStatement = StreamDbUtil.createInsertPrepareStatement(conn, userAdapter.getName(), userAdapter.getDataColumnNames());
                    StreamDbUtil.insertStreamData(preparedStatement, userAdapter.getDataColumnNames(), readData, dataIndex);
                    dataIndex++;
                }
                if(this.stopFlg) break;
            }
        } catch(Exception e) {
            throw e;
        }
        return null;
    } 


    public void endEvent(Object doEventResult) throws Exception {
        userAdapter = null;
        return;
    }

    public void stopEvent() {
        this.stopFlg = true;
        userAdapter.stop();
    }



    private boolean checkDataColumn(Map readData, String[] defColumnList) {
        for (int idx = 0; idx < defColumnList.length; idx++) {
            if (!readData.containsKey(defColumnList[idx])) return false;
        }
        return true;
    }


    /**
     * 一時DBから有効期限切れのデータを削除するスレッド
     *
     */
    class DataCleanWorker extends Thread {

        private String tableName ="";

        private SetsunaCoreAdapterEngine parentEngine = null;

        private long arrivalTime = 0L;


        public DataCleanWorker(String tableName, long arrivalTime, SetsunaCoreAdapterEngine parentEngine) {
            this.tableName = tableName;
            this.arrivalTime = arrivalTime;
            this.parentEngine = parentEngine;
        }

        public void run() {

            StringBuilder queryBuf = new StringBuilder(100);

            queryBuf.append("DELETE FROM ");
            queryBuf.append(this.tableName);
            queryBuf.append(" WHERE C_TIME < ?");
            String deleteQuery = queryBuf.toString();

            while (true) {
                if (this.parentEngine.stopFlg == true) break;
                try {
                    long deleteTime = System.currentTimeMillis();
                    deleteTime = deleteTime - arrivalTime;
                    StreamDbUtil.deleteStreamData(deleteQuery, deleteTime);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
}