package org.eclipse.paho.client.mqttv3.internal.wire;



import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.util.MqttUtil;
;

public class MqttExpandPublish extends MqttExpand {
	public MqttExpandPublish () {
		super();
	}

	public String msg ;
	public JSONObject opts;
	public MqttExpandPublish (String topics, String msg, byte command, JSONObject opts) throws MqttException {
		super();
		this.command = command;
		this.opts = opts;
		this.msg = msg;
		topic = topics;
		setPayload();
	}
	
	
	public void setPayload() throws MqttException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeByte(command);
			byte[] tlv = getTLVBytes(opts, topic, msg);
			if (null != tlv) {
				dos.writeChar(tlv.length);
				dos.write(tlv);
			} else {
				dos.writeChar(0);
			}

			// *************************************************//

			dos.flush();
			encodedPayload = baos.toByteArray();
		} catch (Exception ex) {
			throw new MqttException(-105,ex);
		} finally {
			try {
				dos.close();
				baos.close();
			} catch (Exception e) {

			}
		}
	}
	
	
	public byte[] getTLVBytes(JSONObject opts, String topic, String msg) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			if (!MqttUtil.isEmpty(topic)) {
				dos.writeByte(0);
				dos.writeChar(topic.getBytes().length);
				dos.write(topic.getBytes("UTF-8"));
			}

			if (!MqttUtil.isEmpty(msg)) {
				dos.writeByte(1);
				dos.writeChar(msg.getBytes().length);
				dos.write(msg.getBytes("UTF-8"));
			}
			if (null != opts && opts.length() > 0) {
				Iterator<?> keys = opts.keys();

				while (keys.hasNext()) {
					String key = (String) keys.next();
					String value = opts.getString(key);
					int keyToInt = getKey(key);
					if (keyToInt != -1) {
						dos.writeByte(keyToInt);
						dos.writeChar(value.getBytes().length);
						dos.write(value.getBytes("UTF-8"));
					}
				}
			}
			dos.flush();

			return baos.toByteArray();
		} catch (Exception ex) {
			//YLogger.e("MqttExpandPublish", "", ex);
			System.err.println(ex.getMessage());
			return null;
		} finally {
			try {
				dos.close();
				baos.close();
			} catch (Exception e) {

			}
		}
	}
	
	
	//    0: "topics",
	//    1: "payload",
    //    2: "platform",
	//    3: "time_to_live",
	//    4: "time_delay",
	//   5: "location",
	//    6: "qos",
	//   7: "apn_json",
	private static Map<String, Integer> keys =  new HashMap<String, Integer>();
	static {
		keys.put("platform", 2);
		keys.put("time_to_live", 3);
		keys.put("time_delay", 4);
		keys.put("location", 5);
		keys.put("qos", 6);
		keys.put("apn_json", 7);
	}
	public static int getKey(String key) {
		try {
			if (MqttUtil.isEmpty(key)) return -1;
			return keys.get(key);
		} catch (Exception e) {
			return -1;
		}
	}
	
	 public static String byte2HexString(byte[] b) {
	        char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7',
	                      '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	        char[] newChar = new char[b.length * 2];
	        for(int i = 0; i < b.length; i++) {
	            newChar[2 * i] = hex[(b[i] & 0xf0) >> 4];
	            newChar[2 * i + 1] = hex[b[i] & 0xf];
	        }
	        return new String(newChar);
	    }
}
