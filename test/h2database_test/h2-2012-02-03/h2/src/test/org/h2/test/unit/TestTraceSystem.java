/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.unit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.h2.message.TraceSystem;
import org.h2.store.fs.FileUtils;
import org.h2.test.TestBase;

/**
 * Tests the trace system
 */
public class TestTraceSystem extends TestBase {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    public void test() throws Exception {
        testTraceDebug();
        testReadOnly();
        testAdapter();
    }

    private static void testAdapter() {
        TraceSystem ts = new TraceSystem(null);
        ts.setLevelFile(TraceSystem.ADAPTER);
        ts.getTrace("test").info("test");
        ts.close();
    }

    private void testTraceDebug() {
        TraceSystem ts = new TraceSystem(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ts.setSysOut(new PrintStream(out));
        ts.setLevelSystemOut(TraceSystem.DEBUG);
        ts.getTrace("test").debug(new Exception("error"), "test");
        ts.close();
        String outString = new String(out.toByteArray());
        assertContains(outString, "error");
        assertContains(outString, "Exception");
        assertContains(outString, "test");
    }

    private void testReadOnly() throws Exception {
        String readOnlyFile = getBaseDir() + "/readOnly.log";
        FileUtils.delete(readOnlyFile);
        FileUtils.newOutputStream(readOnlyFile, false).close();
        FileUtils.setReadOnly(readOnlyFile);
        TraceSystem ts = new TraceSystem(readOnlyFile);
        ts.setLevelFile(TraceSystem.INFO);
        ts.getTrace("test").info("test");
        FileUtils.delete(readOnlyFile);
        ts.close();
    }

}
