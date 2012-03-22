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
 * 外部からの入力を受け付けるSetsuna用のサーバ.<br>
 *
 * @author T.Okuyama
 */
public class SetsunaServer {

    private Map executeAdapterEngineMap = new ConcurrentHashMap(100, 80, 64);

    private Map executeQueryEngineMap = new ConcurrentHashMap(100, 80, 64);

    private Map executeEventEngineMap = new ConcurrentHashMap(100, 80, 64);

    private String setsunaServerInfomation = null;


    public static void main(String[] args) {
        SetsunaServer me = new SetsunaServer(args[0]);
        SetsunaStaticConfig.initializeConfig(args);

        try {
            me.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SetsunaServer(String setsunaServerInfomation) {
        this.setsunaServerInfomation = setsunaServerInfomation;
    }


    public void startServer() throws Exception {

        String[] info = this.setsunaServerInfomation.split(":");
        String bindAddr = info[0];
        int portNo = Integer.parseInt(info[1]);


        try {

            // サーバーソケットの生成
            ServerSocket serverSocket = new ServerSocket(portNo);
            Socket socket;

            // メインループ
            while(true) {
                try {
                    System.out.println("クライアントからの接続をポート" + new Integer(portNo).toString() + "で待ちます");
                    // クライアントからの接続を待ちます
                    socket = serverSocket.accept();
                    System.out.println(socket.getInetAddress() + "から接続を受付ました");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch(Exception e) {
            throw e;
        }
    }
}