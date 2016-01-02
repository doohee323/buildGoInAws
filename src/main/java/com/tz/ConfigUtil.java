/*
 * Copyright (c) 2015 Fortinet.
 *
 * Please see LICENSE.TXT for licensing terms of this software.
 */

package com.tz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public final class ConfigUtil {

	static final Logger log = LoggerFactory.getLogger(ConfigUtil.class);

	public static Reader getFileReader(String fileNm) {
		Reader reader = null;
		try {
			reader = new InputStreamReader(getFileInputStream(fileNm));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return reader;
	}

	public static InputStream getFileInputStream(String fileNm) {
		try {
			if (new File(fileNm).exists()) {
				return new FileInputStream(fileNm);
			} else {
				return ConfigUtil.class.getClassLoader().getResourceAsStream(fileNm);
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getConfPath(String fileNm) {
		try {
			if (new File(fileNm).exists()) {
				return fileNm;
			} else {
				return ConfigUtil.class.getClassLoader().getResource(fileNm).getPath();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Properties getJsonProperty(String fileNm) {
		final Properties appProperty = new Properties();
		Reader reader = null;
		try {
			reader = getFileReader(fileNm);
			final JSONObject json = (JSONObject) new JSONParser().parse(reader);
			loadJson(appProperty, "", json);
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return appProperty;
	}

	public static void loadJson(Properties appProperty, String parent, JSONObject json) {
		for (final Iterator<?> iterator = json.keySet().iterator(); iterator.hasNext();) {
			final String key = (String) iterator.next();
			if (json.get(key) instanceof JSONObject) {
				loadJson(appProperty, key, (JSONObject) json.get(key));
			} else {
				appProperty.put(parent + "." + key, json.get(key));
			}
		}
	}

	public static Properties getProperty(String fileNm) {
		final Properties appProperty = new Properties();
		try {
			appProperty.load(getFileReader(fileNm));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return appProperty;
	}

	public static Properties getProperties(String fileNm, String ext) {
		final Properties appProperties = new Properties();
		try {
			final File file = new File(getConfPath(fileNm));
			final File afile[] = file.listFiles();
			for (int i = 0; i < afile.length; i++) {
				if (afile[i].getName().endsWith("." + ext)) {
					final Properties props = new Properties();
					props.load(new FileInputStream(afile[i].getAbsoluteFile().toString()));
					for (final Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
						final String key = (String) en.nextElement();
						final String value = props.getProperty(key);
						appProperties.setProperty(key, value);
					}
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return appProperties;
	}

	public static Properties getProperties(String path) {
		return getProperties(path, "conf");
	}

	static String version = null;

	public static String getVersion(String filePath) {
		try {
			final Reader reader = getFileReader(filePath);
			try {
				final StringBuilder sb = new StringBuilder();
				int data = reader.read();
				while (data != -1) {
					final char dataChar = (char) data;
					sb.append(dataChar);
					data = reader.read();
				}
				return sb.toString().trim();
			} finally {
				reader.close();
			}
		} catch (final Exception e) {
			log.warn("error: " + e.getMessage());
			e.printStackTrace();
		}
		return version;
	}

	public static void setLogbackConfig(String filePath) {
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			final JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			configurator.doConfigure(ConfigUtil.getConfPath(filePath));
		} catch (final JoranException je) {
			System.out.println(je.getMessage());
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

}
