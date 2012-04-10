package setsuna.core.adapter;


import java.util.*;
import java.io.*;
import java.util.concurrent.*;

import org.msgpack.rpc.Server;
import org.msgpack.rpc.loop.EventLoop;

import setsuna.core.util.*;

/**
 * SetsunaMainを利用して立ち上げる場合の入力用のAdapter.<br>
 * Server型.<br>
 * 内部ではMessagePack-RPCを利用してサーバを立ち上げている.<br>
 * RPC定義としては単純にStringのカラム数分の要素を持つ配列を引数にとるメソッドがあるのみ.<br>
 *
 * @author T.Okuyama
 */
public class DefaultEasyServerAdapter extends AbstractDefaultAdapter implements IAdapter  {

    private String[] columnList = null;
    private long arrivalTime = 0L;

    private ArrayBlockingQueue nextDataQueue = null;
    private Object nextDataSync = new Object();
    private boolean firstAccess = true;

    private EventLoop loop = null;
    private Server server = null;

    private InnerServer innerServer = null;

    private boolean stopFlg = false;


    public DefaultEasyServerAdapter(String bindAddress, int bindPort, String[] columnList, long arrivalTime) throws Exception {
        this.columnList = columnList;
        this.nextDataQueue = new ArrayBlockingQueue(1000000);
        this.arrivalTime = arrivalTime;

        this.loop = EventLoop.defaultEventLoop();
        this.server = new Server();
        server.serve(this);
        if (bindAddress == null) {
            server.listen(bindPort);
        } else {
            server.listen(bindAddress, bindPort);
        }
        
        this.innerServer = new InnerServer(this.server, this.loop);
        this.innerServer.start();
        while(this.columnList == null) {
            Thread.sleep(10);
        }
    }



    /**
     * MessagePack-RPCによって呼ばれるRPC用メソッド.<br>
     * 内部ではQueueにデータをいれているだけ<br>
     *
     * @param data クライアントから渡されたデータ配列(カラム数分の想定。違っている場合は-9を返す)
     * @return int 正常終了 0, カラム定義と合わない -9, 内部エラー -1
     */
    public int next(String[] data) {
        if (this.firstAccess)  {
            synchronized (nextDataSync) {
                if (this.firstAccess && this.columnList == null) {

                    // カラム定義が初期化されていなければ作る
                    this.makeColumn(data);
                }
                this.firstAccess = false;
            }
        }

        try {

            if (data.length != this.columnList.length) return -9;
            this.nextDataQueue.put(data);
        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    // カラム定義を作る
    private void makeColumn(String[] data) {
        this.columnList = new String[data.length];

        for (int idx = 0; idx < data.length; idx++) {
            this.columnList[idx] = "COLUMN" + idx;
        }
    }

    /**
     * Adapterの名前を返す.<br>
     * Adapterデータを格納するDB上のテーブル名としても利用される<br>
     *
     * @return String Adapter名(テーブル名)
     */
    public String getName() {
        return SetsunaStaticConfig.DEFAULT_EASY_SERVER_TABLE_NAME;
    }

    /**
     * このアダプターから取得したデータが有効な時間をミリ秒で返却する.<br>
     * 例えば10分であれば1000ミリ秒×60×10=600000となるので、
     * 返却値は600000となる。
     *
     * @return long 有効期限
     */
    public long getArrivalTime() {
        return this.arrivalTime;
    }


    /**
     * Adapterの返すデータのカラム情報を返す.<br>
     * Adapterデータを格納するDBテーブル上のColumn名や、Causeの条件指定時のColumn名に利用される<br>
     *
     * @return String[] カラム名
     */
    public String[] getDataColumnNames() {
        return this.columnList;
    }


    public Map read() throws Exception {

        Map retMap = null;

        try {
            retMap = new LinkedHashMap();
            String[] data = (String[])this.nextDataQueue.take();

            StringBuilder debugData = new StringBuilder();
            String sep ="";
            for (int idx = 0; idx < columnList.length; idx++) {
                debugData.append(sep);
                debugData.append("[" + idx + "]=\"" + data[idx] + "\"");
                sep = ",";
                retMap.put(this.columnList[idx], data[idx]);
            }
            super.debug("Server Input=[" + debugData + "]");
            // -output指定
            super.outputAdapterData(data);

        } catch (Exception e) {
            //System.err.println("readLine=" + readLine);
            //throw new SetsunaException("Adapter read string =[" + readLine + "]", e);
        }
        return retMap;
    }

    public boolean stop() {
        this.stopFlg = true;
        this.innerServer.server.close();
        this.innerServer.loop.shutdown();
        return true;
    }

    class InnerServer extends Thread {
        Server server = null;
        EventLoop loop = null;

        InnerServer(Server server, EventLoop loop) {
            this.server = server;
            this.loop = loop;
        }

        public void run() {
            try {
                loop.join();
            } catch(Exception e) {
                try {
                    server.close();
                    loop.shutdown();
                } catch(Exception e2) {
                }
            }
        }
    }
}