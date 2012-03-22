/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.android;

/**
 * This exception is thrown when a constraint was violated.
 */
public class H2ConstraintException extends H2Exception {

    private static final long serialVersionUID = 1L;

    H2ConstraintException() {
        super();
    }

    H2ConstraintException(String error) {
        super(error);
    }
}
