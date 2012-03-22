package setsuna.core.util;

import java.util.*;
import java.sql.*;
import java.util.concurrent.locks.*;


/**
 * Setsuna用のDB処理集約クラス.<br>
 * テーブル作成や関数作成、Drop、データインサート、削除など<br>
 *
 * @author T.Okuyama
 */
public class StreamDbUtil {

    public static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public static final Lock r = rwl.readLock();
    public static final Lock w = rwl.writeLock();

    private static String[] tableCreateTpl = {"CREATE TABLE ", "%TABLE-NAME%", " (PKEYIDX BIGINT, ", "%COLUMN-DEF%", " )"};
    private static String[] indexCreateTpl = {"CREATE INDEX INDEX_", "", " ON ", "" , "(", "", ")"};
    private static String[] insertSqlTpl = {"INSERT INTO ", "", " Values(?", "" , ",?)"};
    
    private static String createToNumberAlias = "";
    private static String dropToNumberAlias = "DROP ALIAS TO_NUMBER";
//    private static String createToCharAlias = "CREATE ALIAS TO_CHAR FOR \"setsuna.core.util.StreamDbUtil.toChar\"";
    private static String createToCharAlias = "";
    private static String dropToCharAlias = "DROP ALIAS TO_CHAR";


    static {

        if(SetsunaStaticConfig.SETSUNA_LOCAL_SERVER == true) {
            initUserDBFunction();
        }
    }

