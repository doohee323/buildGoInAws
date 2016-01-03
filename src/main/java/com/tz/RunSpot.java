package com.tz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunSpot {
	private static Logger logger = LoggerFactory.getLogger(RunSpot.class);

	final static int nMaxCheck = 20;
	final static int nWait = 60000; // 1 minutes

	private String accesskey = "";
	private String secretkey = "";

	private int instance_num = 0;
	private String common_spec = "";
	private String spot_spec = "";

	private String ami_id = "";
	private String spot_price = "";

	private String keypair = "";
	private String security_group = "";
	private String region = "";

	private String username = "";
	private String pem_file = "";
	private String cmd_file = "";

	private JSONArray cmdConfig = null;

	private List<String> instanceIds = new ArrayList<String>();

	// 0) create master
	public static void main(String[] arg) {
		try {
			RunSpot runSpot = new RunSpot();
			runSpot.init();
			runSpot.getInstance(arg[0]); // common / spot
			runSpot.runScript(arg[1]); // commands group id
			runSpot.terminateInstance();
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public void init() {
		String configFile = "application.property";
		Properties config = ConfigUtil.getProperty(configFile);

		accesskey = config.getProperty("accesskey");
		secretkey = config.getProperty("secretkey");

		instance_num = Integer.parseInt(config.getProperty("instance_num"));
		common_spec = config.getProperty("common_spec");
		spot_spec = config.getProperty("spot_spec");

		ami_id = config.getProperty("ami_id");
		spot_price = config.getProperty("spot_price");

		keypair = config.getProperty("keypair");
		security_group = config.getProperty("security_group");
		region = config.getProperty("region");

		username = config.getProperty("username");
		pem_file = config.getProperty("pem_file");
		cmd_file = config.getProperty("cmd_file");

		cmdConfig = ConfigUtil.getConfig(cmd_file);
	}

	public int getInstance(String instanceType) {
		try {
			// 0. instance exist check
			boolean bReady = false;
			int nCode = checkInstance(instanceType);
			if (nCode < instance_num) {
				// 1. create instance
				int cnt = instance_num - nCode;
				String cmd = "";
				if (instanceType.equals("common")) {
					cmd = "ec2-run-instances -O " + accesskey + " -W " + secretkey;
					cmd += " --region " + region + " " + ami_id + " -g " + security_group + " -n " + cnt + " -k "
							+ keypair;
					cmd += " -t " + common_spec;
				} else if (instanceType.equals("spot")) {
					cmd = "ec2-request-spot-instances -O " + accesskey + " -W " + secretkey + " --price " + spot_price;
					cmd += " --region " + region + " " + ami_id + " -g " + security_group + " -n " + cnt + " -k "
							+ keypair + " -t " + spot_spec;
				}

				CmdUtil.execUnixCommand(cmd);
				Thread.sleep(nWait * 3);
				// 2. checking
				nCode = 0;
				int nCnt = 0;
				while (nCode < instance_num && nCnt < nMaxCheck) {
					nCode = checkInstance(instanceType);
					nCnt++;
					if (nCode == -1) {
						return nCode; // error!
					}
					if (nCode == instance_num) {
						bReady = true;
					} else {
						Thread.sleep(nWait);
					}
				}
			} else if (nCode == instance_num) {
				bReady = true;
			}
			if (bReady) {
				return instance_num;
			} else {
				return nCode;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return -1;
	}

	// 3. execute app.
	public void runScript(String id) throws Exception {
		Map<String, String> hostInfo = new HashMap<String, String>();
		hostInfo.put("username", username);
		hostInfo.put("pem_file", pem_file);

		for (Object object : cmdConfig) {
			JSONObject aJson = (JSONObject) object;
			if (id.equals(aJson.get("id").toString())) {
				JSONArray commands = (JSONArray) aJson.get("commands");
				List<String> commands2 = new ArrayList<String>();
				for (Object cmd2 : commands) {
					logger.debug("=[input]=========" + cmd2.toString());
					commands2.add(cmd2.toString());
					commands2.add("finish!");
				}
				// 4. execute script.
				for (int i = 0; i < instanceIds.size(); i++) {
					String host = getInstanceId(instanceIds.get(i));
					logger.debug("=[host]=========" + host);
					if (!host.equals("")) {
						hostInfo.put("host", host);
						SSHUtil util = new SSHUtil();
						String output = util.shell(hostInfo, commands2);
						logger.debug("=[output]=========" + output);
					}
				}
			}
		}
	}

	// 5. delete instances
	public void terminateInstance() throws Exception {
		for (int i = 0; i < instanceIds.size(); i++) {
			String str = instanceIds.get(i).toString();
			if (str.indexOf("\tfulfilled\t") > -1) {
				String instanceId = str.split("\t")[11];
				String cmd = null;
				if (instanceId.trim().startsWith("i-")) {
					cmd = "ec2-terminate-instances " + instanceId + " --region=" + region;
					CmdUtil.execUnixCommand(cmd);
				}
				String requestId = str.split("\t")[1];
				cmd = "ec2-cancel-spot-instance-requests " + requestId + " --region=" + region;
				CmdUtil.execUnixCommand(cmd);
			}
		}
	}

	@SuppressWarnings("unused")
	private String replaceVariables(String orgStr, Map<String, Object> var) {
		if (var != null) {
			Object[] key = var.keySet().toArray();
			for (int i = 0; i < key.length; i++) {
				if (!((var.get((String) key[i])) instanceof String))
					continue;
				String value = (String) var.get((String) key[i]);
				value = value == null ? "" : value;
				orgStr = orgStr.replaceAll("\\$\\{" + ((String) key[i]) + "\\}", StringUtil.quoteReplacement(value));
			}
		}
		return orgStr;
	}

	private String getInstanceId(String instanceInfo) {
		String rslt = null;
		try {
			String instanceId = instanceInfo.split("\t")[11];
			String cmd = "ec2-describe-instances " + instanceId + " --region=" + region;
			rslt = CmdUtil.execUnixCommand(cmd);
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return rslt.split("\t")[6];
	}

	private int checkInstance(String instanceType) {
		// 1. checking exist of request
		try {
			String cmd = "", splitFlag = "";
			if (instanceType.equals("common")) {
				cmd = "ec2-describe-instances --filter \"tag:Name=golang_WORKER*\"";
				splitFlag = "RESERVATION";
			} else if (instanceType.equals("spot")) {
				cmd = "ec2-describe-spot-instance-requests --region " + region;
				splitFlag = "SPOTINSTANCEREQUEST";
			}
			String rslt = CmdUtil.execUnixCommand(cmd);
			String arry[] = rslt.split(splitFlag);
			for (int i = 0; i < arry.length; i++) {
				if (!arry[i].equals("") && arry[i].split("\t")[5].equals("active")) {
					if (instanceType.equals("common")) {
						arry[i] = arry[i].substring(arry[i].indexOf("INSTANCE") + "INSTANCE ".length(),
								arry[i].length());
					} else if (instanceType.equals("spot")) {
						arry[i] = arry[i].substring(0, arry[i].indexOf(" "));
					}
					instanceIds.add(arry[i]);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		if (instanceIds.size() == 0) {
			return 0;
		} else {
			if (instanceIds.size() == instance_num) {
				return instance_num;
			}
		}
		return -1;
	}
}
