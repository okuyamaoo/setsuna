/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.samples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.api.DatabaseEventListener;
import org.h2.jdbc.JdbcConnection;

/**
 * This example application implements a database event listener.
 * This is useful to display progress information while opening a large database,
 * or to log database exceptions.
 */
public class ShowProgress implements DatabaseEventListener {

    private long last, start;

    /**
     * Create a new instance of this class, and start the timer.
     */
    public ShowProgress() {
        start = last = System.currentTimeMillis();
    }

    /**
     * This method is called when executing this sample application from the
     * command line.
     *
     * @param args the command line parameters
     */
    public static void main(String... args) throws Exception {
        new ShowProgress().test();
    }

    /**
     * Run the progress test.
     */
    void test() throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:test", "sa", "");
        Statement stat = conn.createStatement();
        stat.execute("DROP TABLE IF EXISTS TEST");
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR)");
        PreparedStatement prep = conn.prepareStatement("INSERT INTO TEST VALUES(?, 'Test' || SPACE(100))");
        long time;
        time = System.currentTimeMillis();
        int len = 1000;
        for (int i = 0; i < len; i++) {
            long now = System.currentTimeMillis();
            if (now > time + 1000) {
                time = now;
                System.out.println("Inserting " + (100L * i / len) + "%");
            }
            prep.setInt(1, i);
            prep.execute();
        }
        boolean abnormalTermination = true;
        if (abnormalTermination) {
            ((JdbcConnection) conn).setPowerOffCount(1);
            try {
                stat.execute("INSERT INTO TEST VALUES(-1, 'Test' || SPACE(100))");
            } catch (SQLException e) {
                // ignore
            }
        } else {
            conn.close();
        }

        System.out.println("Open connection...");
        time = System.currentTimeMillis();
        conn = DriverManager.getConnection("jdbc:h2:test;DATABASE_EVENT_LISTENER='" + getClass().getName() + "'", "sa", "");
        time = System.currentTimeMillis() - time;
        System.out.println("Done after " + time + " ms");
        prep.close();
        stat.close();
        conn.close();

    }

    /**
     * This method is called if an exception occurs in the database.
     *
     * @param e the exception
     * @param sql the SQL statement
     */
    public void exceptionThrown(SQLException e, String sql) {
        System.out.println("Error executing " + sql);
        e.printStackTrace();
    }

    /**
     * This method is called when opening the database to notify about the progress.
     *
     * @param state the current state
     * @param name the object name (depends on the state)
     * @param current the current progress
     * @param max the 100% mark
     */
    public void setProgress(int state, String name, int current, int max) {
        long time = System.currentTimeMillis();
        if (time < last + 5000) {
            return;
        }
        last = time;
        String stateName = "?";
        switch (state) {
        case STATE_SCAN_FILE:
            stateName = "Scan " + name;
            break;
        case STATE_CREATE_INDEX:
            stateName = "Create Index " + name;
            break;
        case STATE_RECOVER:
            stateName = "Recover";
            break;
        }
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // ignore
        }
        System.out.println("State: " + stateName + " " + (100 * current / max) + "% (" + current + " of " + max + ") "
                + (time - start) + " ms");
    }

    /**
     * This method is called when the database is closed.
     */
    public void closingDatabase() {
        System.out.println("Closing the database");
    }

    /**
     * This method is called just after creating the instance.
     *
     * @param url the database URL
     */
    public void init(String url) {
        System.out.println("Initializing the event listener for database " + url);
    }

    /**
     * This method is called when the database is open.
     */
    public void opened() {
        // do nothing
    }

}