    public static boolean initUserDBFunction () {
        boolean ret = true;
        Connection conn = null;
        try {

//            createToNumberAlias = "CREATE ALIAS TO_NUMBER AS $$ @CODE double toNumber(String obj) { return Double.parseDouble(obj);}$$;";
            createToNumberAlias = "CREATE ALIAS TO_NUMBER AS $$ @CODE  double toNumber(String value) {char[] charSep = value.toCharArray();StringBuilder createStr = new StringBuilder();boolean appendFlg = false;for (int i= 0; i < charSep.length; i++) {  if((charSep[i] == '+' && appendFlg == false) || (charSep[i] == '-' && appendFlg == false) || charSep[i] == '0' || charSep[i] == '1'  || charSep[i] == '2'  || charSep[i] == '3'  || charSep[i] == '4'  || charSep[i] == '5'  || charSep[i] == '6'  || charSep[i] == '7'  || charSep[i] == '8'  || charSep[i] == '9'  || charSep[i] == '0'  || charSep[i] == '１'  || charSep[i] == '２' || charSep[i] == '３'   || charSep[i] == '４'  || charSep[i] == '５'  || charSep[i] == '６'  || charSep[i] == '７'  || charSep[i] == '８'  || charSep[i] == '９' || charSep[i] == '0') {   if (charSep[i] == '+') createStr.append(\"+\");if (charSep[i] == '-') createStr.append(\"-\");if(charSep[i] == '1') createStr.append(\"1\");if(charSep[i] == '2') createStr.append(\"2\");if(charSep[i] == '3') createStr.append(\"3\");if(charSep[i] == '4') createStr.append(\"4\");if(charSep[i] == '5') createStr.append(\"5\");if(charSep[i] == '6') createStr.append(\"6\");if(charSep[i] == '7') createStr.append(\"7\");if(charSep[i] == '8') createStr.append(\"8\");if(charSep[i] == '9') createStr.append(\"9\");if(charSep[i] == '0') createStr.append(\"0\");if(charSep[i] == '１') createStr.append(\"1\");if(charSep[i] == '２') createStr.append(\"2\");if(charSep[i] == '３') createStr.append(\"3\");if(charSep[i] == '４') createStr.append(\"4\");if(charSep[i] == '５') createStr.append(\"5\");if(charSep[i] == '６') createStr.append(\"6\");if(charSep[i] == '７') createStr.append(\"7\");if(charSep[i] == '８') createStr.append(\"8\");if(charSep[i] == '９') createStr.append(\"9\");if(charSep[i] == '０') createStr.append(\"0\"); appendFlg = true;  } else if (appendFlg == true) {    break;  }}String tmpCnvCVal = createStr.toString();if (tmpCnvCVal.length() > 0) value = tmpCnvCVal;double valueDouble = Double.parseDouble(value); return valueDouble;}$$;";
            createToCharAlias = "CREATE ALIAS TO_CHAR AS $$ @CODE String toChar(Object obj) { return obj.toString();}$$;";
            PreparedStatement preparedStatement = null;
            int upRet = 0;

            conn = StreamDatabaseConnectManager.getConnection(false);

            try {

                preparedStatement = conn.prepareStatement(dropToNumberAlias);
                upRet = preparedStatement.executeUpdate();
                preparedStatement.close();
            } catch (Exception ee1) {
                preparedStatement.close();
                conn.rollback();
                conn.close();

                conn = StreamDatabaseConnectManager.getConnection(false);
            }
            preparedStatement = conn.prepareStatement(createToNumberAlias);
            upRet = preparedStatement.executeUpdate();
            preparedStatement.close();

            try {
                preparedStatement = conn.prepareStatement(dropToCharAlias);
                upRet = preparedStatement.executeUpdate();
                preparedStatement.close();
            } catch (Exception ee2) {
                preparedStatement.close();
                conn.rollback();
                conn.close();

                conn = StreamDatabaseConnectManager.getConnection(false);
            }
            preparedStatement = conn.prepareStatement(createToCharAlias);
            upRet = preparedStatement.executeUpdate();
            preparedStatement.close();
            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
        return ret;
    }


    public static boolean createTable(String tableName, String[] columnList) throws Exception {
        Connection conn = null;
        try {

            StringBuilder columnBuf = new StringBuilder(100);
            StringBuilder creataQueryBuf = new StringBuilder(200);

            for (int idx = 0; idx < columnList.length; idx++) {
                columnBuf.append(columnList[idx]);
                columnBuf.append(" VARCHAR(2000), ");
            }
            columnBuf.append("C_TIME TIMESTAMP , PRIMARY KEY(PKEYIDX,C_TIME)");
            creataQueryBuf.append(tableCreateTpl[0]);
            creataQueryBuf.append(tableName);
            creataQueryBuf.append(tableCreateTpl[2]);
            creataQueryBuf.append(columnBuf.toString());
            creataQueryBuf.append(tableCreateTpl[4]);


            conn = StreamDatabaseConnectManager.getConnection(false);

            PreparedStatement preparedStatement = conn.prepareStatement(creataQueryBuf.toString());

            // テーブル再作成時は自動的に削除される
            try {
                dropTable(tableName);
            } catch (Exception ee) {}

            // テーブル新規作成
            int ret = preparedStatement.executeUpdate();

            if (ret == 0) {
                conn.commit();
                preparedStatement.close();
                // 暗黙の登録時間に対するIndex作成
                preparedStatement = conn.prepareStatement(indexCreateTpl[0] + 
                                                         tableName + "_C_TIME" + 
                                                         indexCreateTpl[2] + 
                                                         tableName + 
                                                         indexCreateTpl[4] + 
                                                         "C_TIME" + 
                                                         indexCreateTpl[6]);
                preparedStatement.executeUpdate();
                preparedStatement.close();


                // その他カラムに対するIndex作成
                for (int idx = 0; idx < columnList.length; idx++) {

                    preparedStatement = conn.prepareStatement(indexCreateTpl[0] + 
                                                             tableName +  "_" + columnList[idx] + 
                                                             indexCreateTpl[2] + 
                                                             tableName + 
                                                             indexCreateTpl[4] + 
                                                             columnList[idx] + 
                                                             indexCreateTpl[6]);
                    preparedStatement.executeUpdate();
                    preparedStatement.close();
                }

                conn.commit();
            } else {
                conn.rollback();
                preparedStatement.close();
                return false;
            }
        
        } catch(Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch(Exception ee) {}
        }
        return true;
    }



    public static boolean dropTable(String tableName) throws Exception {
        Connection conn = null;
        try {

            conn = StreamDatabaseConnectManager.getConnection(false);
            PreparedStatement preparedStatement = conn.prepareStatement("DROP TABLE " + tableName);
            int ret = preparedStatement.executeUpdate();
            if (ret != 1)  {
                conn.rollback();
                return false;
            }
            conn.commit();
        } catch(Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch(Exception ee) {}
        }
        return true;
    }


    public static PreparedStatement createInsertPrepareStatement(Connection conn, String tableName, String[] dataColumnNames) throws Exception {
        PreparedStatement preparedStatement = null;
        try {
            StringBuilder insertQueryBuf = new StringBuilder();

            insertQueryBuf.append(insertSqlTpl[0]);
            insertQueryBuf.append(tableName);
            insertQueryBuf.append("(PKEYIDX");
            for (int idx = 0; idx < dataColumnNames.length; idx++) {
                insertQueryBuf.append(",");
                insertQueryBuf.append(dataColumnNames[idx]);
            }
            insertQueryBuf.append(",C_TIME) ");

            insertQueryBuf.append(insertSqlTpl[2]);
            for (int idx = 0; idx < dataColumnNames.length; idx++) {
                insertQueryBuf.append(",");
                insertQueryBuf.append("?");
            }
            insertQueryBuf.append(insertSqlTpl[4]);
            preparedStatement = conn.prepareStatement(insertQueryBuf.toString()); 
        } catch (Exception e) {
            throw e;
        }
        return preparedStatement;
    }


    public static boolean insertStreamData(PreparedStatement preparedStatement, String[] dataColumnNames, Map readData, long pkeyIdx) throws Exception {
        int ret = 0;
        try {
            int mappingParamIdx = 2;
            preparedStatement.setLong(1, pkeyIdx); //暗黙のPKey値
            for (int idx = 0; idx < dataColumnNames.length; idx++) {
                String mappingString = (String)readData.get(dataColumnNames[idx]);
                if (mappingString == null) {
                    preparedStatement.setNull(mappingParamIdx, Types.NULL);
                } else {
                    preparedStatement.setString(mappingParamIdx, mappingString);
                }

                mappingParamIdx++;
            }
            preparedStatement.setTimestamp(mappingParamIdx, new Timestamp(System.currentTimeMillis())); //暗黙の登録時間

            ret = preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw e;
        }
        return ret == 1 ? true : false;
    }


    public static void deleteStreamData(String query, long param) throws Exception {
        Connection conn = null;
        PreparedStatement preparedStatement = null;

        try {

            conn = StreamDatabaseConnectManager.getConnection(false);
            preparedStatement = conn.prepareStatement(query);
            preparedStatement.setTimestamp(1, new Timestamp(param));

            preparedStatement.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
            }
        }
    }


    public static double toNumber(String obj) throws SQLException {
        double ret = 0.0;
        try {
            ret = Double.parseDouble(obj);
        } catch (Exception e) {
            throw new SQLException("To number not support type");
        }
        return ret;
    }

    public static String toChar(Object obj) throws SQLException {
        String ret = "";
        try {
            ret = obj.toString();
        } catch (Exception e) {
            throw new SQLException("To char not support type");
        }
        return ret;
    }

}