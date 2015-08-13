package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttExpand extends MqttWireMessage{
	public static final byte CMD_GET_ALIAS = 1;
	public static final byte CMD_GET_ALIAS_ACK = 2;
	public static final byte CMD_GET_TOPIC = 3;
	public static final byte CMD_GET_TOPIC_ACK = 4;
	public static final byte CMD_GET_ALIASLIST = 5;
	public static final byte CMD_GET_ALIASLIST_ACK = 6;
	public static final byte CMD_PUBLISH = 7;
	public static final byte CMD_PUBLISH_ACK = 8;
	public static final byte CMD_GET_STATUS = 9;
	public static final byte CMD_GET_STATUS_ACK = 10;
	
	public MqttExpand () {
		super(MqttWireMessage.MESSAGE_TYPE_EXPAND);
	}

	public MqttExpand (String topics, byte command) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_EXPAND);
		this.command = command;
		topic = topics;
		setPayload();
	}
	protected byte[] encodedPayload = null;
	protected String topic = null;
	protected byte command ;

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
		
				//****change the version code . write by Quentin***//
				//********************for massage id refactor******// 
				//dos.writeShort(msgId);
				dos.writeLong(msgId);
				//*************************************************//
			
			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	public boolean isMessageIdRequired() {
		// all publishes require a message ID as it's used as the key to the token store
		return true;
	}



	@Override
	public byte[] getPayload() throws MqttException {
		return encodedPayload;
	}


	
	public void setPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(command);
			if(null != topic) {
				byte[] encodedString = topic.getBytes("UTF-8");
				dos.writeChar(topic.length());
				dos.write(encodedString);
			} else {
				dos.writeChar(0);
			}
			
			//*************************************************//
		
		dos.flush();
		encodedPayload = baos.toByteArray();
	}
	catch (IOException ex) {
		throw new MqttException(ex);
	}
	}

	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}
}
