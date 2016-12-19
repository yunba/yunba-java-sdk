package io.yunba.java.core;

import io.yunba.java.core.event.MessageArrivedEvent;
import io.yunba.java.manager.YunBaManager;

public class EventBusMessageDelivery implements MessageDelivery {

	@Override
	public void postReceivedMessage(String topic, String entity) {
		YunBaManager.getEventBus().post(
				new MessageArrivedEvent(YunBaManager.MESSAGE_RECEIVED_ACTION,
						topic, entity));
	}

	@Override
	public void postReceivedPresence(String topic, String presence) {
		YunBaManager.getEventBus().post(
				new MessageArrivedEvent(YunBaManager.PRESENCE_RECEIVED_ACTION,
						topic, presence));
	}

	@Override
	public void postConnected() {
		YunBaManager.getEventBus().post(
				new MessageArrivedEvent(YunBaManager.MESSAGE_CONNECTED_ACTION,
						null, null));
	}

	@Override
	public void postDisConnected() {
		YunBaManager.getEventBus().post(
				new MessageArrivedEvent(YunBaManager.MESSAGE_DISCONNECTED_ACTION,
						null, null));
	}

}
