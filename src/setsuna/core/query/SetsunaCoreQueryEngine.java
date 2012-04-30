package setsuna.core.query;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.sql.*;

import setsuna.core.AbstractCoreEngine;
import setsuna.core.util.*;



/**
 * Query用のSetsunaのコアエンジンクラス.<br>
 *
 * @author T.Okuyama
 */
public class SetsunaCoreQueryEngine extends AbstractCoreEngine {


    private CauseContainer cause = null;

    private ConditionContainer condition = null;

    private String checkAdapterName = null;

    private boolean stopFlg = false;



    public SetsunaCoreQueryEngine(CauseContainer cause, ConditionContainer condition, String checkAdapterName) {
        this.cause = cause;
        this.condition = condition;
        this.checkAdapterName = checkAdapterName;
    }


    public Object preEvent(Object param) throws Exception {
        String myName = super.getEngineName();

        EngineJoinQueueFolder.createaAapterOutputQueue(myName);
        EngineJoinQueueFolder.createaEventInputQueue(myName);
        AdapterJoinQueueMapper.addMappingInfomation(this.checkAdapterName, myName);
        return null;
    } 


    public Object doEvent(Object preEventResult) throws Exception {
        String myName = super.getEngineName();
        try {
            while (true) {

                Map adapterData = (Map)EngineJoinQueueFolder.pollAapterOutputQueue(myName, 50, TimeUnit.MILLISECONDS);


                if (adapterData == null && stopFlg == true) break;
                if (adapterData == null) continue;

                // Causeが設定されていない場合は全てQuery通過とする
                if (this.cause == null && this.condition == null) {
                    EngineJoinQueueFolder.putEventInputQueue(myName, adapterData);
                    continue;
                }

                // Causeのチェック
                if(this.cause == null || this.cause.checkAllCauseMatchValue(adapterData)) {

                    StreamDbUtil.r.lock();
                    try {
                        if (this.condition == null) {

                            // Conditionがないため、即Event実行
                            EngineJoinQueueFolder.putEventInputQueue(myName, adapterData); // TODO:ResultSetはいるのか？
                        } else {

                            // Query実行
                            if (this.condition.checkConditionMatchRecode(adapterData)) {

                                EngineJoinQueueFolder.putEventInputQueue(myName, adapterData); // TODO:ResultSetはいるのか？
                            }
                        }
                    } catch (Exception ee) {
                        throw ee;
                    } finally {
                        StreamDbUtil.r.unlock();
                    }
                }

                if (stopFlg == true) break;
            }
        } catch(Exception e) {
            throw e;
        }
        return null;
    } 


    public void endEvent(Object doEventResult) throws Exception {
        if (this.condition != null)
            this.condition.clear();

        this.cause = null;
        this.condition = null;
        return;
    }

    public void stopEvent() {
        this.stopFlg = true;
    }
}