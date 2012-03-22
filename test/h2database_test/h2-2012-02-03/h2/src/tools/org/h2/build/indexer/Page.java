/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.build.indexer;

/**
 * Represents a page of the indexer.
 */
public class Page {

    /**
     * The page id.
     */
    int id;

    /**
     * The file name.
     */
    String fileName;

    /**
     * The title of the page.
     */
    String title;

    /**
     * The total weight of this page.
     */
    // TODO page.totalWeight is currently not used
    int totalWeight;

    /**
     * The number of relations between a page and a word.
     */
    int relations;

    Page(int id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }

    public String toString() {
        return "p" + id + "(" + fileName + ")";
    }

}
