package setsuna.core.util;


/**
 * Setsuna用の定数及び、起動引数管理クラス<br>
 *
 * @author T.Okuyama
 */
public class SetsunaStaticConfig {

    public volatile static String STREAM_DATABASE_DRIVER_NAME = "org.h2.Driver";

    public volatile static String STREAM_DATABASE_URI = "jdbc:h2:streamdb;MULTI_THREADED=TRUE;DB_CLOSE_DELAY=-1";
    public volatile static String STREAM_DATABASE_LOCAL_SERVER_URI = "jdbc:h2:tcp://localhost:9092/mem:streamdb;MULTI_THREADED=TRUE;DB_CLOSE_DELAY=-1";
    public volatile static String STREAM_DATABASE_LOCAL_CLIENT_URI = "jdbc:h2:tcp://localhost:9092/mem:streamdb;MULTI_THREADED=TRUE;DB_CLOSE_DELAY=-1";

    // SetsunaMainを利用したパイプインプット時の設定情報
    public volatile static String DEFAULT_PIPEINPUT_TABLE_NAME = "PIPE";

    public volatile static String[] DEFAULT_PIPEINPUT_COLUMN_LIST = null;

    public volatile static String DEFAULT_PIPEINPUT_SEP = " ";

    public volatile static int DEFAULT_PIPEINPUT_SEP_TYPE = 2; // 1はセパレータでsplitするだけ、2はセパレータの2個以上の連続をあらかじめ削除してからsplit実行、3はタイムセパレータ

    public volatile static int DEFAULT_PIPEINPUT_DATA_SEP_TYPE = 1; // PipeInputAdapterの1データの区切り情報(デフォルトは改行) 2=時間(データを着信してから100ミリ秒以内に次のデータがこない場合は1データとする 3=ユーザ指定文字列

    public volatile static String DEFAULT_PIPEINPUT_DATA_SEP_STR = ""; // PipeInputAdapterの1データの区切りのユーザ指定文字列

    public volatile static String DEFAULT_PIPEINPUT_QUERY_TARGET = null; // デフォルトのスタンドアローンモードでどのデータをチェックするかの名前(標準入力を対象とする場合は省略)

    public volatile static String DEFAULT_PIPEINPUT_QUERY_CAUSE = null;

    public volatile static String DEFAULT_PIPEINPUT_QUERY_CONDITION = null;

    public volatile static String DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_EVENT = "setsuna.core.event.DefaultScriptExecutionEventScript";

    public volatile static String DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT = null;

    public volatile static boolean DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_QUERY_EVENT = false;
    public volatile static String DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_QUERY_EVENT_QUERY = "";

    public volatile static int DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT_ARGTYPE = 1; // 1=JSON, 2=CSV

    public volatile static long DEFAULT_PIPEINPUT_DATA_ARRIVAL_TIME = 600*1000;

    public volatile static int DEFAULT_PIPEINPUT_QUERY_TYPE = 1; // 1=通常のカラム取得select句、2=count文のselect句




    public volatile static boolean SETSUNA_LOCAL_MODE = false;
    public volatile static boolean SETSUNA_LOCAL_SERVER = false;


    /**
     * 起動引数を定数に代入する初期化処理.<br>
     * 起動時に一度だけ呼ばれる想定<br>
     *
     *
     * --sv IP:Port&BuckPort
     * --a AapterScriptDir 
     * --q QueryScriptDir 
     * --e EventScriptDir 
     * -db DBType 
     * -dbf DBTypeがファイルの場合のファイルの名前(省略時はsetsunadb固定)
     * -column 自身への標準入力をAdapterとして受ける場合に、その情報のカラム定義(標準ではCOLUMN1、COLUMN2、・・・と定義される)
     * -sep 自身への標準入力をAdapterとして受ける場合に、その情報をカラム情報とて扱うためにインプットを分解するセパレータ(標準は" ")
     * -sept 自身への標準入力をAdapterとして受ける場合に、その情報をカラム情報とて扱うためにインプットを分解するセパレータが2個以上続いた場合に1つとして扱う指定(標準では扱われない)
     * -dst 自身への標準入力をAdapterとして受ける場合に、送られてくるデータを1データとして扱う区切りの指定 1=改行(デフォルト) 2=時間
     * -trigger columnname like ABC
     * -query select * from (select avg(to_number(COLUMN10)) as avgld from PipeAdapter order by COLUMN1 desc limit 10)) t1 where t1.avgld > 2
     * -count -query指定がcount文であることを指定 true=Count文
     * -event イベントで実行するスクリプト(シェルやbatなど)
     * -eventquery イベントを-event指定でのシェルやバッチではなく、任意のSQLを実行させその結果をJSONで画面に出力したい場合はこのオプションにSQLを記述する。
     * -argtype ユーザイベントへの引数のフォーマット(JSON, CSV)
     * -stream inputで受け取った際のデータStreamの名前。内部ではテーブル名などに利用(指定がない場合は"pipe"となる)
     * -atime inputで受け取った際のデータの有効期限を秒で指定する
     * 
     * @param startArgument 起動引数のリスト
     */
    public static void initializeConfig(String[] startArgument) {


        for (int i = 0; i < startArgument.length; i++) {


            if (startArgument[i].trim().equals("-column")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        String[] makeColumnList = startArgument[i+1].trim().split(" ");

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_COLUMN_LIST = new String[makeColumnList.length];
                        // 扱うカラムを全て大文字とする
                        for (int idx = 0; idx < makeColumnList.length; idx++) {
                            SetsunaStaticConfig.DEFAULT_PIPEINPUT_COLUMN_LIST[idx] = makeColumnList[idx].toUpperCase();
                        }
                    }
                }
            }

            if (startArgument[i].trim().equals("-sep")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_SEP = startArgument[i+1];
                    }
                }
            }

            if (startArgument[i].trim().equals("-sept")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_SEP_TYPE = Integer.parseInt(startArgument[i+1].trim());
                    }
                }
            }

            if (startArgument[i].trim().equals("-dst")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_SEP_TYPE = Integer.parseInt(startArgument[i+1].trim());
                    }
                }
            }

            if (startArgument[i].trim().equals("-trigger")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CAUSE = startArgument[i+1];
                    }
                }
            }


            if (startArgument[i].trim().equals("-query")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CONDITION = startArgument[i+1];
                    }
                }
            }


            if (startArgument[i].trim().equals("-event")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT = startArgument[i+1];
                    }
                }
            }


            if (startArgument[i].trim().equals("-argtype")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {
                        if (startArgument[i+1].trim().toUpperCase().equals("CSV")) 
                            SetsunaStaticConfig.DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT_ARGTYPE = 2;
                    }
                }
            }

            if (startArgument[i].trim().equals("-stream")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_TABLE_NAME = startArgument[i+1].trim();
                    }
                }
            }

            if (startArgument[i].trim().equals("-atime")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {
                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_ARRIVAL_TIME = Long.parseLong(startArgument[i+1])*1000;
                    }
                }
            }

            if (startArgument[i].trim().equals("-count")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {
                        if (startArgument[i+1].trim().toUpperCase().equals("TRUE")) 
                            SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TYPE = 2;
                    }
                }
            }

            if (startArgument[i].trim().equals("-target")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TARGET = startArgument[i+1].trim();
                    }
                }
            }

            if (startArgument[i].trim().equals("-eventquery")) {
                if (startArgument.length > (i+1)) {
                    if (startArgument[i+1] != null) {

                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_QUERY_EVENT = true;
                        SetsunaStaticConfig.DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_QUERY_EVENT_QUERY = startArgument[i+1].trim();
                    }
                }
            }


        }
    }
}