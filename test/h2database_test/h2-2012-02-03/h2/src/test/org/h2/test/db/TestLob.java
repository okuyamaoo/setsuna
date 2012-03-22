/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.h2.constant.ErrorCode;
import org.h2.constant.SysProperties;
import org.h2.jdbc.JdbcConnection;
import org.h2.store.FileLister;
import org.h2.store.fs.FileUtils;
import org.h2.test.TestBase;
import org.h2.tools.DeleteDbFiles;
import org.h2.util.Task;
import org.h2.util.Utils;
import org.h2.util.IOUtils;
import org.h2.util.StringUtils;
import org.h2.value.ValueLob;

/**
 * Tests LOB and CLOB data types.
 */
public class TestLob extends TestBase {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase test = TestBase.createCaller().init();
        test.config.big = true;
        test.test();
    }

    public void test() throws Exception {
        testLobSkip();
        testLobSkipPastEnd();
        testCreateIndexOnLob();
        testBlobInputStreamSeek(true);
        testBlobInputStreamSeek(false);
        testDeadlock();
        testCopyManyLobs();
        testCopyLob();
        testConcurrentCreate();
        testLobInLargeResult();
        testUniqueIndex();
        testConvert();
        testCreateAsSelect();
        testDropAllObjects();
        testDelete();
        testTempFilesDeleted(true);
        testTempFilesDeleted(false);
        testAddLobRestart();
        testLobServerMemory();
        if (config.memory) {
            return;
        }
        testLobUpdateMany();
        testLobDeleteTemp();
        testLobDelete();
        testLobVariable();
        testLobDrop();
        testLobNoClose();
        testLobTransactions(10);
        testLobTransactions(10000);
        testLobRollbackStop();
        testLobCopy();
        testLobHibernate();
        testLobCopy(false);
        testLobCopy(true);
        testLobCompression(false);
        testLobCompression(true);
        testManyLobs();
        testClob();
        testUpdateLob();
        testLobReconnect();
        testLob(false);
        testLob(true);
        testJavaObject();
        deleteDb("lob");
        FileUtils.deleteRecursive(TEMP_DIR, true);
    }

    private void testLobSkip() throws Exception {
        deleteDb("lob");
        Connection conn;
        conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.executeUpdate("create table test(x blob) as select secure_rand(1000)");
        ResultSet rs = stat.executeQuery("select * from test");
        rs.next();
        Blob b = rs.getBlob(1);
        byte[] test = b.getBytes(5 + 1, 1000 - 5);
        assertEquals(1000 - 5, test.length);
        stat.execute("drop table test");
        conn.close();
    }

    private void testLobSkipPastEnd() throws Exception {
        if (config.memory) {
            return;
        }
        deleteDb("lob");
        Connection conn;
        conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int, data blob)");
        byte[] data = new byte[150000];
        new Random(0).nextBytes(data);
        PreparedStatement prep = conn.prepareStatement("insert into test values(1, ?)");
        prep.setBytes(1, data);
        prep.execute();
        ResultSet rs = stat.executeQuery("select data from test");
        rs.next();
        for (int blockSize = 1; blockSize < 100000; blockSize *= 10) {
            for (int i = 0; i < data.length; i += 1000) {
                InputStream in = rs.getBinaryStream(1);
                in.skip(i);
                byte[] d2 = new byte[data.length];
                int l = Math.min(blockSize, d2.length - i);
                l = in.read(d2, i, l);
                if (i >= data.length) {
                    assertEquals(-1, l);
                } else if (i + blockSize >= data.length) {
                    assertEquals(data.length - i, l);
                }
                for (int j = i; j < blockSize && j < d2.length; j++) {
                    assertEquals(data[j], d2[j]);
                }
            }
        }
        stat.execute("drop table test");
        conn.close();
    }

    private void testCreateIndexOnLob() throws Exception {
        if (config.memory) {
            return;
        }
        deleteDb("lob");
        Connection conn;
        conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int, name clob)");
        assertThrows(ErrorCode.FEATURE_NOT_SUPPORTED_1, stat).
                execute("create index idx_n on test(name)");
        stat.execute("drop table test");
        conn.close();
    }

    private void testBlobInputStreamSeek(boolean upgraded) throws Exception {
        deleteDb("lob");
        Connection conn;
        conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int primary key, data blob)");
        PreparedStatement prep;
        Random random = new Random();
        byte[] buff = new byte[500000];
        for (int i = 0; i < 10; i++) {
            prep = conn.prepareStatement("insert into test values(?, ?)");
            prep.setInt(1, i);
            random.setSeed(i);
            random.nextBytes(buff);
            prep.setBinaryStream(2, new ByteArrayInputStream(buff), -1);
            prep.execute();
        }
        if (upgraded) {
            if (config.memory) {
                stat.execute("update information_schema.lob_map set pos=null");
            } else {
                stat.execute("alter table information_schema.lob_map drop column pos");
                conn.close();
                conn = getConnection("lob");
            }
        }
        prep = conn.prepareStatement("select * from test where id = ?");
        for (int i = 0; i < 1; i++) {
            random.setSeed(i);
            random.nextBytes(buff);
            for (int j = 0; j < buff.length; j += 10000) {
                prep.setInt(1, i);
                ResultSet rs = prep.executeQuery();
                rs.next();
                InputStream in = rs.getBinaryStream(2);
                in.skip(j);
                int t = in.read();
                assertEquals(t, buff[j] & 0xff);
            }
        }
        conn.close();
        conn.close();
    }

    /**
     * Test for issue 315: Java Level Deadlock on Database & Session Objects
     */
    private void testDeadlock() throws Exception {
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int primary key, name clob)");
        stat.execute("insert into test select x, space(10000) from system_range(1, 3)");
        final Connection conn2 = getConnection("lob");
        Task task = new Task() {

            public void call() throws Exception {
                Statement stat = conn2.createStatement();
                stat.setFetchSize(1);
                for (int i = 0; !stop; i++) {
                    ResultSet rs = stat.executeQuery("select * from test where id > -" + i);
                    while (rs.next()) {
                        // ignore
                    }
                }
            }

        };
        task.execute();
        stat.execute("create table test2(id int primary key, name clob)");
        for (int i = 0; i < 1000; i++) {
            stat.execute("delete from test2");
            stat.execute("insert into test2 values(1, space(10000 + " + i + "))");
        }
        task.get();
        conn.close();
        conn2.close();
    }

    private void testCopyManyLobs() throws Exception {
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id identity, data clob) as select 1, space(10000)");
        stat.execute("insert into test(id, data) select null, data from test");
        stat.execute("insert into test(id, data) select null, data from test");
        stat.execute("insert into test(id, data) select null, data from test");
        stat.execute("insert into test(id, data) select null, data from test");
        stat.execute("delete from test where id < 10");
        stat.execute("shutdown compact");
        conn.close();
    }

    private void testCopyLob() throws Exception {
        if (config.memory) {
            return;
        }
        deleteDb("lob");
        Connection conn;
        Statement stat;
        ResultSet rs;
        conn = getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create table test(id identity, data clob) as select 1, space(10000)");
        stat.execute("insert into test(id, data) select 2, data from test");
        stat.execute("delete from test where id = 1");
        conn.close();
        conn = getConnection("lob");
        stat = conn.createStatement();
        rs = stat.executeQuery("select * from test");
        rs.next();
        assertEquals(10000, rs.getString(2).length());
        conn.close();
    }

    private void testConcurrentCreate() throws Exception {
        deleteDb("lob");
        final JdbcConnection conn1 = (JdbcConnection) getConnection("lob");
        final JdbcConnection conn2 = (JdbcConnection) getConnection("lob");
        conn1.setAutoCommit(false);
        conn2.setAutoCommit(false);

        final byte[] buffer = new byte[10000];

        Task task1 = new Task() {
            public void call() throws Exception {
                while (!stop) {
                    Blob b = conn1.createBlob();
                    OutputStream out = b.setBinaryStream(1);
                    out.write(buffer);
                    out.close();
                }
            }
        };
        Task task2 = new Task() {
            public void call() throws Exception {
                while (!stop) {
                    Blob b = conn2.createBlob();
                    OutputStream out = b.setBinaryStream(1);
                    out.write(buffer);
                    out.close();
                }
            }
        };
        task1.execute();
        task2.execute();
        Thread.sleep(1000);
        task1.get();
        task2.get();
        conn1.close();
        conn2.close();
    }

    private void testLobInLargeResult() throws Exception {
        deleteDb("lob");
        Connection conn;
        Statement stat;
        conn = getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create table test(id int, data clob) as select x, null from system_range(1, 1000)");
        stat.execute("insert into test values(0, space(10000))");
        stat.execute("set max_memory_rows 100");
        ResultSet rs = stat.executeQuery("select * from test order by id desc");
        while (rs.next()) {
            // this threw a NullPointerException because
            // the disk based result set didn't know the lob handler
        }
        conn.close();
    }

    private void testUniqueIndex() throws Exception {
        deleteDb("lob");
        Connection conn;
        Statement stat;
        conn = getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create memory table test(x clob unique)");
        stat.execute("insert into test values('hello')");
        stat.execute("insert into test values('world')");
        assertThrows(ErrorCode.DUPLICATE_KEY_1, stat).
                execute("insert into test values('world')");
        stat.execute("insert into test values(space(10000) || 'a')");
        assertThrows(ErrorCode.DUPLICATE_KEY_1, stat).
                execute("insert into test values(space(10000) || 'a')");
        stat.execute("insert into test values(space(10000) || 'b')");
        conn.close();
    }

    private void testConvert() throws Exception {
        deleteDb("lob");
        Connection conn;
        Statement stat;
        conn = getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create table test(id int, data blob)");
        stat.execute("insert into test values(1, '')");
        ResultSet rs;
        rs = stat.executeQuery("select cast(data as clob) from test");
        rs.next();
        assertEquals("", rs.getString(1));
        stat.execute("drop table test");

        stat.execute("create table test(id int, data clob)");
        stat.execute("insert into test values(1, '')");
        rs = stat.executeQuery("select cast(data as blob) from test");
        rs.next();
        assertEquals("", rs.getString(1));

        conn.close();
    }

    private void testCreateAsSelect() throws Exception {
        deleteDb("lob");
        Connection conn;
        Statement stat;
        conn = getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create table test(id int, data clob) as select 1, space(10000)");
        conn.close();
    }

    private void testDropAllObjects() throws Exception {
        if (SysProperties.LOB_IN_DATABASE || config.memory) {
            return;
        }
        deleteDb("lob");
        Connection conn;
        Statement stat;
        conn = getConnection("lob");
        stat = conn.createStatement();

        stat.execute("create table test(id int primary key, name clob)");
        stat.execute("insert into test values(1, space(10000))");
        assertEquals(1, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());
        stat.execute("drop table test");
        assertEquals(0, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());

        stat.execute("create table test(id int primary key, name clob)");
        stat.execute("insert into test values(1, space(10000))");
        assertEquals(1, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());
        stat.execute("drop all objects");
        assertEquals(0, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());

        stat.execute("create table test(id int primary key, name clob)");
        stat.execute("insert into test values(1, space(10000))");
        assertEquals(1, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());
        stat.execute("truncate table test");
        assertEquals(0, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());

        conn.close();
    }

    private void testDelete() throws Exception {
        if (!SysProperties.LOB_IN_DATABASE || config.memory) {
            return;
        }
        deleteDb("lob");
        Connection conn;
        Statement stat;
        conn = getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create table test(id int primary key, name clob)");
        stat.execute("insert into test values(1, space(10000))");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("insert into test values(2, space(10000))");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("delete from test where id = 1");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("insert into test values(3, space(10000))");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("insert into test values(4, space(10000))");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("delete from test where id = 2");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("delete from test where id = 3");
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 1);
        stat.execute("delete from test");
        conn.close();
        conn = getConnection("lob");
        stat = conn.createStatement();
        assertSingleValue(stat, "select count(*) from information_schema.lob_data", 0);
        stat.execute("drop table test");
        conn.close();
    }

    private void testTempFilesDeleted(boolean stream) throws Exception {
        FileUtils.deleteRecursive(TEMP_DIR, true);
        FileUtils.createDirectories(TEMP_DIR);
        List<String> list = FileUtils.newDirectoryStream(TEMP_DIR);
        assertEquals("Unexpected temp file: " + list, 0, list.size());
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat;
        stat = conn.createStatement();
        stat.execute("create table test(id int primary key, name text)");
        PreparedStatement prep = conn.prepareStatement("insert into test values(2, ?)");
        if (stream) {
            String large = new String(new char[1024 * 1024 * 2]).replace((char) 0, 'x');
            prep.setCharacterStream(1, new StringReader(large), -1);
            large = null;
            prep.execute();
        } else {
            stat.execute("insert into test values(1, space(100000))");
        }
        /*
        list = FileUtils.newDirectoryStream(TEMP_DIR);
        assertEquals("Unexpected temp file: " + list, 0, list.size());
        */
        ResultSet rs;
        rs = stat.executeQuery("select * from test");
        while (rs.next()) {
            rs.getCharacterStream("name").close();
        }
        prep.close();
        conn.close();
        list = FileUtils.newDirectoryStream(TEMP_DIR);
        assertEquals("Unexpected temp file: " + list, 0, list.size());
    }

    private static void testAddLobRestart() throws SQLException {
        DeleteDbFiles.execute("memFS:", "lob", true);
        Connection conn = org.h2.Driver.load().connect("jdbc:h2:memFS:lob", null);
        Statement stat = conn.createStatement();
        stat.execute("create table test(d blob)");
        stat.execute("set MAX_LENGTH_INPLACE_LOB 1");
        PreparedStatement prep = conn.prepareCall("insert into test values('0000')");
        // long start = System.currentTimeMillis();
        for (int i = 0; i < 2000; i++) {
            // if (i % 1000 == 0) {
            //     long now = System.currentTimeMillis();
            //     System.out.println(i + " " + (now - start));
            //     start = now;
            // }
            prep.execute();
            ValueLob.resetDirCounter();
        }
        conn.close();
        DeleteDbFiles.execute("memFS:", "lob", true);
    }

    private void testLobUpdateMany() throws SQLException {
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table post(id int primary key, text clob) as select x, space(96) from system_range(1, 329)");
        PreparedStatement prep = conn.prepareStatement("update post set text = ?");
        prep.setCharacterStream(1, new StringReader(new String(new char[1025])), -1);
        prep.executeUpdate();
        conn.close();
    }

    private void testLobDeleteTemp() throws SQLException {
        if (SysProperties.LOB_IN_DATABASE) {
            return;
        }
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("create table test(data clob) as select space(100000) from dual");
        assertEquals(1, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());
        stat.execute("delete from test");
        conn.close();
        assertEquals(0, FileUtils.newDirectoryStream(getBaseDir() + "/lob.lobs.db").size());
    }

    private void testLobServerMemory() throws SQLException {
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT, DATA CLOB)");
        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST VALUES(1, ?)");
        StringReader reader = new StringReader(new String(new char[100000]));
        prep.setCharacterStream(1, reader, -1);
        prep.execute();
        conn.close();
    }

    private void testLobDelete() throws SQLException {
        if (config.memory || SysProperties.LOB_IN_DATABASE) {
            return;
        }
        deleteDb("lob");
        Connection conn = reconnect(null);
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT, DATA CLOB)");
        stat.execute("INSERT INTO TEST SELECT X, SPACE(10000) FROM SYSTEM_RANGE(1, 10)");
        ArrayList<String> list = FileLister.getDatabaseFiles(getBaseDir(), "lob", true);
        stat.execute("UPDATE TEST SET DATA = SPACE(5000)");
        collectAndWait();
        stat.execute("CHECKPOINT");
        ArrayList<String> list2 = FileLister.getDatabaseFiles(getBaseDir(), "lob", true);
        if (list2.size() >= list.size() + 5) {
            fail("Expected not many more files, got " + list2.size() + " was " + list.size());
        }
        stat.execute("DELETE FROM TEST");
        collectAndWait();
        stat.execute("CHECKPOINT");
        ArrayList<String> list3 = FileLister.getDatabaseFiles(getBaseDir(), "lob", true);
        if (list3.size() >= list.size()) {
            fail("Expected less files, got " + list2.size() + " was " + list.size());
        }
        conn.close();
    }

    private static void collectAndWait() {
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private void testLobVariable() throws SQLException {
        deleteDb("lob");
        Connection conn = reconnect(null);
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT, DATA CLOB)");
        stat.execute("INSERT INTO TEST VALUES(1, SPACE(100000))");
        stat.execute("SET @TOTAL = SELECT DATA FROM TEST WHERE ID=1");
        stat.execute("DROP TABLE TEST");
        stat.execute("CALL @TOTAL LIKE '%X'");
        stat.execute("CREATE TABLE TEST(ID INT, DATA CLOB)");
        stat.execute("INSERT INTO TEST VALUES(1, @TOTAL)");
        stat.execute("INSERT INTO TEST VALUES(2, @TOTAL)");
        stat.execute("DROP TABLE TEST");
        stat.execute("CALL @TOTAL LIKE '%X'");
        conn.close();
    }

    private void testLobDrop() throws SQLException {
        if (config.networked) {
            return;
        }
        deleteDb("lob");
        Connection conn = reconnect(null);
        Statement stat = conn.createStatement();
        for (int i = 0; i < 500; i++) {
            stat.execute("CREATE TABLE T" + i + "(ID INT, C CLOB)");
        }
        stat.execute("CREATE TABLE TEST(ID INT, C CLOB)");
        stat.execute("INSERT INTO TEST VALUES(1, SPACE(10000))");
        for (int i = 0; i < 500; i++) {
            stat.execute("DROP TABLE T" + i);
        }
        ResultSet rs = stat.executeQuery("SELECT * FROM TEST");
        while (rs.next()) {
            rs.getString("C");
        }
        conn.close();
    }

    private void testLobNoClose() throws Exception {
        if (config.networked) {
            return;
        }
        deleteDb("lob");
        Connection conn = reconnect(null);
        conn.createStatement().execute("CREATE TABLE TEST(ID IDENTITY, DATA CLOB)");
        conn.createStatement().execute("INSERT INTO TEST VALUES(1, SPACE(10000))");
        ResultSet rs = conn.createStatement().executeQuery("SELECT DATA FROM TEST");
        rs.next();
        SysProperties.lobCloseBetweenReads = true;
        Reader in = rs.getCharacterStream(1);
        in.read();
        conn.createStatement().execute("DELETE FROM TEST");
        SysProperties.lobCloseBetweenReads = false;
        conn.createStatement().execute("INSERT INTO TEST VALUES(1, SPACE(10000))");
        rs = conn.createStatement().executeQuery("SELECT DATA FROM TEST");
        rs.next();
        in = rs.getCharacterStream(1);
        in.read();
        conn.setAutoCommit(false);
        try {
            conn.createStatement().execute("DELETE FROM TEST");
            conn.commit();
            // DELETE does not fail in Linux, but in Windows
            // error("Error expected");
            // but reading afterwards should fail
            int len = 0;
            while (true) {
                int x = in.read();
                if (x < 0) {
                    break;
                }
                len++;
            }
            in.close();
            if (len > 0) {
                // in Linux, it seems it is still possible to read in files
                // even if they are deleted
                if (System.getProperty("os.name").indexOf("Windows") > 0) {
                    fail("Error expected; len=" + len);
                }
            }
        } catch (SQLException e) {
            assertKnownException(e);
        }
        conn.rollback();
        conn.close();
    }

    private void testLobTransactions(int spaceLen) throws SQLException {
        deleteDb("lob");
        Connection conn = reconnect(null);
        conn.createStatement().execute("CREATE TABLE TEST(ID IDENTITY, DATA CLOB, DATA2 VARCHAR)");
        conn.setAutoCommit(false);
        Random random = new Random(0);
        int rows = 0;
        Savepoint sp = null;
        int len = getSize(100, 400);
        for (int i = 0; i < len; i++) {
            switch (random.nextInt(10)) {
            case 0:
                trace("insert");
                conn.createStatement().execute(
                        "INSERT INTO TEST(DATA, DATA2) VALUES('" + i + "' || SPACE(" + spaceLen + "), '" + i + "')");
                rows++;
                break;
            case 1:
                if (rows > 0) {
                    trace("delete");
                    conn.createStatement().execute("DELETE FROM TEST WHERE ID=" + random.nextInt(rows));
                }
                break;
            case 2:
                if (rows > 0) {
                    trace("update");
                    conn.createStatement().execute(
                            "UPDATE TEST SET DATA='x' || DATA, DATA2='x' || DATA2 WHERE ID=" + random.nextInt(rows));
                }
                break;
            case 3:
                if (rows > 0) {
                    trace("commit");
                    conn.commit();
                    sp = null;
                }
                break;
            case 4:
                if (rows > 0) {
                    trace("rollback");
                    conn.rollback();
                    sp = null;
                }
                break;
            case 5:
                trace("savepoint");
                sp = conn.setSavepoint();
                break;
            case 6:
                if (sp != null) {
                    trace("rollback to savepoint");
                    conn.rollback(sp);
                }
                break;
            case 7:
                if (rows > 0) {
                    trace("checkpoint");
                    conn.createStatement().execute("CHECKPOINT");
                    trace("shutdown immediately");
                    conn.createStatement().execute("SHUTDOWN IMMEDIATELY");
                    trace("shutdown done");
                    conn = reconnect(conn);
                    conn.setAutoCommit(false);
                    sp = null;
                }
                break;
            default:
            }
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEST");
            while (rs.next()) {
                String d1 = rs.getString("DATA").trim();
                String d2 = rs.getString("DATA2").trim();
                assertEquals(d1, d2);
            }

        }
        conn.close();
    }

    private void testLobRollbackStop() throws SQLException {
        deleteDb("lob");
        Connection conn = reconnect(null);
        conn.createStatement().execute("CREATE TABLE TEST(ID INT PRIMARY KEY, DATA CLOB)");
        conn.createStatement().execute("INSERT INTO TEST VALUES(1, SPACE(10000))");
        conn.setAutoCommit(false);
        conn.createStatement().execute("DELETE FROM TEST");
        conn.createStatement().execute("CHECKPOINT");
        conn.createStatement().execute("SHUTDOWN IMMEDIATELY");
        conn = reconnect(conn);
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEST");
        assertTrue(rs.next());
        rs.getInt(1);
        assertEquals(10000, rs.getString(2).length());
        conn.close();
    }

    private void testLobCopy() throws SQLException {
        deleteDb("lob");
        Connection conn = reconnect(null);
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int, data clob)");
        stat.execute("insert into test values(1, space(1000));");
        stat.execute("insert into test values(2, space(10000));");
        stat.execute("create table test2(id int, data clob);");
        stat.execute("insert into test2 select * from test;");
        stat.execute("drop table test;");
        stat.execute("select * from test2;");
        stat.execute("update test2 set id=id;");
        stat.execute("select * from test2;");
        conn.close();
    }

    private void testLobHibernate() throws Exception {
        deleteDb("lob");
        Connection conn0 = reconnect(null);

        conn0.getAutoCommit();
        conn0.setAutoCommit(false);
        DatabaseMetaData dbMeta0 = conn0.getMetaData();
        dbMeta0.getDatabaseProductName();
        dbMeta0.getDatabaseMajorVersion();
        dbMeta0.getDatabaseProductVersion();
        dbMeta0.getDriverName();
        dbMeta0.getDriverVersion();
        dbMeta0.supportsResultSetType(1004);
        dbMeta0.supportsBatchUpdates();
        dbMeta0.dataDefinitionCausesTransactionCommit();
        dbMeta0.dataDefinitionIgnoredInTransactions();
        dbMeta0.supportsGetGeneratedKeys();
        conn0.getAutoCommit();
        conn0.getAutoCommit();
        conn0.commit();
        conn0.setAutoCommit(true);
        Statement stat0 = conn0.createStatement();
        stat0.executeUpdate("drop table CLOB_ENTITY if exists");
        stat0.getWarnings();
        stat0.executeUpdate("create table CLOB_ENTITY (ID bigint not null, DATA clob, CLOB_DATA clob, primary key (ID))");
        stat0.getWarnings();
        stat0.close();
        conn0.getWarnings();
        conn0.clearWarnings();
        conn0.setAutoCommit(false);
        conn0.getAutoCommit();
        conn0.getAutoCommit();
        PreparedStatement prep0 = conn0.prepareStatement("select max(ID) from CLOB_ENTITY");
        ResultSet rs0 = prep0.executeQuery();
        rs0.next();
        rs0.getLong(1);
        rs0.wasNull();
        rs0.close();
        prep0.close();
        conn0.getAutoCommit();
        PreparedStatement prep1 = conn0
                .prepareStatement("insert into CLOB_ENTITY (DATA, CLOB_DATA, ID) values (?, ?, ?)");
        prep1.setNull(1, 2005);
        StringBuilder buff = new StringBuilder(10000);
        for (int i = 0; i < 10000; i++) {
            buff.append((char) ('0' + (i % 10)));
        }
        Reader x = new StringReader(buff.toString());
        prep1.setCharacterStream(2, x, 10000);
        prep1.setLong(3, 1);
        prep1.addBatch();
        prep1.executeBatch();
        prep1.close();
        conn0.getAutoCommit();
        conn0.getAutoCommit();
        conn0.commit();
        conn0.isClosed();
        conn0.getWarnings();
        conn0.clearWarnings();
        conn0.getAutoCommit();
        conn0.getAutoCommit();
        PreparedStatement prep2 = conn0
                .prepareStatement("select c_.ID as ID0_0_, c_.DATA as S_, " +
                        "c_.CLOB_DATA as CLOB3_0_0_ from CLOB_ENTITY c_ where c_.ID=?");
        prep2.setLong(1, 1);
        ResultSet rs1 = prep2.executeQuery();
        rs1.next();
        rs1.getCharacterStream("S_");
        Clob clob0 = rs1.getClob("CLOB3_0_0_");
        rs1.wasNull();
        rs1.next();
        rs1.close();
        prep2.getMaxRows();
        prep2.getQueryTimeout();
        prep2.close();
        conn0.getAutoCommit();
        Reader r = clob0.getCharacterStream();
        for (int i = 0; i < 10000; i++) {
            int ch = r.read();
            if (ch != ('0' + (i % 10))) {
                fail("expected " + (char) ('0' + (i % 10)) + " got: " + ch + " (" + (char) ch + ")");
            }
        }
        int ch = r.read();
        if (ch != -1) {
            fail("expected -1 got: " + ch);
        }
        conn0.close();
    }

    private void testLobCopy(boolean compress) throws SQLException {
        deleteDb("lob");
        Connection conn;
        conn = reconnect(null);
        Statement stat = conn.createStatement();
        if (compress) {
            stat.execute("SET COMPRESS_LOB LZF");
        } else {
            stat.execute("SET COMPRESS_LOB NO");
        }
        conn = reconnect(conn);
        stat = conn.createStatement();
        ResultSet rs;
        rs = stat.executeQuery("select value from information_schema.settings where NAME='COMPRESS_LOB'");
        rs.next();
        assertEquals(compress ? "LZF" : "NO", rs.getString(1));
        assertFalse(rs.next());
        stat.execute("create table test(text clob)");
        stat.execute("create table test2(text clob)");
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buff.append(' ');
        }
        String spaces = buff.toString();
        stat.execute("insert into test values('" + spaces + "')");
        stat.execute("insert into test2 select * from test");
        rs = stat.executeQuery("select * from test2");
        rs.next();
        assertEquals(spaces, rs.getString(1));
        stat.execute("drop table test");
        rs = stat.executeQuery("select * from test2");
        rs.next();
        assertEquals(spaces, rs.getString(1));
        stat.execute("alter table test2 add column id int before text");
        rs = stat.executeQuery("select * from test2");
        rs.next();
        assertEquals(spaces, rs.getString("text"));
        conn.close();
    }

    private void testLobCompression(boolean compress) throws Exception {
        deleteDb("lob");
        Connection conn;
        conn = reconnect(null);
        if (compress) {
            conn.createStatement().execute("SET COMPRESS_LOB LZF");
        } else {
            conn.createStatement().execute("SET COMPRESS_LOB NO");
        }
        conn.createStatement().execute("CREATE TABLE TEST(ID INT PRIMARY KEY, C CLOB)");
        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST VALUES(?, ?)");
        long time = System.currentTimeMillis();
        int len = getSize(10, 40);
        if (config.networked && config.big) {
            len = 5;
        }
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buff.append(StringUtils.xmlNode("content", null, "This is a test " + i));
        }
        String xml = buff.toString();
        for (int i = 0; i < len; i++) {
            prep.setInt(1, i);
            prep.setString(2, xml + i);
            prep.execute();
        }
        for (int i = 0; i < len; i++) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEST");
            while (rs.next()) {
                if (i == 0) {
                    assertEquals(xml + rs.getInt(1), rs.getString(2));
                } else {
                    Reader r = rs.getCharacterStream(2);
                    String result = IOUtils.readStringAndClose(r, -1);
                    assertEquals(xml + rs.getInt(1), result);
                }
            }
        }
        time = System.currentTimeMillis() - time;
        trace("time: " + time + " compress: " + compress);
        conn.close();
        if (!config.memory && SysProperties.LOB_IN_DATABASE) {
            long length = new File(getBaseDir() + "/lob.h2.db").length();
            trace("len: " + length + " compress: " + compress);
        }
    }

    private void testManyLobs() throws Exception {
        deleteDb("lob");
        Connection conn;
        conn = reconnect(null);
        conn.createStatement().execute("CREATE TABLE TEST(ID INT PRIMARY KEY, B BLOB, C CLOB)");
        int len = getSize(10, 2000);
        if (config.networked) {
            len = 100;
        }

        int first = 1, increment = 19;

        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST(ID, B, C) VALUES(?, ?, ?)");
        for (int i = first; i < len; i += increment) {
            int l = i;
            prep.setInt(1, i);
            prep.setBinaryStream(2, getRandomStream(l, i), -1);
            prep.setCharacterStream(3, getRandomReader(l, i), -1);
            prep.execute();
        }

        conn = reconnect(conn);
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEST ORDER BY ID");
        while (rs.next()) {
            int i = rs.getInt("ID");
            Blob b = rs.getBlob("B");
            Clob c = rs.getClob("C");
            int l = i;
            assertEquals(l, b.length());
            assertEquals(l, c.length());
            assertEqualStreams(getRandomStream(l, i), b.getBinaryStream(), -1);
            assertEqualReaders(getRandomReader(l, i), c.getCharacterStream(), -1);
        }

        prep = conn.prepareStatement("UPDATE TEST SET B=?, C=? WHERE ID=?");
        for (int i = first; i < len; i += increment) {
            int l = i;
            prep.setBinaryStream(1, getRandomStream(l, -i), -1);
            prep.setCharacterStream(2, getRandomReader(l, -i), -1);
            prep.setInt(3, i);
            prep.execute();
        }

        conn = reconnect(conn);
        rs = conn.createStatement().executeQuery("SELECT * FROM TEST ORDER BY ID");
        while (rs.next()) {
            int i = rs.getInt("ID");
            Blob b = rs.getBlob("B");
            Clob c = rs.getClob("C");
            int l = i;
            assertEquals(l, b.length());
            assertEquals(l, c.length());
            assertEqualStreams(getRandomStream(l, -i), b.getBinaryStream(), -1);
            assertEqualReaders(getRandomReader(l, -i), c.getCharacterStream(), -1);
        }

        conn.close();
    }

    private void testClob() throws Exception {
        deleteDb("lob");
        Connection conn;
        conn = reconnect(null);
        conn.createStatement().execute("CREATE TABLE TEST(ID IDENTITY, C CLOB)");
        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST(C) VALUES(?)");
        prep.setCharacterStream(1, new CharArrayReader("Bohlen".toCharArray()), "Bohlen".length());
        prep.execute();
        prep.setCharacterStream(1, new CharArrayReader("B\u00f6hlen".toCharArray()), "B\u00f6hlen".length());
        prep.execute();
        prep.setCharacterStream(1, getRandomReader(501, 1), -1);
        prep.execute();
        prep.setCharacterStream(1, getRandomReader(1501, 2), 401);
        prep.execute();
        conn = reconnect(conn);
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEST ORDER BY ID");
        rs.next();
        assertEquals("Bohlen", rs.getString("C"));
        assertEqualReaders(new CharArrayReader("Bohlen".toCharArray()), rs.getCharacterStream("C"), -1);
        rs.next();
        assertEqualReaders(new CharArrayReader("B\u00f6hlen".toCharArray()), rs.getCharacterStream("C"), -1);
        rs.next();
        assertEqualReaders(getRandomReader(501, 1), rs.getCharacterStream("C"), -1);
        Clob clob = rs.getClob("C");
        assertEqualReaders(getRandomReader(501, 1), clob.getCharacterStream(), -1);
        assertEquals(501, clob.length());
        rs.next();
        assertEqualReaders(getRandomReader(401, 2), rs.getCharacterStream("C"), -1);
        assertEqualReaders(getRandomReader(1500, 2), rs.getCharacterStream("C"), 401);
        clob = rs.getClob("C");
        assertEqualReaders(getRandomReader(1501, 2), clob.getCharacterStream(), 401);
        assertEqualReaders(getRandomReader(401, 2), clob.getCharacterStream(), 401);
        assertEquals(401, clob.length());
        assertFalse(rs.next());
        conn.close();
    }

    private Connection reconnect(Connection conn) throws SQLException {
        long time = System.currentTimeMillis();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        conn = getConnection("lob");
        trace("re-connect=" + (System.currentTimeMillis() - time));
        return conn;
    }

    private void testUpdateLob() throws SQLException {
        deleteDb("lob");
        Connection conn;
        conn = reconnect(null);

        PreparedStatement prep = conn
                .prepareStatement("CREATE TABLE IF NOT EXISTS p( id int primary key, rawbyte BLOB ); ");
        prep.execute();
        prep.close();

        prep = conn.prepareStatement("INSERT INTO p(id) VALUES(?);");
        for (int i = 0; i < 10; i++) {
            prep.setInt(1, i);
            prep.execute();
        }
        prep.close();

        prep = conn.prepareStatement("UPDATE p set rawbyte=? WHERE id=?");
        for (int i = 0; i < 8; i++) {
            prep.setBinaryStream(1, getRandomStream(10000, i), 0);
            prep.setInt(2, i);
            prep.execute();
        }
        prep.close();
        conn.commit();

        conn = reconnect(conn);

        conn.setAutoCommit(true);
        prep = conn.prepareStatement("UPDATE p set rawbyte=? WHERE id=?");
        for (int i = 8; i < 10; i++) {
            prep.setBinaryStream(1, getRandomStream(10000, i), 0);
            prep.setInt(2, i);
            prep.execute();
        }
        prep.close();

        prep = conn.prepareStatement("SELECT * from p");
        ResultSet rs = prep.executeQuery();
        while (rs.next()) {
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                rs.getMetaData().getColumnName(i);
                rs.getString(i);
            }
        }
        conn.close();
    }

    private void testLobReconnect() throws Exception {
        deleteDb("lob");
        Connection conn = reconnect(null);
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, TEXT CLOB)");
        PreparedStatement prep;
        prep = conn.prepareStatement("INSERT INTO TEST VALUES(1, ?)");
        String s = new String(getRandomChars(10000, 1));
        byte[] data = s.getBytes("UTF-8");
        prep.setBinaryStream(1, new ByteArrayInputStream(data), 0);
        prep.execute();

        conn = reconnect(conn);
        stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT * FROM TEST WHERE ID=1");
        rs.next();
        InputStream in = new ByteArrayInputStream(data);
        assertEqualStreams(in, rs.getBinaryStream("TEXT"), -1);

        prep = conn.prepareStatement("UPDATE TEST SET TEXT = ?");
        prep.setBinaryStream(1, new ByteArrayInputStream(data), 0);
        prep.execute();

        conn = reconnect(conn);
        stat = conn.createStatement();
        rs = stat.executeQuery("SELECT * FROM TEST WHERE ID=1");
        rs.next();
        assertEqualStreams(rs.getBinaryStream("TEXT"), new ByteArrayInputStream(data), -1);

        stat.execute("DROP TABLE IF EXISTS TEST");
        conn.close();
    }

    private void testLob(boolean clob) throws Exception {
        deleteDb("lob");
        Connection conn = reconnect(null);
        conn = reconnect(conn);
        Statement stat = conn.createStatement();
        stat.execute("DROP TABLE IF EXISTS TEST");
        PreparedStatement prep;
        ResultSet rs;
        long time;
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, VALUE " + (clob ? "CLOB" : "BLOB") + ")");

        int len = getSize(1, 1000);
        if (config.networked && config.big) {
            len = 100;
        }

        time = System.currentTimeMillis();
        prep = conn.prepareStatement("INSERT INTO TEST VALUES(?, ?)");
        for (int i = 0; i < len; i += i + i + 1) {
            prep.setInt(1, i);
            int size = i * i;
            if (clob) {
                prep.setCharacterStream(2, getRandomReader(size, i), 0);
            } else {
                prep.setBinaryStream(2, getRandomStream(size, i), 0);
            }
            prep.execute();
        }
        trace("insert=" + (System.currentTimeMillis() - time));
        traceMemory();
        conn = reconnect(conn);

        time = System.currentTimeMillis();
        prep = conn.prepareStatement("SELECT ID, VALUE FROM TEST");
        rs = prep.executeQuery();
        while (rs.next()) {
            int id = rs.getInt("ID");
            int size = id * id;
            if (clob) {
                Reader rt = rs.getCharacterStream(2);
                assertEqualReaders(getRandomReader(size, id), rt, -1);
                Object obj = rs.getObject(2);
                if (obj instanceof Clob) {
                    obj = ((Clob) obj).getCharacterStream();
                }
                assertEqualReaders(getRandomReader(size, id), (Reader) obj, -1);
            } else {
                InputStream in = rs.getBinaryStream(2);
                assertEqualStreams(getRandomStream(size, id), in, -1);
                Object obj = rs.getObject(2);
                if (obj instanceof Blob) {
                    obj = ((Blob) obj).getBinaryStream();
                }
                assertEqualStreams(getRandomStream(size, id), (InputStream) obj, -1);
            }
        }
        trace("select=" + (System.currentTimeMillis() - time));
        traceMemory();

        conn = reconnect(conn);

        time = System.currentTimeMillis();
        prep = conn.prepareStatement("DELETE FROM TEST WHERE ID=?");
        for (int i = 0; i < len; i++) {
            prep.setInt(1, i);
            prep.executeUpdate();
        }
        trace("delete=" + (System.currentTimeMillis() - time));
        traceMemory();
        conn = reconnect(conn);

        conn.setAutoCommit(false);
        prep = conn.prepareStatement("INSERT INTO TEST VALUES(1, ?)");
        if (clob) {
            prep.setCharacterStream(1, getRandomReader(0, 0), 0);
        } else {
            prep.setBinaryStream(1, getRandomStream(0, 0), 0);
        }
        prep.execute();
        conn.rollback();
        prep.execute();
        conn.commit();

        conn.createStatement().execute("DELETE FROM TEST WHERE ID=1");
        conn.rollback();
        conn.createStatement().execute("DELETE FROM TEST WHERE ID=1");
        conn.commit();

        conn.createStatement().execute("DROP TABLE TEST");
        conn.close();
    }

    private void testJavaObject() throws SQLException {
        deleteDb("lob");
        Connection conn = getConnection("lob");
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, DATA OTHER)");
        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST VALUES(1, ?)");
        prep.setObject(1, new TestLobObject("abc"));
        prep.execute();
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEST");
        rs.next();
        Object oa = rs.getObject(2);
        assertEquals(TestLobObject.class.getName(), oa.getClass().getName());
        Object ob = rs.getObject("DATA");
        assertEquals(TestLobObject.class.getName(), ob.getClass().getName());
        assertEquals("TestLobObject: abc", oa.toString());
        assertEquals("TestLobObject: abc", ob.toString());
        assertFalse(rs.next());

        conn.createStatement().execute("drop table test");
        stat.execute("create table test(value other)");
        prep = conn.prepareStatement("insert into test values(?)");
        prep.setObject(1, Utils.serialize(""));
        prep.execute();
        rs = stat.executeQuery("select value from test");
        while (rs.next()) {
            assertEquals("", (String) rs.getObject("value"));
        }
        conn.close();
    }

    private static Reader getRandomReader(int len, int seed) {
        return new CharArrayReader(getRandomChars(len, seed));
    }

    private static char[] getRandomChars(int len, int seed) {
        Random random = new Random(seed);
        char[] buff = new char[len];
        for (int i = 0; i < len; i++) {
            char ch;
            do {
                ch = (char) random.nextInt(Character.MAX_VALUE);
                // UTF8: String.getBytes("UTF-8") only returns 1 byte for
                // 0xd800-0xdfff
            } while (ch >= 0xd800 && ch <= 0xdfff);
            buff[i] = ch;
        }
        return buff;
    }

    private static InputStream getRandomStream(int len, int seed) {
        Random random = new Random(seed);
        byte[] buff = new byte[len];
        random.nextBytes(buff);
        return new ByteArrayInputStream(buff);
    }

}
