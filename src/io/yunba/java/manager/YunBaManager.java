package io.yunba.java.manager;

import io.yunba.java.core.Constants;
import io.yunba.java.core.MQTTMessage;
import io.yunba.java.core.MQTTStack;
import io.yunba.java.util.CommonUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttExpand;

import com.google.common.eventbus.EventBus;


public class YunBaManager {
	public static final String PUBLISH_RECEIVED_ACTION = "io.yunba.java.PUBLISH_RECEIVED_ACTION";
	public static final String SUBSCRIBE_RECEIVED_ACTION = "io.yunba.java.SUBSCRIBE_RECEIVED_ACTION";
	public static final String MESSAGE_RECEIVED_ACTION = "io.yunba.java.MESSAGE_RECEIVED_ACTION";
	public static final String PRESENCE_RECEIVED_ACTION = "io.yunba.java.PRESENCE_RECEIVED_ACTION";
	public static final String MESSAGE_CONNECTED_ACTION = "io.yunba.java.MESSAGE_CONNECTED_ACTION";
	public static final String MESSAGE_DISCONNECTED_ACTION = "io.yunba.java.MESSAGE_DISCONNECTED_ACTION";
	public static final String MQTT_TOPIC = "topic";
	public static final String MQTT_MSG = "message";
	public static final String LAST_PUB = "last_topic";
	public static final String LAST_SUB = "last_sub";
	public static final String HISTORY_TOPICS = "history_topics";
	
	private static int callBackId = 0;
	public static ConcurrentHashMap<Integer, IMqttActionListener> callbacks = new ConcurrentHashMap<Integer, IMqttActionListener>();
	
	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	private static MQTTStack mMqttStack;
	public static String AppKey = null;
	private static EventBus mEventBus = null;
	
	public static void start() {
		if (null == mMqttStack) {
			synchronized (YunBaManager.class) { 
				if (null == mMqttStack) {
					mMqttStack = new MQTTStack(AppKey);
				}
			}
		}
		if (null == mEventBus) {
			synchronized (YunBaManager.class) { 
				if (null == mEventBus) {
					mEventBus = new EventBus();
				}
			}
		}
		mMqttStack.start();
	}

	public static IMqttActionListener getTagAliasCallback(int seqId) {
		try {
			return callbacks.remove(seqId);
		} catch (Exception e) {
			return null;
		}
	}
	
	private synchronized static int getCallbackID() {
		return callBackId++;
	}
	
	public static EventBus getEventBus() {
		if (null == mEventBus) {
			synchronized (YunBaManager.class) { 
				if (null == mEventBus) {
					mEventBus = new EventBus();
				}
			}
		}
		return mEventBus;
	}

	public static void start(String appKey) {
		AppKey = appKey;
		start();
	}

	public static void stop() {
		mMqttStack.handleStopAction();
	}

	public static void resume() {
		mMqttStack.handleResumeAction();
	}

	public static boolean isStop() {
		return mMqttStack.isStop();
	}

	public static void ping() {
		mMqttStack.handleKeepLive(0);
	}
	
	public static void subscribe(String topic,
			final IMqttActionListener mqttAction) {
		subscribe(new String[] { topic }, 1, mqttAction);
	}

	public static void subscribe(String[] topics,
			final IMqttActionListener mqttAction) {
		subscribe(topics, 1, mqttAction);
	}

