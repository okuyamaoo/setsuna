/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.fulltext;

/**
 * The settings of one full text search index.
 */
public class IndexInfo {

    /**
     * The index id.
     */
    protected int id;

    /**
     * The schema name.
     */
    protected String schema;

    /**
     * The table name.
     */
    protected String table;

    /**
     * The column indexes of the key columns.
     */
    protected int[] keys;

    /**
     * The column indexes of the index columns.
     */
    protected int[] indexColumns;

    /**
     * The column names.
     */
    protected String[] columns;
}
