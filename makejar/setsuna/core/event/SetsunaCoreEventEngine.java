package setsuna.core.event;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import setsuna.core.AbstractCoreEngine;
import setsuna.core.util.*;


/**
 * Event用のSetsunaのコアエンジンクラス.<br>
 *
 * @author T.Okuyama
 */
public class SetsunaCoreEventEngine extends AbstractCoreEngine {


    protected EventContainer eventContainer = null;

    protected String queryName = null;

    private boolean stopFlg = false;

    // 実行したいEventクラスと、このイベントを発火するクエリクラスの名前を指定
    public SetsunaCoreEventEngine(EventContainer eventContainer, String bindQueryName) {
        this.eventContainer = eventContainer;
        this.queryName = bindQueryName;
    }


    public Object preEvent(Object param) throws Exception {

        return null;
    } 


    public Object doEvent(Object preEventResult) throws Exception {
        try {
            while (true) {

                Map matchData = (Map)EngineJoinQueueFolder.pollEventInputQueue(queryName, 50, TimeUnit.MILLISECONDS);
                if (this.stopFlg == true && matchData == null) break;
                if (matchData == null) continue;

                this.eventContainer.executeEvent(matchData);

                if (this.stopFlg == true) break;
            }
        } catch(Exception e) {
            throw e;
        }
        return null;
    } 


    public void endEvent(Object doEventResult) throws Exception {
        this.eventContainer = null;
        return;
    }


    public void stopEvent() {
        this.stopFlg = true;
    }

}