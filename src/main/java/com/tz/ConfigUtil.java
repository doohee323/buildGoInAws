/*
 * Copyright (c) 2016.
 *
 */

package com.tz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigUtil {

	private static Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

	public static Properties getProperty(String fileNm) {
		final Properties appProperty = new Properties();
		try {
			appProperty.load(getFileReader(fileNm));
		} catch (final Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return appProperty;
	}

	public static Reader getFileReader(String fileNm) {
		Reader reader = null;
		try {
			reader = new InputStreamReader(getFileInputStream(fileNm));
		} catch (final Exception e) {
			logger.error(e.getMessage());
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
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static Properties parseJson(String fileNm) {
		final Properties appProperty = new Properties();
		Reader reader = null;
		try {
			reader = getFileReader(fileNm);
			final JSONObject json = (JSONObject) new JSONParser().parse(reader);
			loadJson(appProperty, null, json);
		} catch (final Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				logger.error(e.getMessage());
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
				if (parent == null) {
					appProperty.put(key, json.get(key));
				} else {
					appProperty.put(parent + "." + key, json.get(key));
				}
			}
		}
	}

	public static JSONArray getConfig(String xmlFile) {
		try {
			Properties pr = parseJson(xmlFile);
			return (JSONArray) pr.get("list");
		} catch (Exception e) {
			logger.error("getConfig error!: " + e.getMessage());
		}
		return null;
	}

	public static String getProperty(List<Map<String, Object>> mConfig, String key) {
		for (int i = 0; i < mConfig.size(); i++) {
			if (mConfig.get(i).get("key").toString().equals(key)) {
				return mConfig.get(i).get("value").toString();
			}
		}
		return null;
	}

	public static String encode(String str) {
		str = str.replaceAll(" < ", " µ ").replaceAll(" <= ", " µ= ").replaceAll(" > ", " ± ")
				.replaceAll(" >= ", " ±= ").replaceAll("&", "¶");
		str = StringUtil.Trim(str);
		return str;
	}

	public static String decode(String str) {
		str = str.replaceAll(" µ ", " < ").replaceAll(" µ= ", " <= ").replaceAll(" ± ", " > ")
				.replaceAll(" ±= ", " >= ").replaceAll("¶", "&").replaceAll("``", "\"");
		str = StringUtil.Trim(str);
		return str;
	}

	/**
	 * <pre>
	 * </pre>
	 */
	public static StringBuffer getFromFile(String fileName, String strChar) throws IOException {
		if (strChar == null) {
			Scanner scanner = new Scanner(new File(fileName)).useDelimiter("\\Z");
			String contents = scanner.next();
			scanner.close();
			return new StringBuffer(contents);
		}

		if (strChar.equals(""))
			strChar = null;

		StringBuffer sb = new StringBuffer(1000);
		InputStreamReader is = null;
		BufferedReader in = null;
		String lineSep = System.getProperty("line.separator");

		try {
			File f = new File(fileName);
			if (f.exists()) {
				if (strChar != null)
					is = new InputStreamReader(new FileInputStream(f), strChar);
				else
					is = new InputStreamReader(new FileInputStream(f));
				in = new BufferedReader(is);
				String str = "";

				int readed = 0;
				while ((str = in.readLine()) != null) {
					if (strChar != null)
						readed += (str.getBytes(strChar).length);
					else
						readed += (str.getBytes().length);
					sb.append(str + lineSep);
				}
			}
		} catch (Exception e) {
			logger.error(e.toString());
		} finally {
			if (is != null)
				is.close();
			if (in != null)
				in.close();
		}
		return sb;
	}
}
