package io.yunba.java.core;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;

public class MQTTMessage {
	
	public final static int TYPE_SUBSCRIBE = 1; 
	public final static int TYPE_PUBLISH = 2; 
	public final static int TYPE_UNSUBSCRIBE = 3; 
	public final static int TYPE_EXPAND = 4; 
	/**
	 * 1 : subscribe
	 * 2 : publish
	 * 3 : unsubscribe
	 * 4 : expand
	 */
	public int type;
	
	public int callbackId;
	
	public String topic; // maybe  topic, alias, jsonobject
	
	public String msg;
	
	public int qos;
	
	public Object userContent;
	
	public byte EXPAND_COMMNAD;
	
	public IMqttActionListener callback;
	
	public MQTTMessage(int type, String topic, String msg, int qos, Object userContent,  int callback, 
			IMqttActionListener listener) {
		this.type = type;
		this.callbackId = callback;
		this.topic = topic;
		this.msg = msg;
		this.qos = qos;
		this.userContent = userContent;
		this.callback = listener;
	}
	
	
	@Override
	public String toString() {
		return "type = " + type  + " topic = " + topic + " msg = " + topic + " userContent = " + userContent;
	}
	

}
