package org.eclipse.paho.client.mqttv3;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.internal.Token;

/**
 *  Provides a mechanism for tracking the completion of an asynchronous action.
 *  <p>
 *  A token that implements the ImqttToken interface is returned from all non-blocking 
 *  method with the exception of publish. 
 *  </p>
 *  
 * @see IMqttToken
 */

public class MqttToken implements IMqttToken {
	/**
	 * A reference to the the class that provides most of the implementation of the 
	 * MqttToken.  MQTT application programs must not use the internal class.
	 */
	public Token internalTok = null;
		
	public MqttToken() {
	}
	
	public MqttToken(String logContext) {
		internalTok = new Token(logContext);
	}
	
	public MqttException getException() {
		return internalTok.getException();
	}

	public boolean isComplete() {
		return internalTok.isComplete();
	}

	public void setActionCallback(IMqttActionListener listener) {
		internalTok.setActionCallback(listener);

	}
	public IMqttActionListener getActionCallback() {
		return internalTok.getActionCallback();
	}

	public void waitForCompletion() throws MqttException {
		internalTok.waitForCompletion(-1);
	}

	public void waitForCompletion(long timeout) throws MqttException {
		internalTok.waitForCompletion(timeout);
	}
	
	public IMqttAsyncClient getClient() {
		return internalTok.getClient();
	}
	
	public String[] getTopics() {
		return internalTok.getTopics();
	}

	public Object getUserContext() {
		return internalTok.getUserContext();
	}

	public void setUserContext(Object userContext) {
		internalTok.setUserContext(userContext);	}

	public long getMessageId() {
		return internalTok.getMessageID();
	}
	
	private String alias;

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	private JSONObject result;

	@Override
	public JSONObject getResult() {
		return result;
	}

	@Override
	public void setResult(JSONObject result) {
		this.result = result;
	}


}
