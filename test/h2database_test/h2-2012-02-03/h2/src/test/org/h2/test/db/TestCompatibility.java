/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.constant.ErrorCode;
import org.h2.test.TestBase;

/**
 * Tests the compatibility with other databases.
 */
public class TestCompatibility extends TestBase {

    private Connection conn;

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    public void test() throws SQLException {
        deleteDb("compatibility");

        testCaseSensitiveIdentifiers();
        testKeyAsColumnInMySQLMode();

        conn = getConnection("compatibility");
        testDomain();
        testColumnAlias();
        testUniqueIndexSingleNull();
        testUniqueIndexOracle();
        testHsqlDb();
        testMySQL();
        testDB2();
        testDerby();
        testPlusSignAsConcatOperator();

        conn.close();
        deleteDb("compatibility");
    }

    private void testKeyAsColumnInMySQLMode() throws SQLException {
        Connection c = getConnection("compatibility;MODE=MYSQL");
        Statement stat = c.createStatement();
        stat.execute("create table test(id int primary key, key varchar)");
        stat.execute("drop table test");
        c.close();
    }

    private void testCaseSensitiveIdentifiers() throws SQLException {
        Connection c = getConnection("compatibility;DATABASE_TO_UPPER=FALSE");
        Statement stat = c.createStatement();
        stat.execute("create table test(id int primary key, name varchar) as select 1, 'hello'");
        assertThrows(ErrorCode.TABLE_OR_VIEW_ALREADY_EXISTS_1, stat).
            execute("create table test(id int primary key, name varchar)");
        assertThrows(ErrorCode.DUPLICATE_COLUMN_NAME_1, stat).
            execute("alter table test add column Name varchar");
        ResultSet rs;

        DatabaseMetaData meta = c.getMetaData();
        rs = meta.getTables(null, null, "test", null);
        assertTrue(rs.next());
        assertEquals("test", rs.getString("TABLE_NAME"));

        rs = stat.executeQuery("select id, name from test");
        assertEquals("id", rs.getMetaData().getColumnLabel(1));
        assertEquals("name", rs.getMetaData().getColumnLabel(2));

        rs = stat.executeQuery("select Id, Name from Test");
        assertEquals("id", rs.getMetaData().getColumnLabel(1));
        assertEquals("name", rs.getMetaData().getColumnLabel(2));

        rs = stat.executeQuery("select ID, NAME from TEST");
        assertEquals("id", rs.getMetaData().getColumnLabel(1));
        assertEquals("name", rs.getMetaData().getColumnLabel(2));

        stat.execute("select COUNT(*), count(*), Count(*), Sum(id) from test");

        stat.execute("select LENGTH(name), length(name), Length(name) from test");

        stat.execute("select t.id from test t group by t.id");
        stat.execute("select id from test t group by t.id");
        stat.execute("select id from test group by ID");
        stat.execute("select id as c from test group by c");
        stat.execute("select t.id from test t group by T.ID");
        stat.execute("select id from test t group by T.ID");

        stat.execute("drop table test");
        c.close();
    }

