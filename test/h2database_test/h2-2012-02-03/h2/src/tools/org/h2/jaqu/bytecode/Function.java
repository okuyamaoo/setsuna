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
 * A method call.
 */
class Function implements Token {

    private final String name;
    private final Token expr;

    Function(String name, Token expr) {
        this.name = name;
        this.expr = expr;
    }

    public String toString() {
        return name + "(" + expr + ")";
    }

    public <T> void appendSQL(SQLStatement stat, Query<T> query) {
        // untested
        stat.appendSQL(name + "(");
        expr.appendSQL(stat, query);
        stat.appendSQL(")");
    }
}
