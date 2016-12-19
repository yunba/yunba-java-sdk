package io.yunba.java.core.event;

public class MessageArrivedEvent implements IEvent{
	private String mAction;
	private String mTopic;
	private String mMsg;
	
	public MessageArrivedEvent(String action, String topic, String msg) {
		this.mAction = action;
		this.mTopic = topic;
		this.mMsg = msg;
	}
	
	public String getAction() {
		return mAction;
	}
	
	public String getTopic() {
		return mTopic;
	}
	
	public String getMessage() {
		return mMsg;
	}
}
