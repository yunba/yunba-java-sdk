package io.yunba.java.core;

public interface MessageDelivery {

	public void postReceivedMessage(String topic, String entity);
	
	public void postReceivedPresence(String topic, String presence);
	
	public void postConnected();
	
	public void postDisConnected();
}
