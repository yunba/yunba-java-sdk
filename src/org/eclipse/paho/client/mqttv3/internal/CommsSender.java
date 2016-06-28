/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;


import java.io.OutputStream;
import java.text.SimpleDateFormat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttOutputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;


public class CommsSender implements Runnable {
	/**
	 * Sends MQTT packets to the server on its own thread
	 */
	private boolean running 		= false;
	private Object lifecycle 		= new Object();
	private ClientState clientState = null;
	private MqttOutputStream out;
	private ClientComms clientComms = null;
	private CommsTokenStore tokenStore = null;
	private Thread 	sendThread		= null;
	
	private final static String className = "CommsSender";
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, className);
	
	public CommsSender(ClientComms clientComms, ClientState clientState, CommsTokenStore tokenStore, OutputStream out) {
		this.out = new MqttOutputStream(out);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
		log.setResourceName(clientComms.getClient().getClientId());
	}
	
	/**
	 * Starts up the Sender thread.
	 */
	public void start(String threadName) {
		synchronized (lifecycle) {
			if (running == false) {
				running = true;
				sendThread = new Thread(this, threadName);
				sendThread.start();
			}
		}
	}

	/**
	 * Stops the Sender's thread.  This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		
		synchronized (lifecycle) {
			//@TRACE 800=stopping sender
			log.fine(className,methodName,"800");
			if (running) {
				running = false;
				if (!Thread.currentThread().equals(sendThread)) {
					try {
						// first notify get routine to finish
						clientState.notifyQueueLock();
						// Wait for the thread to finish.
						sendThread.join();
					}
					catch (InterruptedException ex) {
					}
				}
			}
			sendThread=null;
			//@TRACE 801=stopped
			log.fine(className,methodName,"801");
		}
	}
	
	
	//force destory the thread
	public void destory() {
		running = false;
		try {
			sendThread.interrupt();
		} catch (Exception e) {
			//YLogger.e("CommsSender", "destory", e);
		}
		sendThread = null;
	}
	
	public void run() {
		final String methodName = "run";
		MqttWireMessage message = null;
		while (running && (out != null)) {
			try {
				message = clientState.get();
				if (message != null) {
					SimpleDateFormat time_formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
					String current_time_str = time_formatter.format(System.currentTimeMillis());
					System.out.println(current_time_str + " : Send msg to server: msgId = " + message.getKey() + " key = " + message.getTypeStr());
					//@TRACE 802=network send key={0} msg={1}
					log.fine(className,methodName,"802", new Object[] {message.getKey(),message});
                 
					if (message instanceof MqttAck) {
						out.write(message);
						out.flush();
					} else {
						MqttToken token = tokenStore.getToken(message);
						// While quiescing the tokenstore can be cleared so need 
						// to check for null for the case where clear occurs
						// while trying to send a message.
						if (token != null) {
							synchronized (token) {
								out.write(message);
								out.flush();
								clientState.notifySent(message);
							}
						} else {
							//YLogger.e(className, "null token :" + "msgId = " + message.getKey() + " key = " + message.getType());
						}
					}
				} else { // null message
					//@TRACE 803=get message returned null, stopping}
					log.fine(className,methodName,"803");

					running = false;
					clientComms.shutdownConnection(null, new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT));
				}
			} catch (MqttException me) {
			//	YLogger.e(className, methodName, me);
				handleRunException(message, me);
			} catch (Throwable ex) {		
			//	YLogger.e(className, methodName, ex);
				handleRunException(message, ex);	
			}
		} // end while
		
		//@TRACE 805=<
		log.fine(className, methodName,"805");

	}

	private void handleRunException(MqttWireMessage message, Throwable ex) {
		final String methodName = "handleRunException";
		//@TRACE 804=exception
		//log.fine(className,methodName,"804",null, ex);
		//YLogger.d(className, methodName);
		MqttException mex;
		if ( !(ex instanceof MqttException)) {
			mex = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ex);
		} else {
			mex = (MqttException)ex;
		}

		running = false;
		clientComms.shutdownConnection(null, mex);
	}
}
