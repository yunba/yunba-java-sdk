package io.yunba.java.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Properties;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttUtil {
	public final static String RIGSTER_URL = "http://reg.yunba.io:8383/device/reg/";
	public static String BROKER_URL = "tcp://182.92.154.3:1883";
	public static final String TCP_TICKET_URL = "tick-t.yunba.io";
	public static final String HTTP_TICKET_IP = "http://101.200.229.48:9999";
	public static final String MQTT_HOST = "tick-b.yunba.io";

	public static final int TCP_TICKET_PORT = 9977;
	public static final String TCP_TICKET_IP = "123.57.32.238";

	public static final String DATA_FILE = ".opts";
	private static Properties pro;
	private static long mLastLookupTime = 0;

	public static void register(JSONObject obj) {
		pro = getOptsPro();
		if (null != pro
				&& pro.getProperty("a", null).equals(obj.optString("a", ""))) {
			return;
		}

		URL url = null;
		HttpURLConnection connection = null;
		OutputStream out = null;
		BufferedReader reader = null;
		try {
			url = new URL(RIGSTER_URL);

			connection = (HttpURLConnection) url.openConnection();

			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);

			connection.setRequestProperty("Content-Type", "application/json");
			// connection.setRequestProperty("Content-Type", "text/xml");
			connection.setRequestProperty("Accept", "application/json");
			connection.connect();
			// System.out.println(obj.toString());
			out = connection.getOutputStream();
			out.write(obj.toString().getBytes());
			out.flush();

			reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String lines;
			StringBuffer sb = new StringBuffer("");
			while ((lines = reader.readLine()) != null) {
				lines = new String(lines.getBytes(), "utf-8"); // 涓枃
				sb.append(lines);
			}
			// System.out.println(sb.toString());
			JSONObject obj2 = new JSONObject(sb.toString());
			obj2.put("a", obj.get("a"));
			saveOpts(obj2);
			// return sb.toString();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (null != out)
					out.close();
			} catch (Exception e) {
			}
			try {
				if (null != reader)
					reader.close();
			} catch (Exception e) {
			}
			connection.disconnect();
		}
	}

	private static void saveOpts(JSONObject obj) {
		String fileDir = System.getProperty("user.dir");
		File file = new File(fileDir, DATA_FILE);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			Properties pro = new Properties();
			Iterator it = obj.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				String value = obj.getString(key);
				pro.setProperty(key, value);
			}
			pro.store(fos, " ");
			fos.close();
		} catch (Exception e) {

		} finally {
			try {
				if (null != fos)
					fos.close();
			} catch (IOException e) {

			}
		}
	}

	public static Properties getOptsPro() {
		if (null != pro)
			return pro;
		Reader fis = null;
		try {
			String file = System.getProperty("user.dir") + File.separator
					+ DATA_FILE;
			// File file = new File(fileDir, DATA_FILE);
			fis = new FileReader(file);
			pro = new Properties();
			pro.load(fis);
			String uName = pro.getProperty("u", null);
			String pwd = pro.getProperty("p", null);
			String cid = pro.getProperty("c", null);
			String appkey = pro.getProperty("a", null);

			if (isEmpty(uName) || isEmpty(pwd) || isEmpty(cid)
					|| isEmpty(appkey)) {
				return null;
			}
			return pro;

		} catch (Exception e) {
			// System.out.println("鏂囦欢涓嶅瓨鍦�:"+ e.toString());
			return null;
		} finally {
			if (null != fis) {
				try {
					fis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	public static MqttConnectOptions getOpts() {
		Properties pro = getOptsPro();
		if (null != pro) {
			String uName = pro.getProperty("u", null);
			String pwd = pro.getProperty("p", null);
			String cid = pro.getProperty("c", null);

			if (!isEmpty(uName) && !isEmpty(pwd) && !isEmpty(cid)) {
				MqttConnectOptions opts = new MqttConnectOptions();
				opts.setKeepAliveInterval(250);
				opts.setUserName(uName);
				opts.setPassword(pwd.toCharArray());
				return opts;
			}

		}
		return null;
	}

	public static String getCid() {
		Properties pro = getOptsPro();
		if (null != pro) {

			String cid = pro.getProperty("c", null);

			if (!isEmpty(cid)) {
				return cid;
			}

		}
		return null;
	}

	/**
	 * will trim the string
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isEmpty(String s) {
		if (null == s)
			return true;
		if (s.length() == 0)
			return true;
		if (s.trim().length() == 0)
			return true;
		return false;
	}

	public static String hostToIp(String host) {
		InetAddress address;
		try {
			address = InetAddress.getByName(host);
			return address.getHostAddress();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	public static String getBroker() {
		String broker = lookup();
		if (!isEmpty(broker))
			return broker;
		String ip = hostToIp(MQTT_HOST);
		if (isEmpty(ip))
			return BROKER_URL;
		return new StringBuilder().append("tcp://").append(ip).append(":1883")
				.toString();
	}

	public static String doTcpJSON2(String url, int port, String json) {
		if (isEmpty(json))
			return null;
		String ret = null;
		Socket socket = null;
		BufferedReader in = null;
		DataInputStream din = null;
		DataOutputStream dos = null;
		ByteArrayOutputStream outSteam = null;
		try {
			// json =
			// "{\"a\":\"5520a2887e353f5814e10b62\",\"n\":\"WIFI\",\"v\":\"erlang-sdk\",\"o\":\"CDMA\",\"c\":\"00000001-0000002\",\"t\":\"ip\"}";
			socket = new Socket(url, port);
			socket.setSoTimeout(2000);
			din = new DataInputStream(socket.getInputStream());
			// OutputStreamWriter out = new
			// OutputStreamWriter(socket.getOutputStream());

			ByteArrayOutputStream bot = new ByteArrayOutputStream();
			DataOutputStream mydata = new DataOutputStream(bot);
			mydata.writeByte(1);
			mydata.writeShort(json.getBytes().length);
			// mydata.writeChar(json.getBytes().length);
			// System.err.println("lenth = " + json.getBytes().length);
			mydata.write(json.getBytes());
			dos = new DataOutputStream(socket.getOutputStream());

			// String outMsgString = "xgapplist:" + getLocalXGApps();
			// if (content != null) {
			// outMsgString = content;
			// }

			// for (byte theByte : bot.toByteArray())
			// {
			// System.out.print(Integer.toHexString(theByte) + " ");
			// }
			dos.write(bot.toByteArray());

			dos.flush();
			outSteam = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = -1;
			while ((len = socket.getInputStream().read(buffer)) > -1) {

				// System.err.println("2receive msg from getInputStream len = "
				// +
				// len);
				// ret = new String(buffer);
				// System.err.println("2receive msg from watchdog server " +
				// ret);
				outSteam.write(buffer, 0, len);
			}

			if (outSteam.toByteArray().length > 3) {
				// byte[] bytes =
				// TpnsSecurity.oiSymmetryDecrypt2Byte(outSteam.toByteArray());
				byte[] returnBytes = outSteam.toByteArray();
				byte[] stringBytes = new byte[returnBytes.length - 3];
				System.arraycopy(returnBytes, 3, stringBytes, 0,
						stringBytes.length);
				;
				ret = new String(stringBytes);
			} else {
				System.err.println("receive msg from ticket server null");
			}
			// nextSendTime = now + 10 * 1000;
		} catch (Throwable e) {
			System.err.println(e);
		} finally {
			if (null != socket) {
				try {
					socket.close();
				} catch (Exception e) {
					System.err.println("close socket failed " + e.getMessage());
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception ee) {

				}
			}
			if (null != outSteam) {
				try {
					outSteam.close();
				} catch (IOException e) {

				}
			}
			if (dos != null) {
				try {
					dos.close();
				} catch (Exception ee) {

				}
			}

			if (din != null) {
				try {
					din.close();
				} catch (Exception dee) {

				}
			}
		}
		return ret;
	}

	public static String lookup() {
		// if (Math.abs(System.currentTimeMillis() - mLastLookupTime) < 10 * 60
		// * 1000) {
		// return null;
		// }
		try {
			JSONObject ticket = new JSONObject();
			Properties pro = getOptsPro();
			if (null == pro)
				return null;
			ticket.put("a", pro.getProperty("a"));
			ticket.put("v", "1.0");

			if (!isEmpty(pro.getProperty("c"))) {
				ticket.put("c", pro.getProperty("a"));
			}
			String ticketIp = TCP_TICKET_IP;
			String tIp = hostToIp(TCP_TICKET_URL);
			if (!isEmpty(tIp)) {
				ticketIp = tIp;
			}
			String ticketInfo = doTcpJSON2(ticketIp, TCP_TICKET_PORT,
					ticket.toString());
			System.err.println("ticket info = " + ticketInfo);
			JSONObject result = new JSONObject(ticketInfo);
			String ip = result.getString("c");
			return ip;

		} catch (Exception e) {
			return null;
		}
	}
}
