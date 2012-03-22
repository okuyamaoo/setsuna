/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.android;

import android.database.Cursor;

/**
 * A factory and event listener for cursors.
 */
public interface H2CursorDriver {

    /**
     * The cursor was closed.
     */
    void cursorClosed();

    /**
     * The cursor was deactivated.
     */
    void cursorDeactivated();

    /**
     * The query was re-run.
     *
     * @param cursor the old cursor
     */
    void cursorRequeried(Cursor cursor);

    /**
     * Execute the query.
     *
     * @param factory the cursor factory
     * @param bindArgs the parameter values
     * @return the cursor
     */
    Cursor query(H2Database.CursorFactory factory, String[] bindArgs);

    /**
     * Set the parameter values.
     *
     * @param bindArgs the parameter values.
     */
    void setBindArguments(String[] bindArgs);

}
