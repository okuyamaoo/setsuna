
import java.util.*;
import java.io.*;
import java.sql.*;

import org.h2.tools.Server;
import org.h2.tools.*;
import org.h2.constant.ErrorCode;
import org.h2.constant.SysProperties;
import org.h2.message.DbException;
import org.h2.message.TraceSystem;
import org.h2.server.Service;
import org.h2.server.ShutdownHandler;
import org.h2.server.TcpServer;
import org.h2.server.pg.PgServer;
import org.h2.server.web.WebServer;
import org.h2.util.StringUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;


public class Test extends Thread{

    private Connection conn = null;
    private long addTime = 0L;

    public static volatile boolean endFlg = false;
    public static long testTime = 0L;
    public static long testTime1 = 0L;


    public Test(Connection conn, long addTime) {
        this.conn = conn;
        this.addTime = addTime;
    }


    public static void main(String[] args) {
        try {

            // start the TCP Server
            Server server = Server.createTcpServer().start();
            String insertSql = "insert into test1(id, col1, col2, col3) values(?, ?, ?, ?)";

            Class.forName("org.h2.Driver");

            Connection conn = getConnection();
            conn.setAutoCommit(false);


            PreparedStatement statement = null;

            statement = conn.prepareStatement("CREATE TABLE TEST1(ID INT PRIMARY KEY, COL1 VARCHAR(255), COL2 VARCHAR(255), COL3 BIGINT); CREATE INDEX searchR ON TEST1(COL3);CREATE INDEX searchX ON TEST1(COL1); CREATE VIEW TEST_VIEW AS SELECT * FROM TEST1 WHERE ID > 1000;");
            int ret = statement.executeUpdate();
            System.out.println("Create table ret =" + ret);
            conn.commit();
            statement.close();

            long start = System.nanoTime();
            statement = conn.prepareStatement(insertSql);
            for (int i = 0; i < 100000; i++) {
                statement.setInt(1, i);
                statement.setString(2, "AAAAAAAAAAAAA_" + i);
                statement.setString(3, "BBBBBBBBBBBBB_" + i);
                statement.setLong(4, System.nanoTime());
                statement.executeUpdate();
                if (i == 20000) testTime1 = System.nanoTime();
                if (i == 10000) testTime = System.nanoTime();
            }
            conn.commit();
            statement.close();
            conn.close();
            long end = System.nanoTime();
            System.out.println("100000 insert time =" + ((end - start) / 1000 / 1000));
            
            conn = getConnection();
            statement = conn.prepareStatement("SELECT CURRENT_TIME() as timeT, CURRENT_TIMESTAMP() AS timeS FROM TEST1 where col3 > ?");


            statement.setLong(1, testTime);
            ResultSet resultSet = statement.executeQuery();
            resultSet.close();

            start = System.nanoTime();
            for (int i = 0; i < 2; i++) {
                statement.setLong(1, testTime1+i);
                resultSet = statement.executeQuery();
                if(resultSet.next()) {
                    System.out.println("timeT=" + resultSet.getString(1) + " timeS" + resultSet.getString(2));
                }
                resultSet.close();
            }
            end = System.nanoTime();
            System.out.println("100 select Execution time=" + ((end - start) / 1000 / 1000) + "milli time");
            System.out.println("-----------------------------------------");
            conn.close();


            int max = 5;
            Test[] testList = new Test[max];
            for (int i = 0; i < max; i++) {
                testList[i] = new Test(getConnectionReadOnly(), 1000000*(i+1));
            }

            for (int i = 0; i < max; i++) {
                testList[i].start();
            }

            Thread.sleep(1000*59);
            Thread.sleep(1000);
            endFlg = true;
            for (int i = 0; i < max; i++) {
                testList[i].join();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public static Connection getConnection() {
        Connection conn = null;
        try {
            //conn = DriverManager.getConnection("jdbc:h2:mem:test;MULTI_THREADED=TRUE;AUTOCOMMIT=OFF;DB_CLOSE_DELAY=-1");
            conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mem:testdb;MULTI_THREADED=TRUE;AUTOCOMMIT=OFF;DB_CLOSE_DELAY=-1");
        } catch(Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static Connection getConnectionReadOnly() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mem:testdb;MULTI_THREADED=TRUE;AUTOCOMMIT=OFF;DB_CLOSE_DELAY=-1");
            conn.setReadOnly(true);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    public void run() {
        try {
            PreparedStatement statement = null;
            statement = this.conn.prepareStatement("SELECT ID AS CNT FROM TEST_VIEW where col3 > ? and col1 = ?");
            long start = System.nanoTime();
            int count = 0;
            int cntRet = 0;
            Random rnd = new Random();
            for (int i = 0; i < 10000000; i++) {
                if (Test.endFlg) break;
                count++;
                statement.setLong(1, testTime1 + this.addTime + i);
                statement.setString(2, "AAAAAAAAAAAAA_" + rnd.nextInt(100000));
                ResultSet resultSet = statement.executeQuery();
                if(resultSet.next()) {
                    cntRet++;
                }
                resultSet.close();
                statement.close();
                statement = this.conn.prepareStatement("SELECT ID AS CNT FROM TEST_VIEW where col3 > ? and col1 = ?");
            }
            long end = System.nanoTime();
            System.out.println(count + "\t select Execution time=" + ((end - start) / 1000 / 1000) + "milli time Count=" + cntRet);
            this.conn.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}