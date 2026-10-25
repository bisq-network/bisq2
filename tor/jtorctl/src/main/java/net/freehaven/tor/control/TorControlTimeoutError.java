// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.freehaven.tor.control;

import java.io.IOException;

/**
 * An exception raised when Tor tells us about an error.
 */
public class TorControlTimeoutError extends IOException {

    static final long serialVersionUID = 4;

    public TorControlTimeoutError(String s) {
        super(s);
    }
}

