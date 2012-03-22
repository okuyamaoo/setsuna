/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jaqu;

/**
 * An OR or an AND condition.
 */
enum ConditionAndOr implements Token {
    AND("AND"),
    OR("OR");

    private String text;

    ConditionAndOr(String text) {
        this.text = text;
    }

    public <T> void appendSQL(SQLStatement stat, Query<T> query) {
        stat.appendSQL(text);
    }

}
