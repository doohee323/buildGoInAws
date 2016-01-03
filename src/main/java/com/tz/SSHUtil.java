package com.tz;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;

public class SSHUtil {

	private Logger logger = LoggerFactory.getLogger(SSHUtil.class);
	public final int maxWait = 20;

	public static void main(String[] arg) {
		String username = "ec2-user";
		String host = "ec2-54-162-101-184.compute-1.amazonaws.com";
		String pem_file = "/securedKeys/awskey1.pem";

		Map<String, String> hostInfo = new HashMap<String, String>();
		hostInfo.put("username", username);
		hostInfo.put("host", host);
		hostInfo.put("pem_file", pem_file);

		List<String> commands = new ArrayList<String>();
		// commands.add(""); upload file
		commands.add("sudo su");
		commands.add("su - tajo");
		commands.add("cd /home/tajo/tajo/bin/");
		commands.add("./tsql");

		String sql = "CREATE EXTERNAL TABLE dual_cnt (CNT text)";
		sql += "USING CSV LOCATION 's3n://tz-tajo/test/dual_cnt';";
		commands.add(sql);

		sql = "DROP TABLE dual_cnt;";
		commands.add(sql);

		sql = "INSERT INTO dual_cnt SELECT 1 AS CNT from USERS limit 1";
		commands.add(sql);

		SSHUtil util = new SSHUtil();
		util.shell(hostInfo, commands);
	}

	public String shell(Map<String, String> hostInfo, List<String> commands) {
		String result = "";
		try {
			String username = hostInfo.get("username");
			String host = hostInfo.get("host");
			String pem_file = hostInfo.get("pem_file");
			int port = 22;
			File privateKey = new File(pem_file);

			com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
			jsch.addIdentity(privateKey.getAbsolutePath());
			com.jcraft.jsch.Session session = jsch.getSession(username, host, port);
			session.setDaemonThread(true);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(out);

			Channel channel = session.openChannel("shell");
			channel.setOutputStream(ps);
			// channel.setOutputStream(System.out);
			PrintStream shellStream = new PrintStream(channel.getOutputStream()); // printStream
			channel.connect();

			String currentLog = "";
			for (String command : commands) {
				if (!command.equals("")) {
					result = out.toString();
					if (command.startsWith("wait ")) {
						String offset = command.substring("wait ".length(), command.length()).replaceAll("'", "");
						currentLog = wait(out, result, offset, currentLog);
					} else {
						if (command.startsWith("sleep ")) {
							String strTime = StringUtil.Trim(command.substring("sleep ".length(), command.length()));
							Thread.sleep(Integer.parseInt(strTime) * 1000);
						} else {
							shellStream.println(command);
							shellStream.flush();
							Thread.sleep(1000);
						}
						String showStr = result.substring(currentLog.length(), result.length()).trim();
						logger.info(showStr);
						currentLog = result;
					}
				}
			}
			currentLog = wait(out, result, "finish!", currentLog);
			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			logger.error("ERROR: Connecting via shell to " + e.toString());
			e.printStackTrace();
		}
		return result;
	}

	public String wait(ByteArrayOutputStream out, String result, String offset, String currentLog)
			throws InterruptedException {
		boolean bGo = true;
		int nCnt = 0;
		while (bGo) {
			Thread.sleep(5000);
			result = out.toString();
			if (result.indexOf(offset) > -1) {
				bGo = false;
			}
			if (nCnt > maxWait) {
				bGo = false;
			}
			String showStr = result.substring(currentLog.length(), result.length()).trim();
			logger.info(showStr);
			currentLog = result;
			nCnt++;
		}
		return currentLog;
	}

	public void exec(Map<String, String> hostInfo, String command) {
		String username = hostInfo.get("username");
		String host = hostInfo.get("host");
		String pem_file = hostInfo.get("pem_file");
		int port = 22;
		File privateKey = new File(pem_file);

		try {
			com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
			jsch.addIdentity(privateKey.getAbsolutePath());
			com.jcraft.jsch.Session session = jsch.getSession(username, host, port);
			session.setDaemonThread(true);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();

			com.jcraft.jsch.Channel channel = session.openChannel("exec");
			// Channel channel=session.openChannel("shell"); exec
			FileOutputStream fos = new FileOutputStream("/data3/logs/a.log", true);
			channel.setOutputStream(fos);

			((com.jcraft.jsch.ChannelExec) channel).setCommand(command);

			// X Forwarding
			// channel.setXForwarding(true);

			// channel.setInputStream(System.in);
			channel.setInputStream(null);

			// FileOutputStream fos=new FileOutputStream("/tmp/stderr");
			// ((ChannelExec)channel).setErrStream(fos);
			((com.jcraft.jsch.ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					logger.info("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
			session.disconnect();

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

}