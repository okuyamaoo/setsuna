package setsuna.core.util;

import java.util.*;
import java.util.regex.*;

/**
 * -easyqueryで指定された関数指定を実際のSQLに変換するクラス<br>
 *
 * @author T.Okuyama 
 */
public class EasyQueryCompiler {


    /**
     * 簡易型クエリーのパーサー.<br>
     * 引数の各こうもくは全てコーテーション及び、ダブルコーテーションなしの値を指定
     * 時間の概念は全て秒単位で指定
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
     * "last_range_avg_over":あるテーブルのあるカラムの値の一定時間前の一定期間の平均値と現在同一一定期間の平均値を比べ指定した倍数分現在の平均値が大きいかを判定(table, column, rangetime, beforeseconde, multiple_number)
     *                       現在が2時だとした場合、24時間前の1時から2時までの間のあるカラムの値の平均値と現在の1時から2時までの間のあるカラムの値の平均値を比べて現在の方が24時間前の平均値の2倍になっていたらイベント実行などに使う
     *                       上記の場合、指定方法は以下のようになる
     *                       last_range_avg_over(loadaveragetable, loadaverage, 3600, 86400, 2) <=先頭から-streamで指定した値、カラム名、1時～2時を表す3600秒、24時間前を表す86400秒、2倍を表す2)
     * "last_range_avg_below":
     */
    public static String[] compileFunction2Sql(String queryString) throws Exception {
        List sqlList = new ArrayList();
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
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("avg_below(") == 0) {

                // table, column, belowvalue
                sql = "select avg(to_number(" +  params[1] + ")) as avgval from " + params[0];
                sql = "select * from (" + sql + ") where avgval <  " + params[2];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("over_value(") == 0) {

                // table, column, overvalue
                sql = "select max(to_number(" +  params[1] + ")) as maxval from " + params[0];
                sql = "select * from (" + sql + ") where maxval >  " + params[2];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("below_value(") == 0) {

                // table, column, belowvalue
                sql = "select min(to_number(" +  params[1] + ")) as minval from " + params[0];
                sql = "select * from (" + sql + ") where minval  < " + params[2];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("avg_more_over(") == 0) {

                // table, column, multiple_number
                sql = "select * from (" + params[0]+ ") where to_number(" + params[1] + ") >= " + "(select (avg(to_number(" +  params[1] + ")) * " + params[2] + ") as val from " + params[0] + ")";
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("last_time_avg_more_over(") == 0) {

                // table, column, lasttime, multiple_number
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select * from (" + sql+ ") where to_number(" + params[1] + ") >= " + "(select (avg(to_number(" +  params[1] + ")) * " + params[3] + ") as val from " + params[0] + " where where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp))";
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("count_last_time_avg_more_over(") == 0) {

                // table, column, lasttime, multiple_number, count
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select count(" + params[1] + ") as cnt from (" + sql+ ") where to_number(" +  params[1] + ") >  " + "(select (avg(to_number(" +  params[1] + ")) * " + params[3] + ") as val from " + params[0] + " where where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp))";
                sql = "select * from (" + sql + ") where cnt >= " + params[4];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("time_range_in_avg_over(") == 0) {

                // table, column, rangetime, overvalue
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select * from (" + sql+ ") where to_number(" + params[1] + ") >= " +  params[3];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("time_range_in_avg_below(") == 0) {

                // table, column, rangetime, belowvalue
                sql = "select " + params[1] + " from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp)";
                sql = "select * from (" + sql+ ") where to_number(" + params[1] + ") <= " +  params[3];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("time_range_in_value_multi_exist(") == 0) {

                // table, column, rangetime, multiple_number
                sql = "select " + params[1] + " as val, count(" + params[1] + ") as cnt from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2] + ", current_timestamp) group by " + params[1];
                sql = "select * from (" + sql+ ") where cnt >= " +  params[3];
                sqlList.add(sql);
            } else if (convertTargetStr.indexOf("last_range_avg_over(") == 0) {

                // table, column, rangetime, beforeseconde, multiple_number
                String beforeSql = "select avg(to_number(" + params[1] + ")) as beforeAvg from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + (Integer.parseInt(params[2].trim()) + Integer.parseInt(params[3].trim())) + ", current_timestamp) and C_TIME < DATEADD(SECOND, -" + Integer.parseInt(params[3].trim()) +  ", current_timestamp)";
                String nowSql = "select avg(to_number(" + params[1] + ")) as nowAvg from " + params[0] + " where C_TIME > DATEADD(SECOND, -" + params[2].trim() + ", current_timestamp)";
                sql = "select * from (" +  beforeSql + ") t1 where (t1.beforeAvg * " + params[4] + ") < (" + nowSql + ")";
                sqlList.add(sql);
            } else {
                // Unknown pattern
                throw new Exception("Unknown -esayquery pattern");
            }
        } catch (Exception e) {
            throw e;
        }
        String[] returnSql = new String[sqlList.size()];
        for (int i = 0; i < sqlList.size(); i++) {
            returnSql[i] = (String)sqlList.get(i);
        }
        return returnSql;
    }
}