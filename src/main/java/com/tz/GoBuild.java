package com.tz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
	private String host = "";
	private String pem_file = "";
	
	private List instanceIds = new ArrayList();

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
		hostInfo.put("pem_file", pem_file);

		for (int i = 0; i < instanceIds.size(); i++) {
			String cmd = "ec2-describe-instances " + instanceIds.get(i) + " --region=" + region; 
			String rslt = CmdUtil.execUnixCommand(cmd);
			
			hostInfo.put("host", host);
			List<String> commands = new ArrayList<String>();
			commands.add("ls -al");

			SSHUtil util = new SSHUtil();
			rslt = util.shell(hostInfo, commands);
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

	public String replaceVariables(String orgStr, Map<String, Object> var) {
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

	public int checkInstance(String instanceType) {
		// 1. checking exist of request
		try {
			String cmd = "", splitFlag = "";
			if (instanceType.equals("common")) {
				cmd = "ec2-describe-instances --filter \"tag:Name=golang_WORKER*\"";
				splitFlag = "RESERVATION";
			} else if (instanceType.equals("spot")) {
				cmd = "/usr/local/bin/aws ec2 describe-spot-instance-requests";
				splitFlag = "SPOTINSTANCEREQUEST";
			}
			String rslt = CmdUtil.execUnixCommand(cmd);
			if(rslt.length() > 0) {
				JSONParser parser = new JSONParser();
				if (instanceType.equals("common")) {
				} else if (instanceType.equals("spot")) {
					JSONArray jsonArry = (JSONArray) ((JSONObject) parser.parse(rslt)).get("SpotInstanceRequests");
					Iterator itr = jsonArry.iterator();
					while (itr.hasNext()) {
						JSONObject featureJsonObj = (JSONObject) itr.next();
						String state = featureJsonObj.get("State").toString();
						JSONObject info = (JSONObject) featureJsonObj.get("LaunchSpecification");
						String keyName = info.get("KeyName").toString();
						if (state.equals("active") && keyName.equals("golang2")) {
							instanceIds.add(featureJsonObj.get("InstanceId").toString());
						}
					}
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

		// // 1.1 checking
		// DefaultHttpClient httpClient = new DefaultHttpClient();
		// StringBuilder result = new StringBuilder();
		// try {
		// String url = "http://" + master_external_ip + ":26080/cluster.jsp";
		// HttpGet postRequest = new HttpGet(url);
		// postRequest.addHeader("Content-Type", "application/json");
		// HttpResponse response = httpClient.execute(postRequest);
		// if (response.getStatusLine().getStatusCode() != 200) {
		// System.out.println("fail sending url => " + url);
		// throw new RuntimeException("Failed : HTTP error code : " +
		// response.getStatusLine().getStatusCode());
		// }
		// BufferedReader br = new BufferedReader(new
		// InputStreamReader((response.getEntity().getContent())));
		// String output;
		// while ((output = br.readLine()) != null) {
		// result.append(output);
		// }
		// String tmp = result.toString();
		// if (tmp.indexOf("<h2>Worker</h2>") > -1) {
		// tmp = tmp.substring(tmp.indexOf("<h2>Worker</h2>"), tmp.length());
		// tmp = tmp.substring(tmp.indexOf(":") + 1, tmp.length());
		// tmp = tmp.substring(0, tmp.indexOf(","));
		// }
		// if (tmp.length() > 0) {
		// return Integer.parseInt(tmp);
		// }
		// } catch (ClientProtocolException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		return -1;
	}
}
