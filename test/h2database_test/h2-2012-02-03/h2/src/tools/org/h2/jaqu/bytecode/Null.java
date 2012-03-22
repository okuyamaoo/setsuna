/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jaqu.bytecode;

import org.h2.jaqu.Query;
import org.h2.jaqu.SQLStatement;
import org.h2.jaqu.Token;

/**
 * The Java 'null'.
 */
public class Null implements Token {

    static final Null INSTANCE = new Null();

    private Null() {
        // don't allow to create new instances
    }

    public String toString() {
        return "null";
    }

    public <T> void appendSQL(SQLStatement stat, Query<T> query) {
        // untested
        stat.appendSQL("NULL");
    }

}