    private void testDomain() throws SQLException {
        if (config.memory) {
            return;
        }
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int primary key) as select 1");
        assertThrows(ErrorCode.USER_DATA_TYPE_ALREADY_EXISTS_1, stat).
                execute("create domain int as varchar");
        conn.close();
        conn = getConnection("compatibility");
        stat = conn.createStatement();
        stat.execute("insert into test values(2)");
        stat.execute("drop table test");
    }

    private void testColumnAlias() throws SQLException {
        Statement stat = conn.createStatement();
        String[] modes = { "PostgreSQL", "MySQL", "HSQLDB", "MSSQLServer", "Derby", "Oracle", "Regular" };
        String columnAlias;
        columnAlias = "MySQL,Regular";
        stat.execute("CREATE TABLE TEST(ID INT)");
        for (String mode : modes) {
            stat.execute("SET MODE " + mode);
            ResultSet rs = stat.executeQuery("SELECT ID I FROM TEST");
            ResultSetMetaData meta = rs.getMetaData();
            String columnName = meta.getColumnName(1);
            String tableName = meta.getTableName(1);
            if ("ID".equals(columnName) && "TEST".equals(tableName)) {
                assertTrue(mode + " mode should not support columnAlias", columnAlias.indexOf(mode) >= 0);
            } else if ("I".equals(columnName) && tableName == null) {
                assertTrue(mode + " mode should support columnAlias", columnAlias.indexOf(mode) < 0);
            } else {
                fail();
            }
        }
        stat.execute("DROP TABLE TEST");
    }

    private void testUniqueIndexSingleNull() throws SQLException {
        Statement stat = conn.createStatement();
        String[] modes = { "PostgreSQL", "MySQL", "HSQLDB", "MSSQLServer", "Derby", "Oracle", "Regular" };
        String multiNull = "PostgreSQL,MySQL,Oracle,Regular";
        for (String mode : modes) {
            stat.execute("SET MODE " + mode);
            stat.execute("CREATE TABLE TEST(ID INT)");
            stat.execute("CREATE UNIQUE INDEX IDX_ID_U ON TEST(ID)");
            try {
                stat.execute("INSERT INTO TEST VALUES(1), (2), (NULL), (NULL)");
                assertTrue(mode + " mode should not support multiple NULL", multiNull.indexOf(mode) >= 0);
            } catch (SQLException e) {
                assertTrue(mode + " mode should support multiple NULL", multiNull.indexOf(mode) < 0);
            }
            stat.execute("DROP TABLE TEST");
        }
    }

    private void testUniqueIndexOracle() throws SQLException {
        Statement stat = conn.createStatement();
        stat.execute("SET MODE ORACLE");
        stat.execute("create table t2(c1 int, c2 int)");
        stat.execute("create unique index i2 on t2(c1, c2)");
        stat.execute("insert into t2 values (null, 1)");
        assertThrows(ErrorCode.DUPLICATE_KEY_1, stat).
                execute("insert into t2 values (null, 1)");
        stat.execute("insert into t2 values (null, null)");
        stat.execute("insert into t2 values (null, null)");
        stat.execute("insert into t2 values (1, null)");
        assertThrows(ErrorCode.DUPLICATE_KEY_1, stat).
                execute("insert into t2 values (1, null)");
        stat.execute("DROP TABLE T2");
    }

    private void testHsqlDb() throws SQLException {
        Statement stat = conn.createStatement();
        stat.execute("DROP TABLE TEST IF EXISTS; CREATE TABLE TEST(ID INT PRIMARY KEY); ");
        stat.execute("CALL CURRENT_TIME");
        stat.execute("CALL CURRENT_TIMESTAMP");
        stat.execute("CALL CURRENT_DATE");
        stat.execute("CALL SYSDATE");
        stat.execute("CALL TODAY");

        stat.execute("DROP TABLE TEST IF EXISTS");
        stat.execute("CREATE TABLE TEST(ID INT)");
        stat.execute("INSERT INTO TEST VALUES(1)");
        PreparedStatement prep = conn.prepareStatement("SELECT LIMIT ? 1 ID FROM TEST");
        prep.setInt(1, 2);
        prep.executeQuery();
        stat.execute("DROP TABLE TEST IF EXISTS");

    }

    private void testMySQL() throws SQLException {
        Statement stat = conn.createStatement();
        stat.execute("SELECT 1");
        stat.execute("DROP TABLE IF EXISTS TEST");
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR)");
        stat.execute("INSERT INTO TEST VALUES(1, 'Hello'), (2, 'World')");
        org.h2.mode.FunctionsMySQL.register(conn);
        assertResult("0", stat, "SELECT UNIX_TIMESTAMP('1970-01-01 00:00:00Z')");
        assertResult("1196418619", stat, "SELECT UNIX_TIMESTAMP('2007-11-30 10:30:19Z')");
        assertResult("1196418619", stat, "SELECT UNIX_TIMESTAMP(FROM_UNIXTIME(1196418619))");
        assertResult("2007 November", stat, "SELECT FROM_UNIXTIME(1196300000, '%Y %M')");
        assertResult("2003-12-31", stat, "SELECT DATE('2003-12-31 11:02:03')");

        if (config.memory) {
            return;
        }
        // need to reconnect, because meta data tables may be initialized
        conn.close();
        conn = getConnection("compatibility;MODE=MYSQL");
        stat = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        assertResult("test", stat, "SHOW TABLES");
        ResultSet rs = stat.executeQuery("SELECT * FROM TEST");
        rs.next();
        rs.updateString(2, "Hallo");
        rs.updateRow();

        conn.close();
        conn = getConnection("compatibility");
    }

    private void testPlusSignAsConcatOperator() throws SQLException {
        Statement stat = conn.createStatement();
        stat.execute("SET MODE MSSQLServer");
        stat.execute("DROP TABLE IF EXISTS TEST");
        stat.execute("CREATE TABLE TEST(NAME VARCHAR(50), SURNAME VARCHAR(50))");
        stat.execute("INSERT INTO TEST VALUES('John', 'Doe')");
        stat.execute("INSERT INTO TEST VALUES('Jack', 'Sullivan')");

        assertResult("abcd123", stat, "SELECT 'abc' + 'd123'");

        assertResult("Doe, John", stat,
                "SELECT surname + ', ' + name FROM test WHERE SUBSTRING(NAME,1,1)+SUBSTRING(SURNAME,1,1) = 'JD'");

        stat.execute("ALTER TABLE TEST ADD COLUMN full_name VARCHAR(100)");
        stat.execute("UPDATE TEST SET full_name = name + ', ' + surname");
        assertResult("John, Doe", stat, "SELECT full_name FROM TEST where name='John'");

        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST VALUES(?, ?, ? + ', ' + ?)");
        int ca = 1;
        prep.setString(ca++, "Paul");
        prep.setString(ca++, "Frank");
        prep.setString(ca++, "Paul");
        prep.setString(ca++, "Frank");
        prep.executeUpdate();
        prep.close();

        assertResult("Paul, Frank", stat, "SELECT full_name FROM test WHERE name = 'Paul'");

        prep = conn.prepareStatement("SELECT ? + ?");
        int cb = 1;
        prep.setString(cb++, "abcd123");
        prep.setString(cb++, "d123");
        prep.executeQuery();
        prep.close();

        prep = conn.prepareStatement("SELECT full_name FROM test WHERE (SUBSTRING(name, 1, 1) + SUBSTRING(surname, 2, 3)) = ?");
        prep.setString(1, "Joe");
        ResultSet res = prep.executeQuery();
        assertTrue("Result cannot be empty", res.next());
        assertEquals("John, Doe", res.getString(1));
        res.close();
        prep.close();

    }

    private void testDB2() throws SQLException {
        conn = getConnection("compatibility;MODE=DB2");
        ResultSet res = conn.createStatement().executeQuery("SELECT 1 FROM sysibm.sysdummy1");
        res.next();
        assertEquals("1", res.getString(1));
        conn.close();
        conn = getConnection("compatibility;MODE=MySQL");
        assertThrows(ErrorCode.SCHEMA_NOT_FOUND_1, conn.createStatement()).
                executeQuery("SELECT 1 FROM sysibm.sysdummy1");
        conn.close();
        conn = getConnection("compatibility;MODE=DB2");
        Statement stmt = conn.createStatement();
        stmt.execute("drop table test");
        stmt.execute("create table test(id varchar)");
        stmt.execute("insert into test values ('3'),('1'),('2')");
        res = stmt.executeQuery("select id from test order by id fetch next 2 rows only");
        conn = getConnection("compatibility");
        res.next();
        assertEquals("1", res.getString(1));
        res.next();
        assertEquals("2", res.getString(1));
        assertFalse(res.next());
    }

    private void testDerby() throws SQLException {
        conn = getConnection("compatibility;MODE=Derby");
        ResultSet res = conn.createStatement().executeQuery("SELECT 1 FROM sysibm.sysdummy1 fetch next 1 row only");
        res.next();
        assertEquals("1", res.getString(1));
        conn.close();
        conn = getConnection("compatibility;MODE=PostgreSQL");
        assertThrows(ErrorCode.SCHEMA_NOT_FOUND_1, conn.createStatement()).
                executeQuery("SELECT 1 FROM sysibm.sysdummy1");
        conn.close();
        conn = getConnection("compatibility");
    }
}
