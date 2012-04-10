package setsuna.core.util;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Query指定のコンテナクラス<br>
 *
 * @author T.Okuyama 
 */
public class ConditionContainer {


    protected String[] queryList = null;

    protected Connection conn = null;

    protected PreparedStatement[] preparedStatementList = null;


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

                this.preparedStatementList[idx] = this.conn.prepareStatement(this.queryList[idx]);
            }
        } catch (Exception e) {
            throw e;
        }
    }


    // Queryを実行してマッチするデータを探す
    // 全てのクエリの結果が1件以上存在する場合にtrueを返す
    public boolean checkConditionMatchRecode() throws Exception {
        boolean ret = true;


        try {
            /*for (int idx = 0; idx < mappingParamList.size(); idx++) {
                this.preparedStatement.setObject(idx+1, mappingParamList.get(idx));
            }*/

            for (int idx = 0; idx < this.preparedStatementList.length; idx++) {
                SystemUtil.debug("-query SQL=[" + this.queryList[idx] + "]");
                ResultSet resultSet = this.preparedStatementList[idx].executeQuery();
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

                // table, column, overvalue
                sql = "select avg(to_number(" +  params[1] + ")) as avgval from " + params[0];
                sql = "select * from (" + sql + ") where avgval <  " + params[2];
            } else if (convertTargetStr.indexOf("over_value(") == 0) {

                // table, column, overvalue
                sql = "select max(to_number(" +  params[1] + ")) as maxval from " + params[0];
                sql = "select * from (" + sql + ") where maxval >  " + params[2];
            } else if (convertTargetStr.indexOf("below_value(") == 0) {

                // table, column, overvalue
                sql = "select min(to_number(" +  params[1] + ")) as minval from " + params[0];
                sql = "select * from (" + sql + ") where minval  < " + params[2];
            } else {
                // Unknown pattern
                throw new Exception("Unknown -esayquery pattern");                
            }
        } catch (Exception e) {
            throw e;
        }
        System.out.println(sql);
        return sql;
    }
}
