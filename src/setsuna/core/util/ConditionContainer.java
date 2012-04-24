package setsuna.core.util;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.*;

/**
 * Query指定のコンテナクラス<br>
 *
 * @author T.Okuyama 
 */
public class ConditionContainer {


    protected String[] queryList = null;

    protected Connection conn = null;

    protected PreparedStatement[] preparedStatementList = null;

    protected boolean replaceQueryParamFlg = false;


    public ConditionContainer(String query) throws Exception {
        try {
            this.queryList = new String[1];
             this.queryList[0] = query;
        } catch (Exception e) {
            throw e;
        }
    }


    public ConditionContainer(String[] queryList) throws Exception {
        try {
            this.queryList = queryList;
        } catch (Exception e) {
            throw e;
        }
    }

    public void buildStreamDatabaseStatement(Connection conn) throws Exception {
        this.conn = conn;
        try {
            this.preparedStatementList = new PreparedStatement[this.queryList.length];

            for (int idx = 0; idx < this.queryList.length; idx++) {

                // SQLにAdapterからの値置換の指定が入っているか確認
                // 指定は"%カラム名%"というフォーマットになる
                Pattern p = Pattern.compile("%.*%");
                Matcher m = p.matcher(this.queryList[idx]);
                if (m.find()) {

                    // カラム指定あり
                    // カラム指定ありの場合はPrepareStatementを作らない
                    this.replaceQueryParamFlg = true;
                } else {

                    // カラム指定なし
                    this.preparedStatementList[idx] = this.conn.prepareStatement(this.queryList[idx]);
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }


    // Queryを実行してマッチするデータを探す
    // 全てのクエリの結果が1件以上存在する場合にtrueを返す
    public boolean checkConditionMatchRecode(Map adapterData) throws Exception {
        boolean ret = true;


        try {
            /*for (int idx = 0; idx < mappingParamList.size(); idx++) {
                this.preparedStatement.setObject(idx+1, mappingParamList.get(idx));
            }*/


            for (int idx = 0; idx < this.preparedStatementList.length; idx++) {
                if (!ret) break; // 既にマッチしていない

                ResultSet resultSet = null;
                PreparedStatement pst = null;
                // SQLにAdapterデータ埋め込みの場合はここでクエリ作成
                if (this.replaceQueryParamFlg) {

                    String cnvSql = this.adapterDataMargeQuery(this.queryList[idx], adapterData);
                    SystemUtil.debug("-query SQL=[" + cnvSql + "]");
                    pst = this.conn.prepareStatement(cnvSql);
                    resultSet = pst.executeQuery();
                    long end = System.nanoTime();

                } else {

                    SystemUtil.debug("-query SQL=[" + this.queryList[idx] + "]");
                    resultSet = this.preparedStatementList[idx].executeQuery();
                }

                if (SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TYPE == 1) {
                    if (!resultSet.next()) {
                        ret = false;
                    }
                } else if(SetsunaStaticConfig.DEFAULT_PIPEINPUT_QUERY_TYPE == 2) {
                    if (!resultSet.next()) {
                        ret = false;
                    } else {
                        int count = resultSet.getInt(1);
                        if (count < 1) return false;
                    }
                }
                resultSet.close();
                if (this.replaceQueryParamFlg) pst.close();
            }
        } catch (Exception e) {
            throw e;
        }
        return ret;
    }


    public void clear() {
        try {
            for (int idx = 0; idx < this.preparedStatementList.length; idx++) {
                this.preparedStatementList[idx].close();
            }
            if (this.conn != null) this.conn.close();
        } catch (Exception e) {

            this.conn = null;
            for (int idx = 0; idx < this.preparedStatementList.length; idx++) {
                this.preparedStatementList[idx] = null;
            }
        }
    }


    // '%カラム名%'をMap内のKeyからその部分をValueに置き換える
    private String adapterDataMargeQuery(String targetSql, Map adapterData) {
        String retSql = targetSql;

        Iterator it = adapterData.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();
            retSql = Pattern.compile("%"+key+"%", Pattern.CASE_INSENSITIVE).matcher(retSql).replaceAll("'"+value+"'");
        }
        return retSql;
    }


    /**
     * 簡易型クエリーのパーサー.<br>
     * 内部ではEasyQueryCompilerを呼び出していいるのみ.<br>
     *
     * @param queryString Function指定構文
     * @return 変換後のSQL文(複数の場合もあるので、配列。先頭要素から全て実行後、全て結果有だと合致したことになる)
     * @throw Exception SQLの変換に失敗。もしくは定義になりFunctionを指定した場合
     */
    public static String[] parseEsayQuery(String queryString) throws Exception {
        return EasyQueryCompiler.compileFunction2Sql(queryString);
    }
}
