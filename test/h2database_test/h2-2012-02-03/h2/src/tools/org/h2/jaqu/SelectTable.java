/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jaqu;

import java.util.ArrayList;
import org.h2.jaqu.util.ClassUtils;
import org.h2.util.New;

/**
 * This class represents a table in a query.
 *
 * @param <T> the table class
 */
class SelectTable <T> {

    private static int asCounter;
    private Query<T> query;
    private Class<T> clazz;
    private T current;
    private String as;
    private TableDefinition<T> aliasDef;
    private boolean outerJoin;
    private ArrayList<Token> joinConditions = New.arrayList();
    private T alias;

    @SuppressWarnings("unchecked")
    SelectTable(Db db, Query<T> query, T alias, boolean outerJoin) {
        this.alias = alias;
        this.query = query;
        this.outerJoin = outerJoin;
        aliasDef = (TableDefinition<T>) db.getTableDefinition(alias.getClass());
        clazz = ClassUtils.getClass(alias);
        as = "T" + asCounter++;
    }

    T getAlias() {
        return alias;
    }

    T newObject() {
        return ClassUtils.newObject(clazz);
    }

    TableDefinition<T> getAliasDefinition() {
        return aliasDef;
    }

    void appendSQL(SQLStatement stat) {
        if (query.isJoin()) {
            stat.appendTable(aliasDef.schemaName, aliasDef.tableName).appendSQL(" AS " + as);
        } else {
            stat.appendTable(aliasDef.schemaName, aliasDef.tableName);
        }
    }

    void appendSQLAsJoin(SQLStatement stat, Query<T> q) {
        if (outerJoin) {
            stat.appendSQL(" LEFT OUTER JOIN ");
        } else {
            stat.appendSQL(" INNER JOIN ");
        }
        appendSQL(stat);
        if (!joinConditions.isEmpty()) {
            stat.appendSQL(" ON ");
            for (Token token : joinConditions) {
                token.appendSQL(stat, q);
                stat.appendSQL(" ");
            }
        }
    }

    boolean getOuterJoin() {
        return outerJoin;
    }

    Query<T> getQuery() {
        return query;
    }

    String getAs() {
        return as;
    }

    void addConditionToken(Token condition) {
        joinConditions.add(condition);
    }

    T getCurrent() {
        return current;
    }

    void setCurrent(T current) {
        this.current = current;
    }

}
