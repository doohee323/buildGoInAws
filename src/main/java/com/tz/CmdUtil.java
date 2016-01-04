package com.tz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * </pre>
 * 
 * @version 1.0
 */
public class CmdUtil {

	private static Logger logger = LoggerFactory.getLogger(CmdUtil.class);

	/**
	 * <pre>
	 * </pre>
	 * 
	 * @param request
	 * @param response
	 */
	public static boolean checkUnix() {
		String osName = " " + System.getProperty("os.name");
		logger.debug("checkUnix command:" + osName);
		if (osName.indexOf("Solaris") > 0)
			return true;
		if (osName.indexOf("AIX") > 0)
			return true;
		if (osName.indexOf("Unix") > 0)
			return true;
		if (osName.indexOf("HP-UX") > 0)
			return true;
		if (osName.indexOf("Linux") > 0)
			return true;
		return false;
	}

	/**
	 * <pre>
	 * execCommand(HashMap input)
	 * </pre>
	 * 
	 * @param input
	 * @throws Exception
	 */
	public static void execCommand(HashMap<String, String> input) throws Exception {
		String command = input.get("command").toString();
		logger.debug("execUnixCommand command:" + command);

		try {
			Runtime rt = Runtime.getRuntime();
			rt.exec(command);
		} catch (Exception e) {
			logger.error("execCommand return1 :" + e.getMessage());
			throw new Exception("execCommand command error 1!!!:" + command);
		}
	}

	/**
	 * <pre>
	 * </pre>
	 * 
	 * @param command
	 *            : commmand
	 * @return
	 * @throws Exception
	 */
	public static String execCommand(String command) throws Exception {
		logger.debug("execUnixCommand command:" + command);
		// ProcessBuilder pb = new ProcessBuilder();
		// Map<String, String> envMap = pb.environment();
		// Set<String> keys = envMap.keySet();
		// for (String key : keys) {
		// logger.debug(key + " ==> " + envMap.get(key));
		// }
		StringBuffer strReturn = new StringBuffer();
		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		try {
			String[] cmd = { "/bin/bash", "-c", command };
			ps = rt.exec(cmd);
			int n = ps.waitFor();
			logger.debug("exec result code:" + n);
		} catch (Exception e) {
			logger.error("execUnixCommand return1 :" + e.getMessage());
			throw new Exception("execUnixCommand command error 1!!!:" + command);
		}

		if (ps.exitValue() == 0) {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new SequenceInputStream(ps.getInputStream(), ps.getErrorStream())));
			try {
				String readLine = null;
				while ((readLine = br.readLine()) != null) {
					strReturn.append(readLine).append("\n");
				}
			} catch (IOException e) {
				logger.error("execUnixCommand return2 :" + e.getMessage());
				throw new Exception("execUnixCommand command error 2!!!:" + command);
			}
			logger.debug("execUnixCommand return :" + strReturn);
		}
		return strReturn.toString();
	}

	/**
	 * <pre>
	 * </pre>
	 * 
	 * @param command
	 *            : commmand
	 * @return
	 * @throws Exception
	 */
	public static String execRemoteCommand(String command) throws Exception {
		logger.debug("execUnixCommand command:" + command);
		// ProcessBuilder pb = new ProcessBuilder();
		// Map<String, String> envMap = pb.environment();
		// Set<String> keys = envMap.keySet();
		// for (String key : keys) {
		// logger.debug(key + " ==> " + envMap.get(key));
		// }
		StringBuffer strReturn = new StringBuffer();
		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		try {
			String[] cmd = { "/bin/bash", "-c", command };
			ps = rt.exec(cmd);
			int n = ps.waitFor();
			logger.debug("exec result code:" + n);
		} catch (Exception e) {
			logger.error("execUnixCommand return1 :" + e.getMessage());
			throw new Exception("execUnixCommand command error 1!!!:" + command);
		}

		if (ps.exitValue() == 0) {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new SequenceInputStream(ps.getInputStream(), ps.getErrorStream())));
			try {
				String readLine = null;
				while ((readLine = br.readLine()) != null) {
					strReturn.append(readLine).append("\n");
				}
			} catch (IOException e) {
				logger.error("execUnixCommand return2 :" + e.getMessage());
				throw new Exception("execUnixCommand command error 2!!!:" + command);
			}
			logger.debug("execUnixCommand return :" + strReturn);
		}
		return strReturn.toString();
	}

	public static String loadStream(InputStream s) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(s));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null)
			sb.append(line).append("\n");
		return sb.toString();
	}
}
