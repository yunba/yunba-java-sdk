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

import io.yunba.java.manager.YunBaManager;
import io.yunba.java.util.MqttUtil;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttExpand;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttExpandAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttInputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRec;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRel;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * Receives MQTT packets from the server.
 */
public class CommsReceiver implements Runnable {
	private boolean running = false;
	private Object lifecycle = new Object();
	private ClientState clientState = null;
	private ClientComms clientComms = null;
	private MqttInputStream in;
	private CommsTokenStore tokenStore = null;
	private Thread recThread = null;

	private final static String className = "CommsReceiver";
	private Logger log = LoggerFactory.getLogger(
			LoggerFactory.MQTT_CLIENT_MSG_CAT, className);

	public CommsReceiver(ClientComms clientComms, ClientState clientState,
			CommsTokenStore tokenStore, InputStream in) {
		this.in = new MqttInputStream(in);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
		log.setResourceName(clientComms.getClient().getClientId());
	}

	/**
	 * Starts up the Receiver's thread.
	 */
	public void start(String threadName) {
		final String methodName = "start";
		// @TRACE 855=starting
		log.fine(className, methodName, "855");
		synchronized (lifecycle) {
			if (running == false) {
				running = true;
				recThread = new Thread(this, threadName);
				recThread.start();
			}
		}
	}

	/**
	 * Stops the Receiver's thread. This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		synchronized (lifecycle) {
			// @TRACE 850=stopping
			log.fine(className, methodName, "850");
			if (running) {
				running = false;
				if (!Thread.currentThread().equals(recThread)) {
					try {
						// Wait for the thread to finish.
						recThread.join();
					} catch (InterruptedException ex) {
					}
				}
			}
		}
		recThread = null;
		// @TRACE 851=stopped
		log.fine(className, methodName, "851");
	}

	// force destory the thread
	public void destory() {
		running = false;
		try {
			recThread.interrupt();
		} catch (Exception e) {
			// YLogger.e("CommsReceiver", "destory", e);
		}
		recThread = null;
	}

	/**
	 * Run loop to receive messages from the server.
	 */
	public void run() {
		final String methodName = "run";
		MqttToken token = null;

		while (running && (in != null)) {
			try {
				// @TRACE 852=network read message
				log.fine(className, methodName, "852");
				MqttWireMessage message = in.readMqttWireMessage();
				if (null != message) {
					SimpleDateFormat time_formatter = new SimpleDateFormat(
							"yyyy-MM-dd_HH:mm:ss.SSS");
					String current_time_str = time_formatter.format(System
							.currentTimeMillis());
					System.out.println(current_time_str
							+ " : Receiver msg key = " + message.getKey()
							+ " msgID = " + message.getMessageId()
							+ " getType = " + message.getTypeStr());
				}
				if (message instanceof MqttAck) {
					token = tokenStore.getToken(message);
					if (token != null) {
						synchronized (token) {
							// Ensure the notify processing is done under a lock
							// on the token
							// This ensures that the send processing can
							// complete before the
							// receive processing starts! ( request and ack and
							// ack processing
							// can occur before request processing is complete
							// if not!
							if (message instanceof MqttExpandAck) {
								MqttExpandAck mqttExpand = (MqttExpandAck) message;
								clientState.notifyReceivedExpandAck(mqttExpand);
							} else {
								clientState.notifyReceivedAck((MqttAck) message);
							}
						}
					} else {

						// **************quentin for delete
						// MqttPubRec***********//
						// YLogger.e("[MQTT]",
						// "CommsReceiver - the token is null");
						if (message instanceof MqttPubRec) {
							// YLogger.d("[MQTT]",
							// "message instanceof MqttPubRec");
							MqttPubRec pubRec = (MqttPubRec) (message);
							MqttPubRel rel = new MqttPubRel(pubRec);
							MqttToken token2 = new MqttToken(MqttUtil.getCid());
							tokenStore.saveToken(token2, message);
						}

						continue;
						// **************quentin for delete
						// MqttPubRec***********//
						// It its an ack and there is no token then something is
						// not right.
						// An ack should always have a token assoicated with it.
						// throw new
						// MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
					}
				} else {
					// A new message has arrived
					clientState.notifyReceivedMsg(message);
				}
			} catch (MqttException ex) {
				// @TRACE 856=Stopping, MQttException
				log.fine(className, methodName, "856", null, ex);
				// YLogger.e(className, "MqttException", ex);
				running = false;
				// Token maybe null but that is handled in shutdown
				if (null != clientComms)
					clientComms.shutdownConnection(token, ex);
			} catch (IOException ioe) {
				// @TRACE 853=Stopping due to IOException
				log.fine(className, methodName, "853");
				// YLogger.e(className, "IOException", ioe);
				running = false;
				// An EOFException could be raised if the broker processes the
				// DISCONNECT and ends the socket before we complete. As such,
				// only shutdown the connection if we're not already shutting
				// down.
				if (!clientComms.isDisconnecting()) {
					if (null != clientComms)
						clientComms
								.shutdownConnection(
										token,
										new MqttException(
												MqttException.REASON_CODE_CONNECTION_LOST,
												ioe));
				} // else {
			} catch (Exception ioe) {
				// @TRACE 853=Stopping due to IOException
				log.fine(className, methodName, "853");
				// YLogger.e(className, "Exception", ioe);
				running = false;
				// An EOFException could be raised if the broker processes the
				// DISCONNECT and ends the socket before we complete. As such,
				// only shutdown the connection if we're not already shutting
				// down.

				if (null != clientComms)
					clientComms.shutdownConnection(token, new MqttException(
							MqttException.REASON_CODE_CLIENT_EXCEPTION, ioe));

			}
		}

		// @TRACE 854=<
		log.fine(className, methodName, "854");
	}

	public boolean isRunning() {
		return running;
	}
}
