/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jaqu.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * Messages used in the database engine.
 * Use the PropertiesToUTF8 tool to translate properties files to UTF-8 and back.
 * If the word 'SQL' appears then the whole SQL statement must be a parameter,
 * otherwise this may be added: '; SQL statement: ' + sql
 */
public class Message {

    private int todoDelete;

    private Message() {
        // utility class
    }

    /**
     * Convert an exception to a SQL exception using the default mapping.
     *
     * @param e the root cause
     * @return the SQL exception object
     */
    public static SQLException convert(Throwable e) {
        if (e instanceof SQLException) {
            return (SQLException) e;
        }
        String message;
        if (e instanceof InvocationTargetException) {
            InvocationTargetException te = (InvocationTargetException) e;
            Throwable t = te.getTargetException();
            if (t instanceof SQLException) {
                return (SQLException) t;
            }
            message = "Invocation exception";
        } else if (e instanceof IOException) {
            message = "IO exception";
        } else {
            message = "General exception";
        }
        SQLException e2 = new SQLException(message + ": " + e.toString());
        e2.initCause(e);
        return e2;
    }

}
