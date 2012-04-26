package setsuna.core.adapter;


import java.util.*;
import java.io.*;

import setsuna.core.util.*;


/**
 * パイプライン入力用のAdapter.<br>
 *
 * @author T.Okuyama
 */
public class DefaultPipeInputAdapter extends AbstractDefaultAdapter implements IAdapter {

    private InputStream is = null;
    private String[] columnList = null;
    private String sep = null;
    private int sepType = 1;
    private int dataSepType = 1;
    private String dataSepStr = "";
    private long arrivalTime = 0L;

    private CustomLineReader in = null;
    private String testReadLine = null;

    private boolean stopFlg = false;


    public DefaultPipeInputAdapter(InputStream is, String[] columnList, String sep, int sepType, int dataSepType, String dataSepStr, long arrivalTime) {

        this.is = is;
        this.columnList = columnList;
        this.sep = sep;
        this.sepType = sepType;
        this.dataSepType = dataSepType;
        this.dataSepStr = dataSepStr;
        this.arrivalTime = arrivalTime;

        this.in = new CustomLineReader(is);
        String testLine = null;

        if (columnList == null) {
            try {
                if (dataSepType == 2) {

                    ByteArrayOutputStream strBuf = new ByteArrayOutputStream();

                    while (true) {

                        if (this.in.ready()) {
                            strBuf.write(this.in.read());
                        } else {
                            Thread.sleep(100);
                            if (strBuf.toString().equals("")) continue;
                            if (!this.in.ready()) break;
                        }
                    }

                    testLine = strBuf.toString();
                    testLine = testLine.replaceAll("\r\n", "\n");
                    testLine = testLine.replaceAll("\n\n", "\n");
                    testLine = testLine.replaceAll("\n", this.sep);
                } else {

                    testLine = this.in.readLine();
                }

                // セパレート文字列の連続つぶす場合の処理
                if (this.sepType == 2 && testLine != null) {
                    while (true) {
                        testLine = testLine.replaceAll(this.sep+this.sep, this.sep);
                        if (testLine.indexOf(this.sep+this.sep) == -1) break;
                    }
                }

                String[] testSplit = testLine.split(sep);
                this.columnList = new String[testSplit.length];

                for (int idx = 0; idx < testSplit.length; idx++) {
                    this.columnList[idx] = "COLUMN" + idx;
                }

                this.testReadLine = testLine;
            } catch(Exception e) {}
        }
    } 

    /**
     * Adapterの名前を返す.<br>
     * Adapterデータを格納するDB上のテーブル名としても利用される<br>
     *
     * @return String Adapter名(テーブル名)
     */
    public String getName() {
        return SetsunaStaticConfig.DEFAULT_PIPEINPUT_TABLE_NAME;
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
        String readLine = null;
        try {

            if (this.testReadLine != null) {
                retMap = new LinkedHashMap();

                // デバッグ
                super.debug("Pipe Input=[" + this.testReadLine + "]");
                // -output指定
                super.outputAdapterData(this.testReadLine);

                String[] splitData = this.testReadLine.split(sep);
                for (int idx = 0; idx < columnList.length; idx++) {
                    retMap.put(this.columnList[idx], splitData[idx]);
                }

                this.testReadLine = null;
            } else {

                retMap = new LinkedHashMap();

                if (dataSepType == 2) {

                    ByteArrayOutputStream strBuf = new ByteArrayOutputStream();
                    while (true) {
                        if (this.in.ready()) {

                            strBuf.write(this.in.read());
                        } else {
                            Thread.sleep(100);
                            if(strBuf.toString().equals("")) continue;
                            if (!this.in.ready()) break;
                        }
                    }
                    readLine = strBuf.toString();
                    readLine = readLine.replaceAll("\r\n", "\n");
                    readLine = readLine.replaceAll("\n\n", "\n");
                    readLine = readLine.replaceAll("\n", this.sep);
                } else {
                    readLine = this.in.readLine();
                }

                // デバッグ
                super.debug("Pipe Input=[" + readLine + "]");
                // -output指定
                super.outputAdapterData(readLine);

                // セパレート文字列の連続つぶす場合の処理
                while (true) {
                    readLine = readLine.replaceAll(this.sep+this.sep, this.sep);
                    if (readLine.indexOf(this.sep+this.sep) == -1) break;
                }

                String[] splitData = readLine.split(sep);
                for (int idx = 0; idx < columnList.length; idx++) {
                    retMap.put(this.columnList[idx], splitData[idx]);
                }
            }

        } catch (Exception e) {
            //System.err.println("readLine=" + readLine);
            //throw new SetsunaException("Adapter read string =[" + readLine + "]", e);
        }
        return retMap;
    }

    public boolean stop() {
        this.stopFlg = true;
        return true;
    }



}