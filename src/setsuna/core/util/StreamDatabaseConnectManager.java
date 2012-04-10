package setsuna.core.util;


import java.sql.*;


/**
 * Setsuna用のDBコネクション管理クラス<br>
 *
 * @author T.Okuyama
 */
public class StreamDatabaseConnectManager {

    static {
        try {
            Class.forName(SetsunaStaticConfig.STREAM_DATABASE_DRIVER_NAME);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public static Connection getConnection(boolean autoCommit) throws Exception {
        Connection conn = null;
        
        try {

            conn = DriverManager.getConnection(SetsunaStaticConfig.STREAM_DATABASE_URI);
        } catch(Exception e) {
            throw e;
        }
        return conn;
    }

    public static Connection getConnectionReadOnly() throws Exception {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(SetsunaStaticConfig.STREAM_DATABASE_URI);
            conn.setReadOnly(true);
        } catch(Exception e) {
            throw e;
        }
        return conn;
    }
}