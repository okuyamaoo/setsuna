/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.android;

/**
 * Utility methods.
 */
public class H2Utils {

    /**
     * A replacement for Context.openOrCreateDatabase.
     *
     * @param name the database name
     * @param mode the access mode
     * @param factory the cursor factory to use
     * @return the database connection
     */
    public static H2Database openOrCreateDatabase(String name, int mode, H2Database.CursorFactory factory) {
        return H2Database.openOrCreateDatabase(name, factory);
    }

}
