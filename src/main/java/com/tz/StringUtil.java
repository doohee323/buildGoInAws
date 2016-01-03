package com.tz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * </pre>
 * 
 * @version 1.0
 */
public class StringUtil {

	private Logger logger = LoggerFactory.getLogger(StringUtil.class);

	/**
	 * <pre>
	 * </pre>
	 * 
	 * @param data
	 */
	public static String getText(Object data) {
		if (data == null)
			data = "";
		if (data instanceof String) {
			return data.toString();
		} else if (data instanceof java.lang.String[]) {
			return StringUtil.arrayToString((String[]) data, ",");
		} else {
			return data.toString();
		}
	}

	/**
	 * <pre>
	 * </pre>
	 * 
	 * @param values
	 * @param gubun
	 * @return
	 */
	public static String arrayToString(String[] values, String gubun) {
		StringBuffer sb = new StringBuffer();
		if (values == null || values.length < 1)
			return "";
		sb.append(values[0]);
		for (int i = 1; i < values.length; i++) {
			sb.append(gubun).append(values[i]);
		}
		return sb.toString();
	}

	/**
	 * <pre>
	 * </pre>
	 */
	public static String quoteReplacement(String s) {
		if ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1))
			return s;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				sb.append('\\');
				sb.append('\\');
			} else if (c == '$') {
				sb.append('\\');
				sb.append('$');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * <pre>
	 * remove all superfluous whitespaces in source string
	 * </pre>
	 * 
	 * @param source
	 * @return
	 */
	public static String Trim(String source) {
		return ITrim(LTrim(RTrim(source)));
	}

	/**
	 * <pre>
	 * Left Trim
	 * </pre>
	 * 
	 * @param source
	 * @return
	 */
	public static String LTrim(String source) {
		return source.replaceAll("^\\s+", "");
	}

	/**
	 * <pre>
	 * Right Trim
	 * </pre>
	 * 
	 * @param source
	 * @return
	 */
	public static String RTrim(String source) {
		return source.replaceAll("\\s+$", "");
	}

	/**
	 * <pre>
	 * replace multiple whitespaces between words with single blank
	 * </pre>
	 * 
	 * @param source
	 * @return
	 */
	public static String ITrim(String source) {
		return source.replaceAll("\\b\\s{2,}\\b", " ");
	}

	/**
	 * <pre>
	 * Left / Right Trim
	 * </pre>
	 * 
	 * @param source
	 * @return
	 */
	public static String LRTrim(String source) {
		return LTrim(RTrim(source));
	}

	/**
	 * <pre>
	 * htmlToString(String s)
	 * </pre>
	 * 
	 * @param s
	 * @return
	 */
	public static String htmlToString(String s) {
		if (s == null)
			return null;
		try {
			s = StringUtil.replace(s, "&amp;", "&");
			s = StringUtil.replace(s, "&lt;", "<");
			s = StringUtil.replace(s, "&gt;", ">");
			s = StringUtil.replace(s, "&quot;", "\"");
			s = StringUtil.replace(s, "&#039;", "\'");
		} catch (Exception e) {
		}
		return s;
	}

	/**
	 * <pre>
	 * </pre>
	 */
	public static String replace(String strTarget, String strSearch, String strReplace) throws Exception {
		String result = null;
		try {

			String strCheck = new String(strTarget);
			StringBuffer strBuf = new StringBuffer();
			while (strCheck.length() != 0) {
				int begin = strCheck.indexOf(strSearch);
				if (begin == -1) {
					strBuf.append(strCheck);
					break;
				} else {
					int end = begin + strSearch.length();
					strBuf.append(strCheck.substring(0, begin));
					strBuf.append(strReplace);
					strCheck = strCheck.substring(end);
				}
			}
			result = strBuf.toString();
		} catch (Exception e) {
			throw new Exception("[StringUtil][replace]" + e.getMessage(), e);
		}
		return result;
	}
}