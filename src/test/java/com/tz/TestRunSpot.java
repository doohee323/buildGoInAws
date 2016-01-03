package com.tz;

/**
 * Copyright (c) 2016 tz, Inc.
 *
 **/

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public final class TestRunSpot extends TestCase {
	static final Logger log = LoggerFactory.getLogger(TestRunSpot.class);

	@Test
	public void testStatsObject() throws IOException, FileNotFoundException {
		RunSpot.main(null);
	}
}