	private static void subscribe(String[] topics, int qos,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topics)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		MQTTMessage message = new MQTTMessage(MQTTMessage.TYPE_SUBSCRIBE,
				CommonUtil.join(topics, "$$$"), null, qos, null, -1, mqttAction);
		mMqttStack.addMQTTMessage(message);
	}

	public static void unsubscribe(String topic,
			final IMqttActionListener mqttAction) {
		unsubscribe(new String[] { topic }, 1, mqttAction);
	}

	public static void unsubscribe(String[] topics, int qos,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topics)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		MQTTMessage message = new MQTTMessage(MQTTMessage.TYPE_UNSUBSCRIBE,
				CommonUtil.join(topics, "$$$"), null, qos, null, -1, mqttAction);
		mMqttStack.addMQTTMessage(message);
	}

	public static void publish(String topic, String message,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topic)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		publish(topic, message, 1, mqttAction);
	}

	public static void publish2(String topic, String message, JSONObject opts, 
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topic)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		MQTTMessage cache = new MQTTMessage(MQTTMessage.TYPE_EXPAND, topic, message, 1, 
				opts, -1, mqttAction);
		mMqttStack.addMQTTMessage(cache);
	}
	
	private static void publish(String topic, String message, int qos,
			final IMqttActionListener mqttAction) {
		MQTTMessage cache = new MQTTMessage(MQTTMessage.TYPE_PUBLISH, topic,
				message, qos, null, -1, mqttAction);
		mMqttStack.addMQTTMessage(cache);
	}
	
	public static void setAlias(String alias,
			final IMqttActionListener mqttAction) {
		if(!isValidTopic(alias)){
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		publish(Constants.TOPIC_SET_ALIAS, alias, 1, mqttAction);
	}

	public static void getAlias(final IMqttActionListener mqttAction) {
		if (null == mqttAction) {
			return;
		}
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_ALIAS, null, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}

	public static void subscribePresence(String topic,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topic)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		presence(topic, mqttAction);
	}

	public static void unsubscribePresence(String topic,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topic)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		unPresence(topic, mqttAction);
	}

	private static void presence(String topic, IMqttActionListener mqttAction) {
		topic += "/p";
		subscribe(new String[] { topic }, 0, mqttAction);
	}

	private static void unPresence(String topic, IMqttActionListener mqttAction) {
		topic += "/p";
		unsubscribe(new String[] { topic }, 0, mqttAction);
	}

	public static void setBroker(String broker) {

	}

	public static void getAliasList(String topic,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topic)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		getAliassByTopic(topic, mqttAction);
	}

	public static void getAliasListV2(String topic,
			final IMqttActionListener mqttAction) {
		if (!isValidTopic(topic)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		getAliassByTopicV2(topic, mqttAction);
	}

	public static void getState(String alias,
			final IMqttActionListener mqttAction) {
		if (!isValidAlias(alias)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		getStatusByAlias(alias, mqttAction);
	}

	public static void getStateV2(String alias,
			final IMqttActionListener mqttAction) {
		if (!isValidAlias(alias)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		getStatusByAliasV2(alias, mqttAction);
	}

	public static void publishToAlias(String alias, String message,
			final IMqttActionListener mqttAction) {
		if (!isValidAlias(alias)) {
			if (null != mqttAction) {
				executor.execute(new Runnable() {
					public void run() {
						mqttAction.onFailure(null, new MqttException(
								MqttException.REASON_CODE_INVALID_TOPIC));
					}
				});
			}
			return;
		}
		publishByAlias(alias, message, mqttAction);
	}

	private static void publishByAlias(String alias, String message,
			IMqttActionListener mqttAction) {
		publish(",yta/" + alias, message, 1, mqttAction);
	}
	
	public static void getTopicList(String alias,
			final IMqttActionListener mqttAction) {
		getTopics(alias, mqttAction);
	}

	public static void getTopicList(final IMqttActionListener mqttAction) {
		getTopics(null, mqttAction);
	}
	
	public static void getTopicListV2(String alias,
			final IMqttActionListener mqttAction) {
		getTopicsV2(alias, mqttAction);
	}
	
	public static void getTopicListV2(final IMqttActionListener mqttAction) {
		getTopicsV2(null, mqttAction);
	}
	
	private static void getTopics(String alias, IMqttActionListener mqttAction) {
		if (null == mqttAction) {
			return;
		}
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_TOPIC, alias, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}
	
	private static void getTopicsV2(String alias, IMqttActionListener mqttAction) {
		if (null == mqttAction) {
			return;
		}
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_TOPIC_V2, alias, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}
	
	private static void getAliassByTopic(String topic,
			IMqttActionListener mqttAction) {
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_ALIASLIST, topic, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}

	private static void getAliassByTopicV2(String topic,
			IMqttActionListener mqttAction) {
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_ALIASLIST_V2, topic, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}

	private static void getStatusByAlias(String topic,
			IMqttActionListener mqttAction) {
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_STATUS, topic, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}

	private static void getStatusByAliasV2(String topic,
			IMqttActionListener mqttAction) {
		MQTTMessage msg = MQTTMessageFactory.getExpandMessgae(MqttExpand.CMD_GET_STATUS_V2, topic, null, 0, null, mqttAction);
		mMqttStack.addMQTTMessage(msg);
	}
	
	private static class MQTTMessageFactory {
		public static MQTTMessage getExpandMessgae(byte expandCommand, String topic, String msg, int qos, Object userContent, 
				IMqttActionListener listener) {
			int serialId = getCallbackID();
			callbacks.put(serialId, listener);
			MQTTMessage res = new MQTTMessage(MQTTMessage.TYPE_EXPAND, topic, msg, qos, userContent, serialId, listener);
			res.EXPAND_COMMNAD = expandCommand;
			return res;
		}
	}
	
	public static boolean isValidAlias(String s) {
		if (null == s)
			return false;
		Pattern p = Pattern.compile("^[0-9a-zA-Z_]{0,129}$");
		Matcher m = p.matcher(s);
		return m.matches();
	}

	public static boolean isValidTopic(String s) {
		if (null == s)
			return false;
		Pattern p = Pattern.compile("^[0-9a-zA-Z_/+#]{0,129}$");
		Matcher m = p.matcher(s);
		return m.matches();
	}

	public static boolean isValidTopic(String[] topics) {
		if (null == topics || topics.length > 128) {
			return false;
		}
		for (String s : topics) {
			if (!isValidTopic(s))
				return false;
		}
		return true;
	}
}
