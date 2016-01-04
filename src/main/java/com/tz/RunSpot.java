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

@SuppressWarnings("unchecked")
public class RunSpot {
	private static Logger logger = LoggerFactory.getLogger(RunSpot.class);

	final static int nMaxCheck = 20;
	final static int nWait = 60000; // 1 minutes

	private static String awsInstId = "";
	private String accesskey = "";
	private String secretkey = "";

	private String cmd_file = "";

	private JSONObject cmdConfig = null;

	private JSONArray targets = null;
	private Map<String, Object> results = new HashMap<String, Object>();

	// 0) create master
	public static void main(String[] arg) {
		try {
			RunSpot runSpot = new RunSpot();
			awsInstId = arg[0];
			runSpot.init();
			runSpot.getInstance();
			runSpot.runScript();
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

		cmd_file = config.getProperty("cmd_file");

		JSONArray allConf = ConfigUtil.getConfig(cmd_file);

		for (Object object : allConf) {
			JSONObject aJson = (JSONObject) object;
			if (aJson.get(awsInstId) != null) {
				cmdConfig = (JSONObject) aJson.get(awsInstId);
				break;
			}
		}
		targets = (JSONArray) cmdConfig.get("target");
	}

	public void getInstance() {
		try {
			// 0. instance exist check
			for (Object object : targets) {
				boolean bReady = false;
				JSONObject target = (JSONObject) object;
				String instance_type = target.get("instance_type").toString();
				String cli_args = target.get("cli_args").toString();
				String region = target.get("region").toString();
				String inst_spec = target.get("inst_spec").toString();
				int instance_num = Integer.parseInt(target.get("instance_num").toString());
				String ami_id = target.get("ami_id").toString();

				String keypair = target.get("keypair").toString();
				String security_group = target.get("security_group").toString();

				List<String> instanceInfos = checkInstance(target);
				int nCode = instanceInfos.size();
				if (nCode < instance_num) {
					// 1. create instance
					int cnt = instance_num - nCode;
					String cmd = "";
					if (instance_type.equals("common")) {
						cmd = "ec2-run-instances -O " + accesskey + " -W " + secretkey;
						cmd += " --region " + region + " " + ami_id + " -g " + security_group + " -n " + cnt + " -k "
								+ keypair;
						cmd += " -t " + inst_spec;
					} else if (instance_type.equals("spot")) {
						cmd = "ec2-request-spot-instances -O " + accesskey + " -W " + secretkey + " " + cli_args;
						cmd += " --region " + region + " " + ami_id + " -g " + security_group + " -n " + cnt + " -k "
								+ keypair + " -t " + inst_spec;
					}

					CmdUtil.execCommand(cmd);
					Thread.sleep(nWait * 3);
					// 2. checking
					nCode = 0;
					int nCnt = 0;
					while (nCode < instance_num && nCnt < nMaxCheck) {
						instanceInfos = checkInstance(target);
						nCode = instanceInfos.size();
						nCnt++;
						if (nCode == -1) {
							logger.error("error!" + nCode);
							return;
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
					results.put(ami_id, instanceInfos);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	// 3. execute app.
	public void runScript() throws Exception {
		Map<String, String> hostInfo = new HashMap<String, String>();

		JSONArray commands = (JSONArray) cmdConfig.get("commands");
		List<String> commands2 = new ArrayList<String>();
		for (Object cmd2 : commands) {
			logger.debug("=[input]=========" + cmd2.toString());
			commands2.add(cmd2.toString());
			commands2.add("finish!");
		}
		// 4. execute script.
		for (Object object : targets) {
			JSONObject target = (JSONObject) object;
			String pem_file = target.get("pem_file").toString();
			String username = target.get("username").toString();
			hostInfo.put("username", username);
			hostInfo.put("pem_file", pem_file);
			List<String> result = (List<String>) results.get(target.get("ami_id").toString());
			for (int i = 0; i < result.size(); i++) {
				String instanceInfo = (String) result.get(i);
				String host = getInstInfo(instanceInfo, "host");
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

	// 5. delete instances
	public void terminateInstance() throws Exception {
		for (String key : results.keySet()) {
			List<String> instanceInfos = (List<String>) results.get(key);
			for (String str : instanceInfos) {
				String region = getInstInfo(str, "region");
				if (str.indexOf("\tfulfilled\t") > -1) {
					String instanceId = str.split("\t")[11];
					String cmd = null;
					if (instanceId.trim().startsWith("i-")) {
						cmd = "ec2-terminate-instances " + instanceId + " --region=" + region;
						CmdUtil.execCommand(cmd);
					}
					String requestId = str.split("\t")[1];
					cmd = "ec2-cancel-spot-instance-requests " + requestId + " --region=" + region;
					CmdUtil.execCommand(cmd);
				}
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

	private String getInstInfo(String instanceInfo, String colmn) {
		String rslt = null;
		try {
			String region = instanceInfo.split("\t")[21];
			String instanceId = instanceInfo.split("\t")[11];
			region = region.substring(0, region.length() - 1);
			if (colmn.equals("host")) {
				String cmd = "ec2-describe-instances " + instanceId + " --region=" + region;
				rslt = CmdUtil.execCommand(cmd);
				return rslt.split("\t")[6];
			} else if (colmn.equals("ami_id")) {
				return instanceInfo.split("\t")[12];
			} else if (colmn.equals("region")) {
				return region;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private List<String> checkInstance(JSONObject target) {
		List<String> instanceInfos = new ArrayList<String>();
		// 1. checking exist of request
		String instance_type = target.get("instance_type").toString();
		String region = target.get("region").toString();
		// int instance_num =
		// Integer.parseInt(target.get("instance_num").toString());
		try {
			String cmd = "", splitFlag = "";
			if (instance_type.equals("common")) {
				cmd = "ec2-describe-instances --filter \"tag:Name=golang_WORKER*\"";
				splitFlag = "RESERVATION";
			} else if (instance_type.equals("spot")) {
				cmd = "ec2-describe-spot-instance-requests --region " + region;
				splitFlag = "SPOTINSTANCEREQUEST";
			}
			String rslt = CmdUtil.execCommand(cmd);
			String arry[] = rslt.split(splitFlag);
			for (int i = 0; i < arry.length; i++) {
				if (!arry[i].equals("") && arry[i].split("\t")[5].equals("active")) {
					if (instance_type.equals("common")) {
						arry[i] = arry[i].substring(arry[i].indexOf("INSTANCE") + "INSTANCE ".length(),
								arry[i].length());
					} else if (instance_type.equals("spot")) {
						arry[i] = arry[i].substring(0, arry[i].indexOf(" "));
					}
					instanceInfos.add(arry[i]);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return instanceInfos;
	}
}
