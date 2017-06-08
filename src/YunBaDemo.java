import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.common.eventbus.Subscribe;

import io.yunba.java.core.event.MessageArrivedEvent;
import io.yunba.java.manager.YunBaManager;

public class YunBaDemo {
	public static String ALIAS = "51595de01b2943499227e62713fb49ee_9cc8b78f9bf847e0aa1512f347f03bbc";

	public static void main(String[] args) {
		// 初始化 YunBa SDK
		YunBaManager.start("56a0a88c4407a3cd028ac2fe");

		YunBaManager.getEventBus().register(new Object() {

			@Subscribe
			public void listen(MessageArrivedEvent event) {
				switch (event.getAction()) {
				case YunBaManager.MESSAGE_RECEIVED_ACTION:
					System.out.println("mqtt receive topic = "
							+ event.getTopic() + " msg = " + event.getMessage());
					break;
				case YunBaManager.PRESENCE_RECEIVED_ACTION:
					System.out.println("mqtt receive presence = "
							+ event.getTopic() + " msg = " + event.getMessage());
					break;
				case YunBaManager.MESSAGE_CONNECTED_ACTION:
					System.out.println("mqtt connect success");
					break;
				case YunBaManager.MESSAGE_DISCONNECTED_ACTION:
					System.out.println("mqtt disconnect!");
					break;
				default:

				}
			}
		});

		// 订阅一个频道(Topic)，以接收来自频道的消息
		YunBaManager.subscribe("bullet", new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				System.out.println("mqtt succeed subscribe: "
						+ join(asyncActionToken.getTopics(), ","));
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
					Throwable exception) {
				if (exception instanceof MqttException) {
					MqttException ex = (MqttException) exception;
					System.err
							.println("subscribe failed with the error code = "
									+ ex.getReasonCode());
				}
			}
		});

		// 订阅一个频道(Topic)，以接收来自频道的消息
		YunBaManager.subscribe("like", new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				System.out.println("mqtt succeed subscribe: "
						+ join(asyncActionToken.getTopics(), ","));
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
					Throwable exception) {
				if (exception instanceof MqttException) {
					MqttException ex = (MqttException) exception;
					System.err
							.println("subscribe failed with the error code = "
									+ ex.getReasonCode());
				}
			}
		});

		// 订阅一个频道(Topic)，以接收来自频道的消息
		YunBaManager.subscribe("stat", new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				System.out.println("mqtt succeed subscribe: "
						+ join(asyncActionToken.getTopics(), ","));
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
					Throwable exception) {
				if (exception instanceof MqttException) {
					MqttException ex = (MqttException) exception;
					System.err
							.println("subscribe failed with the error code = "
									+ ex.getReasonCode());
				}
			}
		});
		// 向 Topic 发送消息
		YunBaManager.publish("bullet", "like", new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				System.out.println("mqtt publish success");
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
					Throwable exception) {
				if (exception instanceof MqttException) {
					MqttException ex = (MqttException) exception;
					System.err.println("publish failed with the error code = "
							+ ex.getReasonCode() + " cause : " + ex.getCause().toString());
				}
			}
		});
//
//		YunBaManager.unsubscribe("test_topic", new IMqttActionListener() {
//
//			@Override
//			public void onSuccess(IMqttToken asyncActionToken) {
//				String topic = join(asyncActionToken.getTopics(), ",");
//				System.out.println("UnSubscribe succeed : " + topic);
//			}
//
//			@Override
//			public void onFailure(IMqttToken asyncActionToken,
//					Throwable exception) {
//				if (exception instanceof MqttException) {
//					MqttException ex = (MqttException) exception;
//					String msg = "unSubscribe failed with error code : "
//							+ ex.getReasonCode();
//					System.err.println(msg);
//				}
//			}
//		});

		// 调用此函数来绑定账号
		YunBaManager.setAlias(ALIAS, new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				System.out.println("mqtt setAlias success");
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
					Throwable exception) {
				if (exception instanceof MqttException) {
					MqttException ex = (MqttException) exception;
					System.err.println("setAlias failed with the error code = "
							+ ex.getReasonCode());
				}
			}
		});

		// 获取当前用户的别名
		YunBaManager.getAlias(new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				System.out.println("mqtt get alias = "
						+ asyncActionToken.getAlias());
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
					Throwable exception) {
				MqttException mqtt = (MqttException) exception;
				System.err.println("mqtt get alias failed:"
						+ mqtt.getReasonCode());
			}
		});

//		// 向用户别名对象发送消息，用于实现点对点的消息发送
//		YunBaManager.publishToAlias("java_alias", "msg to java_alaias", null);
//
//		// 查询当前用户订阅的频道列表
//		YunBaManager.getTopicList("java_alias", new IMqttActionListener() {
//
//			@Override
//			public void onSuccess(IMqttToken token) {
//				System.out.println("get getTopics = " + token.getResult());
//			}
//
//			@Override
//			public void onFailure(IMqttToken arg0, Throwable exception) {
//				if (exception instanceof MqttException) {
//					MqttException ex = (MqttException) exception;
//					String msg = "getTopicList failed with error code : "
//							+ ex.getReasonCode();
//					System.err.println(msg);
//				}
//			}
//		});
//
//		// 根据 别名 来获取用户的状态
//		YunBaManager.getState("java_alias", new IMqttActionListener() {
//
//			@Override
//			public void onSuccess(IMqttToken asyncActionToken) {
//				JSONObject result = asyncActionToken.getResult();
//				try {
//					String status = result.getString("status");
//					System.out.println("mqtt get state success, status = "
//							+ status);
//				} catch (JSONException e) {
//
//				}
//			}
//
//			@Override
//			public void onFailure(IMqttToken asyncActionToken,
//					Throwable exception) {
//				System.err.println("mqtt get state failed");
//			}
//		});
//
//		// 获取输入 Topic 下面所有订阅用户的 别名
//		YunBaManager.getAliasList("test_topic", new IMqttActionListener() {
//
//			@Override
//			public void onSuccess(IMqttToken asyncActionToken) {
//				JSONObject result = asyncActionToken.getResult();
//				try {
//					JSONArray topics = result.getJSONArray("alias");
//					System.out.println("mqtt getAliasList: "
//							+ topics.toString());
//				} catch (JSONException e) {
//
//				}
//			}
//
//			@Override
//			public void onFailure(IMqttToken asyncActionToken,
//					Throwable exception) {
//				if (exception instanceof MqttException) {
//					MqttException ex = (MqttException) exception;
//					String msg = "getAliasList failed with error code : "
//							+ ex.getReasonCode();
//					System.err.println(msg);
//				}
//			}
//		});
//
//		// 订阅某个频道上的用户的上、下线 及 订阅（或取消订阅）该频道的事件通知
//		YunBaManager.subscribePresence("test_topic", new IMqttActionListener() {
//
//			@Override
//			public void onSuccess(IMqttToken asyncActionToken) {
//				System.out.println("mqtt subscribePresence success");
//			}
//
//			@Override
//			public void onFailure(IMqttToken asyncActionToken,
//					Throwable exception) {
//				MqttException mqtt = (MqttException) exception;
//				System.err.println("mqtt subscribePresence failed:"
//						+ mqtt.getReasonCode());
//			}
//		});
	}

	public static <T> String join(T[] array, String cement) {
		StringBuilder builder = new StringBuilder();
		if (array == null || array.length == 0) {
			return null;
		}
		for (T t : array) {
			builder.append(t).append(cement);
		}
		builder.delete(builder.length() - cement.length(), builder.length());

		return builder.toString();
	}
}
