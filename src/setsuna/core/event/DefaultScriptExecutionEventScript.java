package setsuna.core.event;

import java.util.*;
import java.io.*;

import setsuna.core.util.*;


/**
 * -eventオプションでユーザが指定した外部スクリプトを<br>
 * 実行するイベント用スクリプト.<br>
 *
 * @author T.Okuyama
 */
public class DefaultScriptExecutionEventScript implements IScript {

    private String executeScriptStr = null;
    private int outputDataType = 1;

    private Runtime runtime = null;
    private Process process = null;
    private OutputStream os = null;
    private BufferedWriter bw = null;



    /**
     * コンストラクタ.<br>
     *
     * @param executeScriptStr 実行予定のコマンド文字列
     * @param outputDataType 実行コマンドに渡す引数のデータの形式を指定(1=JSON, 2=カンマ区切り)
     * @throw Exception
     */
    public DefaultScriptExecutionEventScript(String executeScriptStr, int outputDataType) throws Exception {
        this.executeScriptStr = executeScriptStr;
        this.outputDataType = outputDataType;
    }

    /**
     * ユーザコマンドを実行.<br> 
     * 実行時に、inputで入ってきたデータを指定された形式で文字列に変換して引数として渡す
     *
     * @param data
     * @throw Exception
     */
    public void execute(Map data) throws Exception {
        try {
            StringBuilder argStr = new StringBuilder(100);

            // 引数となるデータを指定フォーマットに合わせて文字列化
            if (this.outputDataType == 1) {

                // JSON
                argStr.append("\"{");
                String sep = "";

                Set entrySet = data.entrySet();
                Iterator entryIte = entrySet.iterator(); 

                while(entryIte.hasNext()) {

                    Map.Entry obj = (Map.Entry)entryIte.next();

                    argStr.append(sep);
                    argStr.append("\"");
                    argStr.append(obj.getKey());
                    argStr.append("\"");
                    argStr.append(":");
                    argStr.append("\"");
                    argStr.append(obj.getValue());
                    argStr.append("\"");
                    sep = ",";
                }
                argStr.append("}\"");
            } else if (this.outputDataType == 2){

                // カンマ区切り
                argStr.append("\"");
                String sep = "";

                Set entrySet = data.entrySet();
                Iterator entryIte = entrySet.iterator(); 

                while(entryIte.hasNext()) {

                    Map.Entry obj = (Map.Entry)entryIte.next();

                    argStr.append(sep);
                    argStr.append(obj.getValue());
                    sep = ",";
                }
                argStr.append("\"");
            }
            this.runtime = Runtime.getRuntime();
            SystemUtil.debug("-event=[" + executeScriptStr + " " + argStr.toString() + "]");
            this.process = this.runtime.exec(executeScriptStr + " " + argStr.toString());
        } catch (Exception e) {
            throw e;
        }
    }
}