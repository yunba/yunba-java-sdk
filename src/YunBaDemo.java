

import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class YunBaDemo {

	public static void main(String[] args)  {
		try {
			
			final MqttAsyncClient mqttAsyncClient = MqttAsyncClient.createMqttClient("530bfb6775fce3cd4f363b36");
			mqttAsyncClient.setCallback(new MqttCallback() {

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					System.out.println("mqtt receive topic = " + topic + " msg = " + new String(message.getPayload())) ;//reciver msg from yunba server
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
				}

				@Override
				public void connectionLost(Throwable cause) {
					System.out.println("mqtt connectionLost");
				}
			});
			//API  connect to yunba server
			mqttAsyncClient.connect(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken arg0) {
					System.out.println("mqtt connect success");
					
					try {
						
						mqttAsyncClient.subscribe("test_topic", 1, null, new IMqttActionListener() {

							@Override
							public void onSuccess(IMqttToken asyncActionToken) {
								System.out.println("mqtt succeed subscribe: "+ join(asyncActionToken.getTopics(), ","));
							    try {
							    	
							    	
									mqttAsyncClient.publish("test_topic", "test_msg".getBytes(), 1, false,
											null, new IMqttActionListener() {

												@Override
												public void onFailure(IMqttToken arg0, Throwable arg1) {
													System.out.println("mqtt publish failed");
													
												}

												@Override
												public void onSuccess(IMqttToken arg0) {
													System.out.println("mqtt publish success");
												}
										
									});
								} catch (MqttPersistenceException e) {
									e.printStackTrace();
								} catch (MqttException e) {
									e.printStackTrace();
								}
							}

							@Override
							public void onFailure(IMqttToken asyncActionToken,Throwable exception) {
								if (exception instanceof MqttException) {
									MqttException ex = (MqttException)exception;
									System.err.println("connect to server failed with the error code = " + ex.getReasonCode());
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
			        
					try {
						mqttAsyncClient.setAlias("java_alias", new IMqttActionListener() {
							
							@Override
							public void onSuccess(IMqttToken arg0) {
								try {
									mqttAsyncClient.getAlias(new IMqttActionListener() {
										
										@Override
										public void onSuccess(IMqttToken token) {
											System.out.println("get alias = " +  token.getAlias());
											
										}
										
										@Override
										public void onFailure(IMqttToken arg0, Throwable arg1) {
											if(arg1 instanceof MqttException) {
												MqttException mqtt = (MqttException)arg1;
												System.out.println("get alias failed:" + mqtt.getReasonCode());
											}
								
										}
									});
									mqttAsyncClient.publishToAlias("java_alias", "msg to java_alaias", null);
									mqttAsyncClient.getTopics("java_alias", new IMqttActionListener() {
										
										@Override
										public void onSuccess(IMqttToken token) {
											System.out.println("get getTopics = " + token.getResult());
										}
										
										@Override
										public void onFailure(IMqttToken arg0, Throwable arg1) {
									
											
										}
									});
								} catch (MqttPersistenceException e) {
									e.printStackTrace();
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							
							@Override
							public void onFailure(IMqttToken arg0, Throwable arg1) {
								
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}

				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1) {
					System.out.println("mqtt connect failed" + arg1.getMessage());

				}
			});
       
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}
	
	public static <T> String join(T[] array, String cement) {
	    StringBuilder builder = new StringBuilder();

	    if(array == null || array.length == 0) {
	        return null;
	    }
	    for (T t : array) {
	        builder.append(t).append(cement);
	    }

	    builder.delete(builder.length() - cement.length(), builder.length());

	    return builder.toString();
	}
}
