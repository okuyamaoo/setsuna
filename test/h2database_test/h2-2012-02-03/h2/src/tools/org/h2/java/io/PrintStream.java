/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.java.io;

/**
 * A print stream.
 */
public class PrintStream {

    /**
     * Print the given string.
     *
     * @param s the string
     */
    public void println(String s) {
        // c: int x = LENGTH(s->chars);
        // c: printf("%.*S\n", x, s->chars);
    }

}
