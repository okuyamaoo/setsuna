package setsuna.core.adapter;


import java.util.*;
import java.io.*;
import java.util.concurrent.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import setsuna.core.util.*;


/**
 * SetsunaMainを利用して立ち上げる場合の入力用のAdapter.<br>
 * Server型.<br>
 * 内部ではJettyを利用してHTTPサーバを立ち上げている.<br>
 * カラム定義としてはHTTPリクエスト内のParamterを全てデータとして受け取るGet&Post&Put全て同様に扱う.<br>
 *
 * @author T.Okuyama
 */
public class DefaultHttpServerAdapter extends AbstractDefaultAdapter implements IAdapter  {

    static String[] columnList = null;
    static ArrayBlockingQueue nextDataQueue = new ArrayBlockingQueue(1000000);

    private long arrivalTime = 0L;

    static Object nextDataSync = new Object();
    static boolean firstAccess = true;

    private ServletHandler handler = null;
    private Server server = null;

    private InnerHttpServer innerServer = null;

    private boolean stopFlg = false;


    public DefaultHttpServerAdapter(String bindAddress, int bindPort, String[] columnList, long arrivalTime, String context) throws Exception {
        DefaultHttpServerAdapter.columnList = columnList;
        this.arrivalTime = arrivalTime;

        server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setHost(bindAddress);
        connector.setPort(bindPort);
        server.addConnector(connector);
        
        ServletHandler handler = new ServletHandler();
        
        // サーブレットクラスのマッピング
        if (context == null) {
            handler.addServletWithMapping(setsuna.core.adapter.HttpDataInputServlet.class, "/*");
        } else {
            handler.addServletWithMapping(setsuna.core.adapter.HttpDataInputServlet.class, "/" + context + "/*");
        }

        server.setHandler(handler);
        
        server.start();
        this.innerServer = new InnerHttpServer(this.server);
        this.innerServer.start();
        while(DefaultHttpServerAdapter.columnList == null) {
            Thread.sleep(10);
        }
    }



    /**
     * Adapterの名前を返す.<br>
     * Adapterデータを格納するDB上のテーブル名としても利用される<br>
     *
     * @return String Adapter名(テーブル名)
     */
    public String getName() {
        return SetsunaStaticConfig.DEFAULT_HTTP_SERVER_TABLE_NAME;
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
        return DefaultHttpServerAdapter.columnList;
    }


    public Map read() throws Exception {

        Map retMap = null;

        try {
            retMap = (Map)DefaultHttpServerAdapter.nextDataQueue.take();

            super.debug("Server Input=[" + retMap + "]");
            // -output指定
            super.outputAdapterData(retMap.toString());
        } catch (Exception e) {
            //System.err.println("readLine=" + readLine);
            //throw new SetsunaException("Adapter read string =[" + readLine + "]", e);
        }
        return retMap;
    }

    public boolean stop() {
        this.stopFlg = true;
        try {
            this.innerServer.server.stop();
        } catch (Exception e){}
        return true;
    }


    class InnerHttpServer extends Thread {
        Server server = null;

        InnerHttpServer(Server server) {
            this.server = server;
        }

        public void run() {
            try {
                this.server.join();
            } catch(Exception e) {
                try {
                    server.stop();
                } catch(Exception e2) {
                }
            }
        }
    }
}