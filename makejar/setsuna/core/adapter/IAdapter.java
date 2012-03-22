package setsuna.core.adapter;

import java.util.Map;

/** 
 * Adapterのインターフェース.<br>
 *
 * @author T.Okuyama
 */
public interface IAdapter {

    /**
     * Adapterの名前を返す.<br>
     * Adapterデータを格納するDB上のテーブル名としても利用される<br>
     * This method returns its name<br>
     *
     * @return String Adapter名(テーブル名)
     */
    public String getName();

    /**
     * このアダプターから取得したデータが有効な時間をミリ秒で返却する.<br>
     * 例えば10分であれば1000ミリ秒×60×10=600000となるので、
     * 返却値は600000となる。
     *
     * @return long 有効期限
     */
    public long getArrivalTime();

    /**
     * Adapterの返すデータのカラム情報を返す.<br>
     * Adapterデータを格納するDBテーブル上のColumn名や、Causeの条件指定時のColumn名に利用される<br>
     * This method returns the definition information on Column of the data to read
     * 
     * @return String[] カラム名
     */
    public String[] getDataColumnNames();

    // This method returns one read data at a time
    public Map read() throws Exception;

    /**
     * Adapterの停止が要求された場合に呼び出される
     * リソースの解放などを行う部分を実装
     */
    public boolean stop() ;
}