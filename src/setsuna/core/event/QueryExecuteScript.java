package setsuna.core.event;

import java.util.*;
import java.sql.*;

import setsuna.core.util.*;


/**
 * Setsunaの持つDBに対して指定されたSQLを実行しその結果を<br>
 * 標準出力に出力するSQLイベント用スクリプト.<br>
 *
 * @author T.Okuyama
 */
public class QueryExecuteScript implements IScript {

    protected String query = null;
    protected Connection conn = null;
    protected PreparedStatement preparedStatement = null;

    public QueryExecuteScript(String query) throws Exception {
        this.query = query;
        try {
            this.conn = StreamDatabaseConnectManager.getConnectionReadOnly();
            this.preparedStatement = this.conn.prepareStatement(this.query);
        } catch (Exception e) {
            throw e;
        }
    }

    public void execute(Map data) throws Exception {
        List retList = null;
        String[] columnNameList = null;
        StringBuilder retStrBuf = new StringBuilder();
        try {
            SystemUtil.debug("-eventquery=[" + this.query + "]");
            ResultSet resultSet = this.executeQuery(new ArrayList());
            ResultSetMetaData metaData = resultSet.getMetaData();
            columnNameList = new String[metaData.getColumnCount()];

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columnNameList[i-1] = metaData.getColumnName(i);
            }
            String sep = "";
            retStrBuf.append("[");
            while(resultSet.next()) {
                retStrBuf.append(sep);
                retStrBuf.append("{");

                String colSep = "";
                for (int i = 0; i < columnNameList.length; i++) {
                    retStrBuf.append(colSep);
                    retStrBuf.append("\"");
                    retStrBuf.append(columnNameList[i]);
                    retStrBuf.append("\"");
                    retStrBuf.append(":");
                    retStrBuf.append("\"");
                    retStrBuf.append(resultSet.getObject(columnNameList[i]));
                    retStrBuf.append("\"");
                    colSep = ",";
                }
                retStrBuf.append("}");
                sep = ",";
            }
            retStrBuf.append("]");
            resultSet.close();
            SystemUtil.printout(retStrBuf.toString());
        } catch (Exception e) {
            throw e;
        }
    }

    protected ResultSet executeQuery(List parameterList) throws Exception {
        ResultSet resultSet = null;
        try {

            for (int idx = 0; idx < parameterList.size(); idx++) {
                this.preparedStatement.setObject(idx+1, parameterList.get(idx));
            }
            resultSet = this.preparedStatement.executeQuery();
            return resultSet;
        } catch (Exception e) {
            throw e;
        }
    }
}