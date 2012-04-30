package setsuna.core.adapter;

import java.util.*;
import java.io.*;
import java.util.concurrent.*;

import javax.servlet.ServletException;
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
 * Servletクラス.<br>
 * カラム定義としてはHTTPリクエスト内のParamterを全てデータとして受け取るGet&Post&Put全て同様に扱う.<br>
 *
 * @author T.Okuyama
 */
public class HttpDataInputServlet extends HttpServlet {

        /**
         * JettyのHTTPサーバに呼ばれるServletメソッド.<br>
         * 内部ではQueueにデータをいれているだけ<br>
         * 本メソッドは戻り値にデータの投入の成否を返す.<br>
         *
         * @param data クライアントから渡されたデータ配列(カラム数分の想定。違っている場合は-9を返す)
         * @return int 正常終了 0, カラム定義と合わない -9, 内部エラー -1
         */
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            doGet(request, response);
        }

        public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            if (DefaultHttpServerAdapter.firstAccess)  {
                synchronized (DefaultHttpServerAdapter.nextDataSync) {
                    if (DefaultHttpServerAdapter.firstAccess && DefaultHttpServerAdapter.columnList == null) {
                        // カラム定義が初期化されていなければ作る
                        List parameterList = new ArrayList();
                        Enumeration e = request.getParameterNames();

                        while (e.hasMoreElements()) {
                            String name = (String) e.nextElement();
                            parameterList.add(name.toUpperCase());
                        }
                        DefaultHttpServerAdapter.columnList = new String[parameterList.size()];
                        for (int idx = 0; idx < parameterList.size(); idx++) {
                            DefaultHttpServerAdapter.columnList[idx] = (String)parameterList.get(idx);
                        }
                    }
                    DefaultHttpServerAdapter.firstAccess = false;
                }
            }

            try {
                Map dataMap = new LinkedHashMap();
                Map tmpDataMap = new HashMap();
                Enumeration e = request.getParameterNames();

                while (e.hasMoreElements()) {
                    String name = (String) e.nextElement();
                    tmpDataMap.put(name.toUpperCase(), request.getParameter(name));
                }


                for (int idx = 0; idx < DefaultHttpServerAdapter.columnList.length; idx++) {

                    String data = (String)tmpDataMap.get(DefaultHttpServerAdapter.columnList[idx]);
                    if (data != null)
                        dataMap.put(DefaultHttpServerAdapter.columnList[idx], data);
                }

                if (dataMap.size() != DefaultHttpServerAdapter.columnList.length) {
                    response.setStatus(400);
                } else {
                    DefaultHttpServerAdapter.nextDataQueue.put(dataMap);
                    response.setStatus(200);
                }
            } catch(Exception e) {
                e.printStackTrace();
                response.setStatus(500);
            }
        }
    }
