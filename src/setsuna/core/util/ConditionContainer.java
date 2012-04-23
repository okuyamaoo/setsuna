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
     * 引数の各こうもくは全てコーテーション及び、ダブルコーテーションなしの値を指定
     * "avg_over":特定のカラムの平均が指定値以上か調べる(table, column, overvalue)
     * "avg_below":特定のカラムの平均が指定値以下か調べる(table, column, belowvalue)
     * "over_value":特定のカラムの最大値が指定値以上か調べる(table, column, overvalue)
     * "below_value":特定のカラムの最小値が指定値以下か調べる(table, column, belowvalue)
     * "avg_more_over":特定のカラムの平均の指定倍以上の値が存在するか調べる(table, column, multiple_number)
     * "last_time_avg_more_over":特定のテーブルの特定のカラムの直近特定秒以内のデータの平均値を特定倍以上超えるデータがあるか調べる(table, column, lasttime, multiple_number)
     * "count_last_time_avg_more_over":上記が特定件数以上ある(table, column, lasttime, multiple_number, count)
     * "time_range_in_avg_over":直近の指定値秒間の特定カラムの平均値が指定値以上である(table, column, rangetime, overvalue)
     * "time_range_in_avg_below":直近の指定値秒間の特定カラムの平均値が指定値以下である(table, column, rangetime, belowvalue)
     * "time_range_in_value_multi_exist":あるテーブルのあるカラムの値が同じ値が直近からの指定秒間以内に指定値回登場する(table, column, rangetime, over_value)
     *
     */
    public static String parseEsayQuery(String queryString) throws Exception {
        String sql = null;
        try {
            String convertTargetStr = queryString.trim().replaceAll(" ","").toLowerCase();

            int start = queryString.indexOf("(");
            int end = queryString.lastIndexOf(")");
            String queryParamStr = queryString.substring(start + 1, end);
            String[] params = queryParamStr.split(",");

            if (convertTargetStr.indexOf("avg_over(") == 0) {

                // table, column, overvalue
                sql = "select avg(to_number(" +  params[1] + ")) as avgval from " + params[0];
                sql = "select * from (" + sql + ") where avgval >  " + params[2];
            } else if (convertTargetStr.indexOf("avg_below(") == 0) {

                // table, column, belowvalue
                sql = "select avg(to_number(" +  params[1] + ")) as avgval from " + params[0];
                sql = "select * from (" + sql + ") where avgval <  " + params[2];
            } else if (convertTargetStr.indexOf("over_value(") == 0) {

                // table, column, overvalue
                sql = "select max(to_number(" +  params[1] + ")) as maxval from " + params[0];
                sql = "select * from (" + sql + ") where maxval >  " + params[2];
            } else if (convertTargetStr.indexOf("below_value(") == 0) {

                // table, column, belowvalue
                sql = "select min(to_number(" +  params[1] + ")) as minval from " + params[0];
                sql = "select * from (" + sql + ") where minval  < " + params[2];
            } else if (convertTargetStr.indexOf("avg_more_over(") == 0) {

                // table, column, multiple_number
                sql = "select * from (" + params[0]+ ") where to_number(" + params[1] + ") >= " + "(select (avg(to_number(" +  params[1] + ")) * " + params[2] + ") as val from " + params[0] + ")";
            } else if (convertTargetStr.indexOf("last_time_avg_more_over(") == 0) {

                // table, column, lasttime, multiple_number
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select * from (" + sql+ ") where to_number(" + params[1] + ") >= " + "(select (avg(to_number(" +  params[1] + ")) * " + params[3] + ") as val from " + params[0] + " where where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp))";
            } else if (convertTargetStr.indexOf("count_last_time_avg_more_over(") == 0) {

                // table, column, lasttime, multiple_number, count
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select count(" + params[1] + ") as cnt from (" + sql+ ") where to_number(" +  params[1] + ") >  " + "(select (avg(to_number(" +  params[1] + ")) * " + params[3] + ") as val from " + params[0] + " where where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp))";
                sql = "select * from (" + sql + ") where cnt >= " + params[4];
            } else if (convertTargetStr.indexOf("time_range_in_avg_over(") == 0) {

                // table, column, rangetime, overvalue
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select * from (" + sql+ ") where to_number(" + params[1] + ") >= " +  params[3];
            } else if (convertTargetStr.indexOf("time_range_in_avg_below(") == 0) {

                // table, column, rangetime, belowvalue
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select * from (" + sql+ ") where to_number(" + params[1] + ") <= " +  params[3];
            } else if (convertTargetStr.indexOf("time_range_in_value_multi_exist") == 0) {

                // table, column, rangetime, multiple_number
                sql = "select " + params[1] + " as val, count(" + params[1] + ") as cnt from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp) group by " + params[1];
                sql = "select * from (" + sql+ ") where cnt >= " +  params[3];
            } else {
                // Unknown pattern
                throw new Exception("Unknown -esayquery pattern");
            }
        } catch (Exception e) {
            throw e;
        }
        return sql;
    }
}
