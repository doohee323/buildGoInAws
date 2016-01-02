package com.tz;

/**
 * Copyright (c) 2016 tz, Inc.
 *
 * See LICENSE.txt for licensing terms covering this software.
 *
 **/

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public final class TestGoBuild extends TestCase {
    static final Logger log = LoggerFactory.getLogger(TestGoBuild.class);

    @Test
    public void testStatsObject() throws IOException, FileNotFoundException {
    	GoBuild build = new GoBuild();
    	build.main(null);
    }
}
