package setsuna.core;

import java.io.*;
import java.util.*;
import java.net.*;

import setsuna.core.util.*;
import setsuna.core.event.*;
import setsuna.core.query.*;
import setsuna.core.adapter.*;

import org.h2.tools.Server;


/**
 * コマンドライン用Setsunaラッパー.<br>
 * 1プロセス1イベントになる.<br>
 * お手軽イベントプロセス用.<br>
 *
 * @author T.Okuyama
 */
public class SetsunaMain {

    private SetsunaCore setsunaCore = null;

    /**
     * --sv IP:Port&BuckPort
     * --a AapterScriptDir 
     * --q QueryScriptDir 
     * --e EventScriptDir 
     * -db DBType 
     * -dbf DBTypeがファイルの場合のファイルの名前(省略時はsetsunadb固定)
     * -masterport スタンドアロンモードで起動する場合、マスターサーバとして起動するためのチェックに利用するポート番号
     * -dbport スタンドアロンモードで起動する場合の内部DBのポート番号
     * -server サーバモードの指定 true=サーバモード、false=パイプ入力(デフォルトはこちら)
     * -offset 入力されたデータを実際に取り込み開始するレコードの位置。ここで指定した数分読み込みをスキップする
     * -column 自身への標準入力・サーバモード入力をAdapterとして受ける場合に、その情報のカラム定義(指定しない場合はではCOLUMN0、COLUMN1、・・・と定義される)
     * -sep 自身への標準入力・サーバモード入力をAdapterとして受ける場合に、その情報をカラム単位に分解するためのセパレータ文字列(標準は" ")
     * -sept 自身への標準入力・サーバモード入力をAdapterとして受ける場合に、その情報をカラム情報とて扱うためにインプットを分解するセパレータが2個以上続いた場合に1つとして扱う指定(標準では扱われない) 1=なにもしない 2=不要なセパレータを消す(デフォルト)
     * -dst 自身への標準入力・サーバモード入力をAdapterとして受ける場合に、送られてくるデータを1データとして扱う区切りの指定 1=改行(デフォルト) 2=時間
     * -skiperror カラム定義と異なるデータが入力された場合にExceptionを発行せずに無視する設定
     * -target 標準入力以外のデータを監視したい場合に指定する。例えばsarなどの標準入力をどれかのプロセスが受けてそれに複数のプロセッシングを行いたい場合に名前を指定。指定する名前は対象のプロセスの-stream引数の値
     * -trigger columnname like ABC
     * -query select * from (select avg(to_number(COLUMN10)) as avgld from PipeAdapter order by COLUMN1 desc limit 10)) t1 where t1.avgld > 2
     * -count -query指定がcount文であることを指定 true=Count文
     * -easyquery SQLを直接記述せずに関数を呼び出し、SQLでの確認と同等のことを行う。実行可能な関数はavg_over、avg_below、over_value、below_value、avg_more_over
     * -event イベントで実行するスクリプト(シェルやbatなど)
     * -eventquery イベントを-event指定でのシェルやバッチではなく、任意のSQLを実行させその結果をJSONで標準出力に出力したい場合はこのオプションにSQLを記述する。
     * -argtype ユーザイベントへの引数のフォーマット(JSON, ",")
     * -stream inputで受け取った際のデータStreamの名前。内部ではテーブル名などに利用(指定がない場合は"pipe"となる)
     * -atime inputで受け取った際のデータの有効期限を秒で指定する
     * -errorlog エラー時の出力を標準出力ではなく、指定したファイルに出力しますた。
     * -debug Inputデータ文字列、実行した-trigger、実行したSQL、実行したユーザイベントコマンドを標準出力に出力する(on指定=でdebugとSetsunaの出力両方、only=デバッグのみ出力)
     */
    public String[] startArgument = null; 

