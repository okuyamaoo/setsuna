/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.unit;

import java.sql.Connection;
import java.sql.Statement;
import org.h2.constant.ErrorCode;
import org.h2.test.TestBase;

/**
 * Test the ICU4J collator.
 */
public class TestCollation extends TestBase {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    public void test() throws Exception {
        deleteDb("collation");
        Connection conn = getConnection("collation");
        Statement stat = conn.createStatement();
        assertThrows(ErrorCode.INVALID_VALUE_2, stat).
                execute("set collation xyz");
        stat.execute("set collation en");
        stat.execute("set collation default_en");
        assertThrows(ErrorCode.CLASS_NOT_FOUND_1, stat).
                execute("set collation icu4j_en");
        conn.close();
        deleteDb("collation");
    }

}
