/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.build.indexer;

/**
 * Represents a weight of a token in a page.
 */
public class Weight {

    /**
     * The weight of a word in a title.
     */
    static final int TITLE = 10000;

    /**
     * The weight of a word in the header.
     */
    static final int HEADER = 100;

    /**
     * The weight of a word in a paragraph.
     */
    static final int PARAGRAPH = 1;

    /**
     * The page referenced.
     */
    Page page;

    /**
     * The weight value.
     */
    int value;

    public String toString() {
        return "" + value;
    }

}