    public SetsunaMain(String[] startArgument) {

        SetsunaStaticConfig.initializeConfig(startArgument);
        this.startArgument = startArgument;
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1)  {
                if (args[0].toUpperCase().equals("-HELP") || args[0].toUpperCase().equals("-H")) {

                    System.out.println(" Setsunaは複合イベント処理エンジン(CEP)です.");
                    System.out.println(" ");
                    System.out.println(" 複合イベント処理エンジンは複数のデータストリームを");
                    System.out.println(" インプットとして一時的に蓄積しそれらのデータから複雑な条件で");
                    System.out.println(" データの変化を検知し、ユーザスクリプトを速やかに実行することが可能です。");
                    System.out.println(" リアルタイムにデータの変動をキャッチし、処理を行うことが自動化できます。");
                    System.out.println(" ");
                    System.out.println(" SetsunaMainはエンジンのI/Fを良い扱いやすくしたプログラムです。");
                    System.out.println(" 処理を行いたいデータのエンジンへの投入にパイプライン・サーバモード入力を採用しており");
                    System.out.println(" ダイレクトに他のスクリプトと組み合わせることが可能となっています。");
                    System.out.println(" ");
                    System.out.println(" また1台のサーバ内で複数のプロセスでSetsunaMainを利用した場合であっても");
                    System.out.println(" 自動的に1つのDB上に全てのデータが集合的に管理されます。");
                    System.out.println(" そのためデータの変化を検知するプロセッシング部分では");
                    System.out.println(" 全てのインプットデータへ横断的にデータの変化をサーチすることが可能です。");
                    System.out.println(" ");
                    System.out.println(" そして、プロセッシングの部分で条件にマッチする変化を検知するとユーザが自由に");
                    System.out.println(" 作成したイベントを実行することが可能です。このイベントにはシェルやバッチといった");
                    System.out.println(" 標準的なスクリプトを指定可能にしてあります。");
                    System.out.println(" ※注意点としてはSetsunaMainに渡すデータのフォーマットは常に一定である必要があります。");
                    System.out.println("   例えば入力データの1レコード目と2レコード目が異なる場合正しく動きません。");
                    System.out.println("   grep、awkコマンドなどを組み合わせて常に一定にしてください。");
                    System.out.println(" ");
                    System.out.println(" これらのことから以下のようなフローをSetsunaMainを利用して組み上げることが可能です。");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("  ----- フロー -----");
                    System.out.println("   標準的なコマンドからの出力 => Setsunaによるプロセッシング => イベントスクリプト実行");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" SetsunaMainはかならずパイプからの標準入力か-serverモードでのデータ入力が必要です。");
                    System.out.println(" これは入力データの1データ単位でデータ検知=>イベント実行が動くためです。");
                    System.out.println(" ");
                    System.out.println(" パイプ・サーバモードからの入力から投入したデータは全て自動的に作成されるSetsuna内のH2Database上のテーブルに格納されます。");
                    System.out.println(" データベースなどを準備する必要はありません。SetsunaMainを実行するだけです。");
                    System.out.println(" ");
                    System.out.println(" プロセッシングによりデータの変化を検知する部分には簡単な条件記述となるTriggerと、SQLによるQueryの");
                    System.out.println(" 2段階での検知方式を採用しています。");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" 以下にLinuxの管理コマンドであるsarの結果を入力とした場合の簡単な利用例をいくつか示します。");
                    System.out.println(" 例1) SetsunaMainはパイプラインに繋ぐだけの場合は受け取ったデータを内部的にどのような");
                    System.out.println("      テーブル構造にマッピングしているかを標準出力にJSON形式で出力します。");
                    System.out.println("   $sar -u 2 1000 | grep  --line-buffered all | java -jar setsuna.jar");
                    System.out.println("");
                    System.out.println("   作者の環境はCentOS5.5を利用しておりsarコマンドの結果は以下になります。");
                    System.out.println("   $sar -u 2 1000");
                    System.out.println("   Linux 2.6.18-194.el5 (localhost.localdomain)    2012年XX月XX日");
                    System.out.println("");
                    System.out.println("   20時33分26秒       CPU     %user     %nice   %system   %iowait    %steal     %idle");
                    System.out.println("   20時33分28秒       all      0.00      0.00      0.00      0.00      0.00    100.00");
                    System.out.println("   20時33分30秒       all      0.00      0.00      0.00      0.00      0.00    100.00");
                    System.out.println("   20時33分32秒       all      0.00      0.00      0.00      0.00      0.00    100.00");
                    System.out.println("-----------------------------------------------------------------------------------------");
                    System.out.println("   上記のデータをそのままSetsunaMainに送り込むと1行目を利用してテーブル構造を構築していしまい、");
                    System.out.println("   2行目以降のデータ項目の構造が異なるため、ただしく読み込めません。そこで、grepコマンドを利用して");
                    System.out.println("   CPUの列に'all'という文字列が出力されている行だけ取り込んでいます。");
                    System.out.println("");
                    System.out.println("   取り込んだ後にSetsunaから出力された結果は以下になりました。");
                    System.out.println("    {\"COLUMN0\":\"20時38分54秒\",\"COLUMN1\":\"all\",\"COLUMN2\":\"3.62\",\"COLUMN3\":\"0.00\",\"COLUMN4\":\"0.37\",\"COLUMN5\":\"0.00\",\"COLUMN6\":\"0.00\",\"COLUMN7\":\"96.00\"}");
                    System.out.println("    {\"COLUMN0\":\"20時38分56秒\",\"COLUMN1\":\"all\",\"COLUMN2\":\"21.53\",\"COLUMN3\":\"0.00\",\"COLUMN4\":\"0.88\",\"COLUMN5\":\"0.00\",\"COLUMN6\":\"0.00\",\"COLUMN7\":\"77.60\"}");
                    System.out.println("    {\"COLUMN0\":\"20時38分58秒\",\"COLUMN1\":\"all\",\"COLUMN2\":\"0.12\",\"COLUMN3\":\"0.00\",\"COLUMN4\":\"0.00\",\"COLUMN5\":\"0.00\",\"COLUMN6\":\"0.00\",\"COLUMN7\":\"99.88\"}");
                    System.out.println("   JOSNフォーマットで'カラム名:データ'という形式です。");
                    System.out.println("   それぞれ以下のようにマッピングされています。");
                    System.out.println("    COLUMN0=時刻");
                    System.out.println("    COLUMN1=CPU");
                    System.out.println("    COLUMN2=%user");
                    System.out.println("    COLUMN3=%nice");
                    System.out.println("    COLUMN4=%system");
                    System.out.println("    COLUMN5=%iowait");
                    System.out.println("    COLUMN6=%steal");
                    System.out.println("    COLUMN7=%idle");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" 例2) 受け取ったデータに対して、プロセッシングであるTriggerを適応してみましょう。");
                    System.out.println("      Triggerを利用する場合\"-trigger\"オプションを指定します。");
                    System.out.println("      指定している条件はCOLUMN7にマッピングされているCPUのアイドル値が90%を下回っていることを条件にしています。");
                    System.out.println("      ※データがマッチした際に実行するユーザオリジナルのスクリプトを指定しない場合は標準出力に");
                    System.out.println("        入力されたパイプ入力からのデータが出力されます・。");
                    System.out.println("   $sar -u 2 10000 | grep  --line-buffered all | java -jar setsuna.jar -trigger \"COLUMN7 < 90\"");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" 例3) 受け取ったデータに対して、プロセッシングであるTriggerとQuery適応してみましょう。");
                    System.out.println("      Queryを利用する場合\"-query\"オプションを指定します。");
                    System.out.println("      指定している条件はTriggerが例2と同様でQueryがCOLUMN0にマッピングされているsarの");
                    System.out.println("      取得時間の降順で並べたデータの上位5件のデータ内のCOLUMN5にマッピングされているI/OWait値の平均が10以上であるかを検知しています。");
                    System.out.println("      ※データがマッチした際に実行するユーザオリジナルのスクリプトを指定しない場合は標準出力に入力されたパイプ入力からのデータが出力されます。");
                    System.out.println("   $sar -u 2 10000 | grep  --line-buffered all | java -jar setsuna.jar -trigger \"COLUMN7 < 90\" -query \"select * from (select avg(to_number(column5)) as avgio from (select column5 from pipe order by column0 desc limit 5) as t1)  where avgio > 10\"");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" 例4) 次の例では例3でマッチした場合にオリジナルで作成したシェルスクリプトを実行してみましょう。");
                    System.out.println("      ユーザ作成のスクリプトを実行する場合\"-event\"オプションを指定します。");
                    System.out.println("   $sar -u 2 10000 | grep  --line-buffered all | java -jar setsuna.jar -trigger \"COLUMN7 < 90\" -query \"select * from (select avg(to_number(column5)) as avgio from (select column5 from pipe order by column0 desc limit 5) as t1)  where avgio > 10\" -event /home/setsuna/WarningIOWait.sh");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" 例5) 次の例は応用例になります。ここまでは1つのインプットデータへ検索処理を行ってきましたが、");
                    System.out.println("      複数のインプットデータを処理してみましょう。");
                    System.out.println("      複数のインプットデータを処理するには、まず複数のパイプ入力をSetsunaに投入する必要があります。");
                    System.out.println(" ");
                    System.out.println("      そのために2つのターミナルを起動してください。");
                    System.out.println("       [ターミナル1]ではsarの結果をいままでどおり投入しています。そしてデータに'-stream'オプションを利用してsarという名前を付けています。");
                    System.out.println(" ");
                    System.out.println("        $sar -u 2 10000 | grep  --line-buffered all | java -jar setsuna.jar -stream sar");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("       [ターミナル2]ではtopの先頭行を投入しています。そしてそのデータにtopという名前を付けています。");
                    System.out.println("        さらに-triggerにてload averageの値(COLUMN11)が1以上であることを監視し");
                    System.out.println("        さらに-queryにてターミナル1で投入しているsarのデータの直近I/OWaitの値が10以上であるかを検知しています。");
                    System.out.println("        この-triggerと-queryの両方がマッチした場合に-eventで指定したユーザスクリプトを呼び出しています。");
                    System.out.println(" ");
                    System.out.println("        $top -b -d 1 | grep --line-buffered ^top | java -jar setsuna.jar -stream top -trigger \"COLUMN11 > 1\" -query \"select * from (select avg(to_number(column5)) as avgio from (select column5 from sar order by column0 desc limit 5) as t1)  where avgio > 10\" -event /home/setsuna/WarningIOWait.sh");
                    System.out.println("      このように、複数のインプットデータを横断的に探索することで複数のデータを組み合わせた複雑な検証も可能です。");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("  ----------------------------");
                    System.out.println("  --- 各オプション詳細一覧 ---");
                    System.out.println("  ----------------------------");
                    System.out.println("   ※特に重要なオプションは-server、-trigger -query -event -stream -sep -dstです");
                    System.out.println("   ※デバッグ関係のオプションは-debug、-errorlogです");
                    System.out.println("  ");
                    //System.out.println(" -stream:標準入力から受取るデータの名前。ここで指定した名前で一時テーブルや、-target指定の領域が作成される");
                    System.out.println(" -stream:標準入力・サーバモード入力から受取るデータの名前。ここで指定した名前で一時テーブルが作成されるため、-queryなどでこの指定値を利用する。");
                    System.out.println("        **省略可能**");
                    System.out.println("        ※省略した場合は\"pipe\"となる");
                    System.out.println("        [指定例]");
                    System.out.println("          -stream sartable");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -server:サーバモードの起動指定");
                    System.out.println("         サーバモードで起動した場合、MessagePack-RPCで作られたサーバでデータの投入を待ち受ける");
                    System.out.println("         MessagePack-RPCで作成されたクライアントでデータを投入することが出来る");
                    System.out.println("         本モードで起動した場合に-query等で利用するテーブル名は-streamを指定しない場合、'server'というテーブル名になる");
                    System.out.println("         サーバ側で定義されているRPCメソッド定義は以下");
                    System.out.println("         [メソッド定義]");
                    System.out.println("          int next ( String[] sendData )");
                    System.out.println("         ----------------------------------");
                    System.out.println("         **省略可能**");
                    System.out.println("         ※省略した場合はパイプ入力となる");
                    System.out.println("         [指定例]");
                    System.out.println("           -server true");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -bindaddr:サーバモードの起動時のサーバがバインドするアドレス");
                    System.out.println("           サーバモードで起動した場合のみ有効");
                    System.out.println("           **省略可能**");
                    System.out.println("           ※省略した場合は0.0.0.0にバインドされる");
                    System.out.println("           [指定例]");
                    System.out.println("            -bindaddr 192.168.1.1");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -bindport:サーバモードの起動時のサーバが待ち受けるポート番号");
                    System.out.println("           サーバモードで起動した場合のみ有効");
                    System.out.println("           **省略可能**");
                    System.out.println("           ※省略した場合は10028番で起動する");
                    System.out.println("           [指定例]");
                    System.out.println("            -bindport 10222");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -httpserver:HTTPサーバモードの起動指定");
                    System.out.println("             HTTPサーバで起動した場合はデフォルト8080番ポートでHTTPプロトコルでAdapter入力を待ち受ける");
                    System.out.println("             待ち受けるコンテキストは指定なくルート直下全てが対象となる。つまり「http://setsunaexample.org/」のように指定することになる");
                    System.out.println("             待ち受けるコンテキストを指定したい場合は-httpcontextオプションを利用する");
                    System.out.println("             HTTP通信で1リクエスト1データとして入力が可能");
                    System.out.println("             ・入力のHTTPパラメータのフォーマットはKey=Valueとする。このKeyの部分がSetsunaの内部DBのカラム名となり、");
                    System.out.println("               Valueの部分はデータとなる。-columnを指定している場合は、指定した名前でHTTPパラメータ内を探索するため、");
                    System.out.println("               指定したKeyが無ければパラメータが作れずエラーとなる。");
                    System.out.println("               Getでのリクエストを例にすると以下は4つのカラムデータを投入している");
                    System.out.println("               http://setsunaexample.org/?column1=XXX1&column2=20120501120000&column3=testdata1&column4=exampledata1");
                    System.out.println("             入力後、返却される値はHTTPステータスコードだけとなり、bodyの返却はない");
                    System.out.println("             ・HTTPステータスコードの対応は以下となる");
                    System.out.println("               200 : 入力成功");
                    System.out.println("               400 : 入力データが最初にSetsunaに投入された場合と入力パラメータ数が異なる。正しいからメータ数にすれば復旧可能");
                    System.out.println("               500 : Setsuna側でなんだかのサーバエラーが発生している。クライアントによる復旧不可");
                    System.out.println("             -serverと同時に指定すると-serverで起動するMessagePack-RPCでのサーバが優先されこちらは起動しない");
                    System.out.println("             本モードで起動した場合に-query等で利用するテーブル名は-streamを指定しない場合、'server'というテーブル名になる");
                    System.out.println("             **省略可能**");
                    System.out.println("             ※省略した場合はパイプ入力となる");
                    System.out.println("             [指定例]");
                    System.out.println("               -httpserver true");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -httpbindaddr:HTTPサーバモードの起動時のサーバがバインドするアドレス");
                    System.out.println("               HTTPサーバモードで起動した場合のみ有効");
                    System.out.println("               **省略可能**");
                    System.out.println("               ※省略した場合は0.0.0.0にバインドされる");
                    System.out.println("               [指定例]");
                    System.out.println("                -httpbindaddr 192.168.1.1");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -httpbindport:HTTPサーバモードの起動時のサーバが待ち受けるポート番号");
                    System.out.println("               RHEL系のOSの場合、root権限意外では80番を指定することは出来ない場合があるため注意が必要である");
                    System.out.println("               HTTPサーバモードで起動した場合のみ有効");
                    System.out.println("               **省略可能**");
                    System.out.println("               ※省略した場合は8080番で起動する");
                    System.out.println("               [指定例]");
                    System.out.println("               -httpbindport 9090");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -httpcontext:HTTPサーバモードの起動時のコンテキストを限定したい場合に利用する");
                    System.out.println("              HTTPサーバモードで起動した場合のみ有効");
                    System.out.println("              **省略可能**");
                    System.out.println("              ※省略した場合は全てのコンテキストが1入力になる");
                    System.out.println("              [指定例]");
                    System.out.println("              -httpcontext setsuna");
                    System.out.println("               上記の場合の入力URLは以下となる");
                    System.out.println("               http://setsunaexample.org/setsuna");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -atime:標準入力・サーバモード入力から受取るデータが一時テーブル上に存在する有効期限を秒で指定。");
                    System.out.println("        **省略可能**");
                    System.out.println("        ※省略した場合は600秒");
                    System.out.println("        [指定例]");
                    System.out.println("          -atime 3600");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -column:標準入力・サーバモード入力から受取る情報をデータベースの一時テーブル情報にマッピング。");
                    System.out.println("         するためのカラム情報の定義を指定。カラム名を半角スペース区切りで定義する");
                    System.out.println("         **省略可能**");
                    System.out.println("         ※省略した場合はCOLUMN0、COLUMN1、・・・と自動定義される");
                    System.out.println("         [指定例]");
                    System.out.println("          -column \"DATETIME TYPE USERCPU SYSCPU NICR IOWAIT IDOLE\"");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -sep:標準入力から受取る情報を、カラム単位に分解するためのセパレータ文字列。");
                    System.out.println("      **省略可能**");
                    System.out.println("      ※省略した場合は\" \"となる");
                    System.out.println("      [指定例]");
                    System.out.println("       例1) -sep \",\"");
                    System.out.println("       例2) -sep \"=\"");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -sept:標準入力から受取る情報を、カラム情報として");
                    System.out.println("       マッピングするために分解するセパレータが2個以上続いた場合に1つとして扱う指定。"); 
                    System.out.println("       **省略可能**");
                    System.out.println("       [指定]");
                    System.out.println("        1 = なにもしない");
                    System.out.println("        2 = 2個以上セパレータが連続したな場合は1つとする(デフォルト)");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -dst:自身への標準入力データのレコードの区切りを指定。");
                    System.out.println("      [扱う区切りの指定]");
                    System.out.println("       1 = 改行(デフォルト)");
                    System.out.println("           CRLF or LF");
                    System.out.println("       2 = 時間");
                    System.out.println("           標準入力からのインプットから次のインプットまでの間が100ミリ秒以上ある場合は区切りとする指定");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -skiperror:カラム定義の合わない入力データがきた場合にExceptionを発行せずに無視する");
                    System.out.println("            **省略可能**");
                    System.out.println("            ※省略した場合は定義違いの場合Exception発行してSetsunaを停止");
                    System.out.println("            [指定例]");
                    System.out.println("             -skiperror true");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -offset:データ読み込み時の開始位置。");
                    System.out.println("         標準入力モード時や、サーバモード時に送り込まれたデータを指定されたデータ数");
                    System.out.println("         スキップ(読み込まない)してから始めて内部DBへの格納を開始する");
                    System.out.println("         利用イメージは読み込まれるデータの最初レコードに空白や、本来読み込みたいデータと異なる");
                    System.out.println("         フォーマットが存在した場合に利用する");
                    System.out.println("         このオプションを利用する場合は必ず-columnオプションも設定する必要がある");
                    System.out.println("         これはデータを格納するテーブル定義を作成する必要があるためである");
                    System.out.println("         **省略可能**");
                    System.out.println("         [設定例]");
                    System.out.println("          -offset 3 -column \"ip time method result\"");
                    System.out.println("          ※投入されたデータを3レコードスキップ後、-column指定のカラム定義で取り込む");
                    System.out.println("  ");
                    System.out.println("  ");
                    /*System.out.println(" -target:標準入力以外の別プロセスが入力しているStreamデータを監視したい場合に指定する");
                    System.out.println("         指定する値は監視したい入力を行っているプロセスのStream名(-streamの値)");
                    System.out.println("        **省略可能**");
                    System.out.println("        ※省略した場合は自身への標準入力を監視する");
                    System.out.println("        [指定例]");
                    System.out.println("          -target otherstream");
                    System.out.println("  ");*/
                    System.out.println(" -trigger:データの変化を検知するための第1要素");
                    System.out.println("            この指定が適応可能なデータは自プロセスのSetsunaMainへの標準入力のデータだけである。");
                    System.out.println("            この指定が適応される粒度は、標準入力からデータを-dst指定で区切った1インプット単位である");
                    System.out.println("          カラム名を指定してそれに対しての条件を記述する形式となる。");
                    System.out.println("            指定方法のフォーマットは[カラム名][条件][比べる値]");
                    System.out.println("            [条件]は4つ存在する'>' or '<' or 'like'(部分一致) or '='(完全一致)");
                    System.out.println("            条件の'<'や'>'は指定したカラムの値が数字ではない場合は、可能な限りに変換して比べます");
                    System.out.println("            例えば'10%'や'LEVEL=90'のような数値を持ちながらその他の文字も連結されているような場合は数値のみを抜き出して比べる");
                    System.out.println("          **省略可能**");
                    System.out.println("          ※省略した場合はマッチしたものとして処理");
                    System.out.println("          [指定例]");
                    System.out.println("           例1)-trigger \"COLUMN6 > 10\"");
                    System.out.println("           例2)-trigger \"COLUMN6 like ABCD\"");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -query:データの変化を検知するための第2要素");
                    System.out.println("        -triggerの指定がない場合または、");
                    System.out.println("        -trigger指定でマッチした場合に実行されるSQLクエリ。");
                    System.out.println("          この指定が適応可能なデータは1つのサーバ内で稼働する全てのSetsunaMainへの標準入力のデータである。");
                    System.out.println("          他のSetsunaMainへの標準入力は-streamで指定した名前でテーブルかされているので、SQL内でテーブル指定で利用可能である。");
                    System.out.println("          この指定が適応される粒度は、標準入力からデータを-dst指定で区切った1インプット単位に");
                    System.out.println("        -triggerが適応されそこでマッチした場合に実行される。実行対象となるテーブルデータは");
                    System.out.println("          過去に標準入力から渡されたデータの有効期限内のデータ全てである。");
                    System.out.println("        このSQLクエリの結果が1件でもあれば条件に完全にマッチしたとして、以降の-eventで");
                    System.out.println("          指定されたイベントが実行される。データが1件でもあればという部分に注意が必要である。");
                    System.out.println("          例えば'select count(*)'などはたとえ件数が0件でも0件という");
                    System.out.println("          件数が返されるためイベントが実行されてします。");
                    System.out.println("          通常の'select *'系であれば結果なしになるので、マッチするデータがなければイベントは実行されない。");
                    System.out.println("          もしcount系で0件かどうかを指定したい場合は'-count true'を指定することで、取得された件数を判断するようになる。");
                    System.out.println("        テーブル内には無条件でC_TIMEという登録時間をtimestamp型で持っているカラムと、PKEYIDXという登録順を表すBIGINTの値を必ず持っている");
                    System.out.println("          このそれぞれの項目をSQL内で利用可能");
                    System.out.println("        **省略可能**");
                    System.out.println("        ※省略した場合はマッチしたものとして処理");
                    System.out.println("  ");
                    System.out.println("        記述できる指定はSQLである。テーブル名や、カラム名は'-stream'や、'-column'で指定したものになる。");
                    System.out.println("          また両方を省略している場合は、それぞれのDefault値でテーブル、カラムが作成されている。");
                    System.out.println("        SQLの文法はH2Databaseに準拠している。また、H2Databaseには存在しない、以下の関数が利用可能である");
                    System.out.println("         [利用可能追加関数]");
                    System.out.println("          to_number(varchar)");
                    System.out.println("            ※上記のto_numberは数値が含まれている値の場合は可能な限り数値化を試みます。");
                    System.out.println("          to_char(number)");
                    System.out.println("         [SQLの指定例]");
                    System.out.println("          -query \"select * from (select avg(to_number(COLUMN10)) as avgld from pipe order by COLUMN1 desc limit 10)) t1 where t1.avgld > 2\"");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -count:-queryで指定したSQLがcount結果を返してくることを明示的に指定します");
                    System.out.println("        この指定をおこなった場合は結果が1件以上の場合にユーザスクリプトが実行されるようになる");
                    System.out.println("        [指定例]");
                    System.out.println("         -count true");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -easyquery:SQLを直接記述せずに関数を呼び出し、SQLでの確認と同等のことを行う。");
                    System.out.println("              -queryと同時指定は不可となる。");
                    System.out.println("              現在利用可能な関数とその機能は以下");
                    System.out.println("              ※特殊オプションとしてavg_over、avg_below、over_value、below_value、time_range_in_avg_over、time_range_in_avg_below、time_range_in_value_multi_existこれらの関数の");
                    System.out.println("                最後の引き数である'条件指定の値'部分は自由に特定のテーブルのカラムの値に置換して実行可能となっています。");
                    System.out.println("                これは常に最新の値が適応されて実行されるため、別のテーブルにパラメータを投入し続けておけば、関数にあたえる引き数を動的に");
                    System.out.println("                変動させれることになります。つまり'avg_over'関数であれば、あるカラムの平均値が別のテーブルのあるカラムの最新値よりも");
                    System.out.println("                大きければ、イベント実行のようなことが可能です。");
                    System.out.println("                指定方法は'テーブル名:カラム名'のフォーマットで引き数部分に指定します。最後の実行例の部分にも例があるので、合わせて");
                    System.out.println("                参照してください。");
                    System.out.println("              [関数一覧]");
                    System.out.println("              'avg_over':特定のテーブルの特定のカラムの全てのデータの平均が指定値以上か調べる");
                    System.out.println("                         オプションでの表記方= -easyquery 'avg_over(対象Table名, 対象Column名, 条件指定の値)'");
                    System.out.println("            ");
                    System.out.println("              'avg_below':特定のテーブルの特定のカラムの平均が指定値以下か調べる");
                    System.out.println("                         オプションでの表記方= -easyquery 'avg_below(対象Table名, 対象Column名, 条件指定の値)'");
                    System.out.println("            ");
                    System.out.println("              'over_value':特定のテーブルの特定のカラムの最大値が指定値以上か調べる");
                    System.out.println("                          オプションでの表記方= -easyquery 'over_value(対象Table名, 対象Column名, 条件指定の値)'");
                    System.out.println("            ");
                    System.out.println("              'below_value':特定のテーブルの特定のカラムの最小値が指定値以下か調べる");
                    System.out.println("                            オプションでの表記方= -easyquery 'below_value(対象Table名, 対象Column名, 条件指定の値)'");
                    System.out.println("            ");
                    System.out.println("              'avg_more_over':特定のテーブルの特定のカラムの平均の指定倍以上の値が存在するか調べる");
                    System.out.println("                              オプションでの表記方= -easyquery 'avg_more_over(対象Table名, 対象Column名, 条件指定の値)'");
                    System.out.println("            ");
                    System.out.println("              'last_time_avg_more_over':特定のテーブルの特定のカラムの直近特定秒以内のデータの平均値を特定倍以上超えるデータがあるか調べる");
                    System.out.println("                                        オプションでの表記方= -easyquery 'last_time_avg_more_over(対象Table名, 対象Column名, 直近の特定秒, 特定倍)'");
                    System.out.println("            ");
                    System.out.println("              'count_last_time_avg_more_over':上記の条件のデータが特定件数以上ある");
                    System.out.println("                                              オプションでの表記方= -easyquery 'count_last_time_avg_more_over(対象Table名, 対象Column名, 直近の特定秒, 特定倍, 特定件数)'");
                    System.out.println("            ");
                    System.out.println("              'time_range_in_avg_over':直近からの過去指定値秒間の特定のテーブルの特定カラムの平均値が指定値以上である");
                    System.out.println("                                       オプションでの表記方= -easyquery 'time_range_in_avg_over(対象Table名, 対象Column名, 直近から過去指定秒, 指定値)'");
                    System.out.println("            ");
                    System.out.println("              'time_range_in_avg_below':直近からの過去指定値秒間の特定のテーブルの特定カラムの平均値が指定値以下である");
                    System.out.println("                                        オプションでの表記方= -easyquery 'time_range_in_avg_below(対象Table名, 対象Column名, 直近から過去指定秒, 指定値)'");
                    System.out.println("            ");
                    System.out.println("              'time_range_in_value_multi_exist':特定のテーブルの特定のカラムの値が同じ値が直近からの過去指定秒間以内に指定値回登場する");
                    System.out.println("                                                オプションでの表記方= -easyquery 'time_range_in_value_multi_exist(対象Table名, 対象Column名, 直近から過去指定秒, 指定回数)'");
                    System.out.println("            ");
                    System.out.println("              'last_range_avg_over':特定のテーブルの特定のカラムの値の一定時間前の一定期間の平均値と現在も一定期間の平均値を比べ指定した倍数分現在の平均値が大きいかを判定");
                    System.out.println("                                    現在が2時だとした場合、24時間前の1時から2時までの間のあるカラムの値の平均値と現在の1時から2時までの間のあるカラムの値の平均値を比べて現在の方が24時間前の平均値の2倍になっていたらイベント実行などに使う");
                    System.out.println("                                    上記の場合、指定方法は以下のようになる。(ロードアベレージをtopコマンドで流し込んで-streamでloadaveragetableという名前にして、loadaverageというカラムを-columnで指定した想定場合の想定");
                    System.out.println("                                    last_range_avg_over(loadaveragetable, loadaverage, 3600, 86400, 2) <=先頭から-streamで指定した値、カラム名、1時～2時を表す3600秒、24時間前を表す86400秒、2倍を表す2)");
                    System.out.println("                                    オプションでの表記方= -easyquery 'last_range_avg_over(対象Table名, 対象Column名, 指定一定期間(秒/単位), 一定時間前(秒/単位), 指定倍)'");
                    System.out.println("            ");
                    System.out.println("              [指定例]");
                    System.out.println("               -easyquery avg_over(pipe, column4, 3) ");
                    System.out.println("               -easyquery avg_over(pipe, column4, parametertable:column1) <=パラメータに他のテーブルにあるカラムの値を用いた場合");
                    System.out.println("               ※内部で実際に実行されているSQLは-debug onで確認可能");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -event:イベントで実行するスクリプト(シェルやbatなど)を指定");
                    System.out.println("          フルパス指定を推奨");
                    System.out.println("        指定されたスクリプトは-triggerか-queryで指定した全てがマッチした場合に実行される");
                    System.out.println("        実行される粒度は標準入力から入ってくる1データ単位で-triggerと-queryがマッチした場合に");
                    System.out.println("          1回づつ、別プロセスで実行される。");
                    System.out.println("        また実行されるスクリプトの引数には、このイベントを実行するための基となった、標準入力からの");
                    System.out.println("          1入力レコードが渡される。シェルであれば'$1'変数等で取得出来ることになる。");
                    System.out.println("        [指定例]");
                    System.out.println("         -event /home/setsuna/DataWriteEvent.sh");
                    System.out.println("        [シェル実装例]");
                    System.out.println("         #!/bin/sh");
                    System.out.println("         echo $1 >> /var/tmp/event.log");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -eventquery:イベントを-event指定でのシェルやバッチではなく、任意のSQLを");
                    System.out.println("             実行させその結果をJSONで標準出力に出力したい場合はこのオプションにSQLを記述する。");
                    System.out.println("             [指定例]");
                    System.out.println("              -eventquery \"select * from pile\"");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -argtype:-eventで指定したユーザイベントへの引数のフォーマットを指定");
                    System.out.println("          [指定]");
                    System.out.println("           JSON");
                    System.out.println("           CSV");
                    System.out.println("          [指定例]");
                    System.out.println("           -argtype JSON");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -errorlog:Setsuna自身のエラー出力をコンソールではなく、指定したファイルに出力する");
                    System.out.println("           出力先のファイル名を指定する");
                    System.out.println("          [指定例]");
                    System.out.println("           -errorlog \"setsuna_error.log\"");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -debug:Inputデータ文字列、実行した-trigger、実行したSQL、実行したユーザイベントコマンドを標準出力に出力する");
                    System.out.println("        指定出来る種類は2種類あり、onとonlyである。");
                    System.out.println("        on=デバッグ出力とSetsunaそのもののイベントの出力も混在して出力される");
                    System.out.println("        only=デバッグ出力のみ出力。Setsunaそのもののイベントの出力はでない");
                    System.out.println("        省略時はdebugなし");
                    System.out.println("        [指定例]");
                    System.out.println("         -debug on");
                    System.out.println("         -debug only");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -masterport:スタンドアロンモードで起動する場合、マスターサーバとして起動するためのチェックに利用するポート番号");
                    System.out.println("             ここで指定したポート番号で起動してるサーバがいなければ、マスターとなる。");
                    System.out.println("             注意!!:この指定をおこなう場合は同じサーバ内で起動するSetsunaは全て同じ設定を適応する必要がある。");
                    System.out.println("             数値で指定する");
                    System.out.println("             **省略可能**");
                    System.out.println("             ※省略した場合は10027となる");
                    System.out.println("             [指定例]");
                    System.out.println("             -masterport 9999");
                    System.out.println("  ");
                    System.out.println("  ");
                    System.out.println(" -dbport:スタンドアロンモードで起動する場合の内部DBのポート番号");
                    System.out.println("         数値で指定する");
                    System.out.println("         注意!!:この指定をおこなう場合は同じサーバ内で起動するSetsunaは全て同じ設定を適応する必要がある。");
                    System.out.println("         **省略可能**");
                    System.out.println("         ※省略した場合は9092となる");
                    System.out.println("         [指定例]");
                    System.out.println("         -dbport 9999");
                    System.out.println("  ");
                    System.exit(0);
                }
            }


            SetsunaMain main = new SetsunaMain(args);
            main.startSetsuna();
        } catch (Throwable te) {
            te.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }


    public void startSetsuna() throws Exception {

        try {
            // エラーログ設定
            if (SetsunaStaticConfig.DEFAULT_SETSUNA_ERROR_LOG != null)
                System.setErr(new PrintStream(SetsunaStaticConfig.DEFAULT_SETSUNA_ERROR_LOG));

            if (SetsunaStaticConfig.DATA_INPUT_OFFSET != 0) {
                if (SetsunaStaticConfig.DEFAULT_PIPEINPUT_COLUMN_LIST == null) {
                    throw new IllegalArgumentException("Please, use -column option");
                }
            }

            // ローカルモードで起動した際はtrueとなる
            SetsunaStaticConfig.SETSUNA_LOCAL_MODE = true;
            ServerSocket serverSocket = null;
            Server server = null;

            // ローカルモード時にどれかがサーバになるために、すでにサーバがいないかチェック
            try {
                // サーバソケットを生成してバインドに成功すれば親となる
                InetSocketAddress bindAddress = null;
                bindAddress = new InetSocketAddress("127.0.0.1", SetsunaStaticConfig.DEFAULT_MASTER_CHECK_PORT);
                serverSocket = new ServerSocket();
                serverSocket.bind(bindAddress, 100);
                SetsunaStaticConfig.SETSUNA_LOCAL_SERVER = true;
                // DBサーバを起動/マスターでのみ起動
                String[] dbServerParam = new String[2];
                dbServerParam[0] = "-tcpPort";
                dbServerParam[1] = new Integer(SetsunaStaticConfig.DEFAULT_DB_PORT).toString();
                server = Server.createTcpServer(dbServerParam).start();

                SetsunaStaticConfig.STREAM_DATABASE_URI = SetsunaStaticConfig.STREAM_DATABASE_LOCAL_SERVER_URI;

            } catch(Exception chkE) {

                // 既に親が存在する
                SetsunaStaticConfig.SETSUNA_LOCAL_SERVER = false;
                SetsunaStaticConfig.STREAM_DATABASE_URI = SetsunaStaticConfig.STREAM_DATABASE_LOCAL_CLIENT_URI;
            }
        
            // コアをインスタンス化
            this.setsunaCore = new SetsunaCore();

            // それぞの要素を呼び出し
            AbstractCoreEngine coreAdapterEngine = null;
            if (SetsunaStaticConfig.DEFAULT_EASY_SERVER_MODE) {
            
                coreAdapterEngine = this.startEasyServerAdapter();
            } else if (SetsunaStaticConfig.DEFAULT_HTTP_SERVER_MODE) {
            
                coreAdapterEngine = this.startHttpServerAdapter();
            } else {
                coreAdapterEngine = this.startPipeAdapter();
            }
            AbstractCoreEngine coreQueryEngine = this.startQuery();
            AbstractCoreEngine coreUserEventEngine = this.startUserEvent();

            if (coreAdapterEngine != null) {
                coreAdapterEngine.join();
            } else {
                coreQueryEngine.join();
            }
        } catch (Exception e) {
            throw e;
        }
    }


    private AbstractCoreEngine startPipeAdapter() throws Exception {
        // Setsunaに対するAdapterを開始
        AbstractCoreEngine coreAdapterEngine = null;
        IAdapter defaultPipeAdapter = null;

        try {
            // 標準入力を監視するかの判定
            defaultPipeAdapter = new DefaultPipeInputAdapter(System.in, 
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_COLUMN_LIST, 
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_SEP, 
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_SEP_TYPE, 
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_SEP_TYPE, 
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_SEP_STR,
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_ARRIVAL_TIME);
            // 監視するAdapter名を代入(ここでは標準の設定を利用する)
            SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TARGET = SetsunaStaticConfig.DEFAULT_PIPEINPUT_TABLE_NAME;
            // 標準のパイプラインアダプターを利用する
            coreAdapterEngine = setsunaCore.executeAdapterEngine(defaultPipeAdapter);
        } catch (Exception e) {
            throw e;
        }
        return coreAdapterEngine;
    }


    private AbstractCoreEngine startEasyServerAdapter() throws Exception {
        // Setsunaに対するMsgPack-RPC-Adapterを開始
        AbstractCoreEngine coreAdapterEngine = null;
        IAdapter easyServerAdapter = null;

        try {
            // Server型の入力を監視する
            easyServerAdapter = new DefaultEasyServerAdapter(SetsunaStaticConfig.DEFAULT_EASY_SERVER_BIND_ADDRESS,
                                                             SetsunaStaticConfig.DEFAULT_EASY_SERVER_BIND_PORT,
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_COLUMN_LIST,
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_ARRIVAL_TIME);
            // 監視するAdapter名を代入(ここでは標準の設定を利用する)
            SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TARGET = SetsunaStaticConfig.DEFAULT_EASY_SERVER_TABLE_NAME;
            // 標準のパイプラインアダプターを利用する
            coreAdapterEngine = setsunaCore.executeAdapterEngine(easyServerAdapter);
        } catch (Exception e) {
            throw e;
        }
        return coreAdapterEngine;
    }

    private AbstractCoreEngine startHttpServerAdapter() throws Exception {
        // Setsunaに対するHTTP-Adapterを開始
        AbstractCoreEngine coreAdapterEngine = null;
        IAdapter httpServerAdapter = null;

        try {
            // Server型の入力を監視する
            httpServerAdapter = new DefaultHttpServerAdapter(SetsunaStaticConfig.DEFAULT_HTTP_SERVER_BIND_ADDRESS,
                                                             SetsunaStaticConfig.DEFAULT_HTTP_SERVER_BIND_PORT,
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_COLUMN_LIST,
                                                             SetsunaStaticConfig.DEFAULT_PIPEINPUT_DATA_ARRIVAL_TIME,
                                                             SetsunaStaticConfig.DEFAULT_HTTP_SERVER_CONTEXT);
            // 監視するAdapter名を代入(ここでは標準の設定を利用する)
            SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TARGET = SetsunaStaticConfig.DEFAULT_HTTP_SERVER_TABLE_NAME;
            coreAdapterEngine = setsunaCore.executeAdapterEngine(httpServerAdapter);
        } catch (Exception e) {
            throw e;
        }
        return coreAdapterEngine;
    }


    private AbstractCoreEngine startQuery() throws Exception {

        // 引数で渡されたCauseとConditionを基にQueryを作る
        CauseContainer causeContainer = null;
        ConditionContainer conditionContainer = null;
        AbstractCoreEngine coreQueryEngine = null;
        try {
            if(SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CAUSE != null || SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CONDITION  != null || SetsunaStaticConfig.DEFAULT_PIPEINPUT_EASY_QUERY_CONDITION  != null) {
                if (SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CAUSE != null) {
                    causeContainer = new CauseContainer();
                    for (int idx = 0; idx < SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CAUSE.length; idx++) {
                        causeContainer.add2BuildCause(SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CAUSE[idx]);
                    }
                }

                if (SetsunaStaticConfig.DEFAULT_PIPEINPUT_EASY_QUERY_CONDITION == null && SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CONDITION != null) {
                    conditionContainer = new ConditionContainer(SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_CONDITION);
                }

                if (SetsunaStaticConfig.DEFAULT_PIPEINPUT_EASY_QUERY_CONDITION != null) {

                    List easyQueryList = new ArrayList();
                    for (int idx = 0; idx < SetsunaStaticConfig.DEFAULT_PIPEINPUT_EASY_QUERY_CONDITION.length; idx++) {
                        String[] esayQuerySQL = ConditionContainer.parseEsayQuery(SetsunaStaticConfig.DEFAULT_PIPEINPUT_EASY_QUERY_CONDITION[idx]);

                        for (int multiEasyIdx = 0; multiEasyIdx < esayQuerySQL.length; multiEasyIdx++) {
                            easyQueryList.add(esayQuerySQL[multiEasyIdx]);
                        }
                    }

                    String[] multiEasyQuery = new String[easyQueryList.size()];
                    for (int idx = 0; idx < easyQueryList.size(); idx++) {
                        multiEasyQuery[idx] = (String)easyQueryList.get(idx);
                    }
                    conditionContainer = new ConditionContainer(multiEasyQuery);
                }
            }

            // クエリーを実行(ここで指定したSetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TARGETがQueryがQueueからデータを取得するさいに利用される
            coreQueryEngine = setsunaCore.executeQueryEngine("PipedQuery", SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TARGET, causeContainer, conditionContainer);

        } catch (Exception e) {
            throw e;
        }
        return coreQueryEngine;
    }

    private AbstractCoreEngine startUserEvent() throws Exception {
        AbstractCoreEngine coreScriptEngine = null;
        try {
            // UserEventを実行
            // 標準ではコマンドを受付る。指定されたコマンドに取得したデータを引数として付けた形式で実行される
            // コマンドが起動引数で指定されない場合は、echoのスクリプトが動き標準出力にデータが出力される
            if (SetsunaStaticConfig.DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_QUERY_EVENT == true) {

                EventContainer eventContainer = new EventContainer(EventContainer.SCRIPT_TYPE_JAVA, new QueryExecuteScript(SetsunaStaticConfig.DEFAULT_PIPEINPUT_DEFAULT_EXECUTION_QUERY_EVENT_QUERY), "Query_Event");
                coreScriptEngine = setsunaCore.executeEventEngine(eventContainer, "PipedQuery");
            } else if (SetsunaStaticConfig.DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT == null) {

                EventContainer eventContainer = new EventContainer(EventContainer.SCRIPT_TYPE_JAVA, new EchoScript(), "Echo_Event");
                coreScriptEngine = setsunaCore.executeEventEngine(eventContainer, "PipedQuery");
            } else {


                EventContainer eventContainer = new EventContainer(EventContainer.SCRIPT_TYPE_JAVA, new DefaultScriptExecutionEventScript(SetsunaStaticConfig.DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT, SetsunaStaticConfig.DEFAILT_PIPEINPUT_USER_EVENT_SCRIPT_ARGTYPE), "UserScript_Event");
                coreScriptEngine = setsunaCore.executeEventEngine(eventContainer, "PipedQuery");
            }
        } catch (Exception e) {
            throw e;
        }
        return coreScriptEngine;
    }
}