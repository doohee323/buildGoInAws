package com.tz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.LoggerFactory;

public class GoBuild {

	private org.slf4j.Logger Logger = LoggerFactory.getLogger(GoBuild.class);

	private String accesskey = "";
	private String secretkey = "";

	private int nInstanceNum = 0;
	private String common_spec = "";
	private String spot_spec = "";

	private String ami_id = "";
	private String spot_price = "";

	private String key = "";
	private String security_group = "";
	private String region = "";

	private String username = "";
	private String pem_file = "";

	private List<String> instanceIds = new ArrayList<String>();

	// 0) create master
	public static void main(String[] arg) {
		try {
			String instanceType = "spot"; // common / spot
			GoBuild golang = new GoBuild();
			golang.init();
			golang.getInstance(instanceType);
			golang.runApp("BUILD");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void init() {
		String logConfigFile = "golang.property";
		Properties config = ConfigUtil.getProperty(logConfigFile);

		accesskey = config.getProperty("accesskey");
		secretkey = config.getProperty("secretkey");

		String instanceNum = config.getProperty("instanceNum");
		nInstanceNum = Integer.parseInt(instanceNum);
		common_spec = config.getProperty("common_spec");
		spot_spec = config.getProperty("spot_spec");

		ami_id = config.getProperty("ami_id");
		spot_price = config.getProperty("spot_price");

		key = config.getProperty("key");
		security_group = config.getProperty("security_group");
		region = config.getProperty("region");

		username = config.getProperty("username");
		pem_file = config.getProperty("pem_file");
	}

	public int getInstance(String instanceType) {
		try {
			// 0. instance exist check
			boolean nReady = false;
			int nCode = checkInstance(instanceType);
			if (nCode < nInstanceNum) {
				// 1. create instance
				int cnt = nInstanceNum - nCode;
				String cmd = "";
				if (instanceType.equals("common")) {
					cmd = "ec2-run-instances -O " + accesskey + " -W " + secretkey;
					cmd += " --region " + region + " " + ami_id + " -g " + security_group + " -n " + cnt + " -k " + key;
					cmd += " -t " + common_spec;
				} else if (instanceType.equals("spot")) {
					cmd = "ec2-request-spot-instances -O " + accesskey + " -W " + secretkey + " --price " + spot_price;
					cmd += " --region " + region + " " + ami_id + " -g " + security_group + " -n " + cnt + " -k " + key
							+ " -t " + spot_spec;
				}

				CmdUtil.execUnixCommand(cmd);
				Thread.sleep(60000); // 3 minutes
				// 2. checking
				nCode = 0;
				int nMaxCheck = 3, nCnt = 0;
				while (nCode < nInstanceNum && nCnt < nMaxCheck) {
					nCode = checkInstance(instanceType);
					nCnt++;
					if (nCode == -1) {
						return nCode; // error!
					}
					if (nCode == nInstanceNum) {
						nReady = true;
					} else {
						Thread.sleep(60000); // 1 minutes
					}
				}
			} else if (nCode == nInstanceNum) {
				nReady = true;
			}
			if (nReady) {
				return nInstanceNum;
			} else {
				return nCode;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	// 3. execute app.
	public void runApp(String arg) throws Exception {
		Map<String, String> hostInfo = new HashMap<String, String>();
		hostInfo.put("username", username);
		hostInfo.put("key_file", pem_file);

		// 4. execute script.
		for (int i = 0; i < instanceIds.size(); i++) {
			hostInfo.put("host", getInstanceId(instanceIds.get(i)));
			List<String> commands = new ArrayList<String>();
			String cmd2 = "ls -al";
			commands.add(cmd2);
			commands.add("finish!");

			SSHUtil util = new SSHUtil();
			util.shell(hostInfo, commands);
		}

		// 5. delete instances
		for (int i = 0; i < instanceIds.size(); i++) {
			String str = instanceIds.get(i).toString();
			if (str.indexOf("\tfulfilled\t") > -1) {
				String instanceId = str.split("\t")[11];
				if (instanceId.trim().startsWith("i-")) {
					String cmd = "ec2-terminate-instances " + instanceId;
					System.out.println(cmd);
					CmdUtil.execUnixCommand(cmd);
				}
			}
		}
	}

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

	private String getInstanceId(String instanceId) {
		String rslt = null;
		try {
			String cmd = "ec2-describe-instances " + instanceId + " --region=" + region;
			rslt = CmdUtil.execUnixCommand(cmd);
		} catch (Exception e) {
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
				if (!arry[i].equals("") && arry[i].indexOf("terminated") == -1) {
					if (instanceType.equals("common")) {
						arry[i] = arry[i].substring(arry[i].indexOf("INSTANCE") + "INSTANCE ".length(),
								arry[i].length());
					} else if (instanceType.equals("spot")) {
						arry[i] = arry[i].split("\t")[11];
					}
					instanceIds.add(arry[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (instanceIds.size() == 0) {
			return 0;
		} else {
			if (instanceIds.size() == nInstanceNum) {
				return nInstanceNum;
			}
		}
		return -1;
	}
}
