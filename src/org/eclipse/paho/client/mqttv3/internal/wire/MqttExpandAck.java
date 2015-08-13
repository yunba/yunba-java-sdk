package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttExpandAck extends MqttAck {
	public byte command;
	public byte status;
	public String result;

	public MqttExpandAck(byte info, byte[] data) {
		super(MESSAGE_TYPE_EXPAND);
		DataInputStream dis = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			CountingInputStream counter = new CountingInputStream(bais);
			dis = new DataInputStream(counter);
			msgId = dis.readLong();
			byte[] payload = new byte[data.length - counter.getCounter()];
			dis.readFully(payload);
			command = payload[0];
			status = payload[1];
			if (payload.length > 2) {
				int lenth = getLenth(payload[2], payload[3]);
				byte[] msg = new byte[lenth];

				System.arraycopy(payload, 4, msg, 0, msg.length);
				result = new String(msg, "UTF-8");
				if (command == MqttExpand.CMD_GET_STATUS_ACK) {
					try {
						JSONObject js = new JSONObject();
						js.put("status", result);
						result = js.toString();
					} catch (Exception e) {
						// YLogger.e("JSONObject", "", e);
					}
				} else if(command == MqttExpand.CMD_GET_ALIAS_ACK) {
				      //  System.err.println("MqttExpand.CMD_GET_ALIAS_ACK = " + result);
						JSONObject js = new JSONObject();
						js.put("alias", result);
						result = js.toString();
				
				}
				// YLogger.d("MqttExpandAck", "result = " + result);
			} else {
				// YLogger.d("MqttExpandAck", "no data in payload");
			}

		} catch (Exception e) {
			// YLogger.e("MqttExpandAck", "", e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (Exception e) {

				}
			}
		}

	}

	@Override
	public String toString() {
		return "command " + command + " status " + status + " msg = " + result;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		// TODO Auto-generated method stub
		return null;
	}

	public static int getLenth(byte b1, byte b2) throws IOException {

		return ((b1 << 8) & 0xFF00) | b2 & 0xFF;

	}
}
