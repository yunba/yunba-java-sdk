package io.yunba.java.core;

import io.yunba.java.manager.YunBaManager;
import io.yunba.java.util.CommonUtil;
import io.yunba.java.util.MqttUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;

public class MQTTStack {
	public static int mInterval = (1000 * 5 - 20);
	private static final int HANDLER_PUBLIC_TIMEOUT = 1001;
	private static final int HANDLER_SUB_TIMEOUT = 1002;
	private static final int HANDLER_UNSUB_TIMEOUT = 1003;
	private static final int HANDLER_EXPAND_TIMEOUT = 1004;
	private static final int CALLBACK_TIMEOUT = 30000;
	private static final int CONNECT_ACK_TIMEOUT = 10000;
	private static final int PING_INTERVAL = 5 * 1000;
	private static final int MAX_PING_LOST_COUNT = 3;
	private MqttAsyncClient mMqttClient = null;
	private MQTTState mConnectState = null;
	private MessageDelivery mDelivery = null;
	private ReentrantLock restartLock = new ReentrantLock();
	private static long mLastPingTime = 0;
	private int mPingLostCount = 0;
	private String appKey;
	private Timer mTimer = new Timer();
	private Timer mPingTimer = null;
	private TimerTask mPingTimerTask = null;
	private MQTTThread mThread = null;
	private MQTTMsgHandleThread mMsgHandleThread = null;
	protected boolean runningFlag = false;
	private AtomicBoolean mPingFailed = new AtomicBoolean(false);
	private AtomicBoolean mRunningStoped = new AtomicBoolean(false);
	private static ExecutorService executor = Executors
			.newSingleThreadExecutor();
	private LinkedBlockingQueue<MQTTMessage> mCacheMessages = new LinkedBlockingQueue<MQTTMessage>();
	private ReentrantLock pingLock = new ReentrantLock();

	public enum MQTTStatus {
		CONNECTED, CONNECTING, IDLE, STOPED, UNKNOWN
	}

	public MQTTStack(String appKey) {
		this(appKey, null);
	}

	public MQTTStack(String appkey, MessageDelivery delivery) {
		this.appKey = appkey;
		this.mDelivery = delivery == null ? new EventBusMessageDelivery()
				: delivery;
	}

	public void start() {
		mConnectState = new MQTTIdleState();
		mMsgHandleThread = new MQTTMsgHandleThread(mCacheMessages);
		restartConnectThread();
	}

	private void restartConnectThread() {
		restartLock.lock();
		try {
			if (mConnectState instanceof MQTTConnectedState
					|| mConnectState instanceof MQTTConnectingState)
				return;
			if (null != mThread) {
				if (!mThread.isAlive() && !isRunningFlag()) {
					try {
						mThread.join();
						mThread = new MQTTThread();
						mThread.start();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					mThread = new MQTTThread();
					mThread.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			try {
				restartLock.unlock();
			} catch (Exception e) {
			}
		}
	}

	synchronized public void addMQTTMessage(MQTTMessage cache) {
		mCacheMessages.add(cache);
	}

	public void handleStopAction() {
		if (mRunningStoped.get()) {
			return;
		}
		if (mRunningStoped.compareAndSet(false, true)) {
			mConnectState = new MQTTStopedState();
			if (null != mPingTimer) {
				mPingTimer.purge();
				mPingTimer.cancel();
			}
			if (null != mThread) {
				mThread.interrupt();
			}
			if (null != mMqttClient) {
				try {
					mMqttClient.disconnect(0);
					mMqttClient.destory();
				} catch (MqttException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void handleResumeAction() {
		if (mRunningStoped.compareAndSet(true, false)) {
			mTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					mConnectState.ping(true);
				}
			}, 2000);
		}
	}

	public boolean isStop() {
		MQTTStatus currentStatus = getMQTTStatus();
		if (MQTTStatus.IDLE == currentStatus
				|| MQTTStatus.STOPED == currentStatus) {
			return true;
		}
		return false;
	}

	public void handleKeepLive(int delay) {
		if(mRunningStoped.get()) return;
		if (0 == delay) {
			mConnectState.ping(false);
		} else {
			mPingTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					mConnectState.ping(true);
				}
			}, delay);
		}
	}

	private MQTTStatus getMQTTStatus() {
		if (null == mMqttClient || null == mThread)
			return MQTTStatus.IDLE;
		if (mMqttClient.isConnected())
			return MQTTStatus.CONNECTED;
		if (mMqttClient.isConnecting() || mThread.isAlive())
			return MQTTStatus.CONNECTING;
		else {
			return MQTTStatus.STOPED;
		}
	}

	public boolean isRunningFlag() {
		return runningFlag;
	}

	public synchronized void setRunFlag(boolean runFlag) {
		this.runningFlag = runFlag;
	}

	public static byte[] conn_report = new byte[5];
	ReentrantLock lock = new ReentrantLock();

	private void connectMQTTServer(final MQTTThread thread) {
		if (isMQTTConnected())
			return;
		if (mConnectState instanceof MQTTConnectedState
				|| mConnectState instanceof MQTTConnectingState) {
			return;
		}
		try {
			lock.lock();
			String clientId = MqttUtil.getCid();
			String serverURI = MqttUtil.getBroker();
			if (MqttUtil.isEmpty(clientId))
				throw ExceptionHelper
						.createMqttException(MqttException.REASON_CODE_INVALID_CLIENT_ID);
			if (MqttUtil.isEmpty(serverURI))
				throw ExceptionHelper
						.createMqttException(MqttException.REASON_CODE_BROKER_UNAVAILABLE);

			mMqttClient = new MqttAsyncClient(serverURI, clientId, null);
			mMqttClient.setCallback(new PushCallback());

			final TimerTask connectTimeOutTask = getConnectTimeOutTask();
			mTimer.schedule(connectTimeOutTask, CONNECT_ACK_TIMEOUT);
			mConnectState = new MQTTConnectingState();

			mMqttClient.connect(MqttUtil.getOpts(), null,
					new IMqttActionListener() {

						@Override
						public void onSuccess(IMqttToken asyncActionToken) {
							connectTimeOutTask.cancel();
							mDelivery.postConnected();
							setRunFlag(false);
							mLastPingTime = System.currentTimeMillis();
							mConnectState = new MQTTConnectedState();
							if (!mMsgHandleThread.isAlive()) {
								mMsgHandleThread.start();
							}
							schedulePing();
						}

						@Override
						public void onFailure(IMqttToken asyncActionToken,
								Throwable exception) {
							connectTimeOutTask.cancel();
							setRunFlag(false);
							mConnectState = new MQTTStopedState();
							handleConnectFailed();
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	private void schedulePing() {
		mPingFailed.compareAndSet(true, false);
		mPingTimer = new Timer();
		mPingTimer.schedule(mPingTimerTask = new TimerTask() {

			@Override
			public void run() {
				handleKeepLive(0);
			}
		}, mInterval, PING_INTERVAL);
	}

	private boolean isMQTTConnected() {
		return (null != mMqttClient && mMqttClient.isConnected());
	}

	private int mDisconnectedTimes = 0;

	private void tryReconnect() {
		int delayTime = (int) (Math.pow(2, mDisconnectedTimes) * 3 * 1000);
		mDisconnectedTimes += 1;
		if (delayTime > mInterval * 500)
			delayTime = mInterval * 500;
		if (mDisconnectedTimes <= 4) {
			mTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					if (mConnectState != null) {
						mConnectState.ping(false);
					}
				}
			}, delayTime);
		}
	}

	private void handlePingFailed() {
		if (isRunningFlag())
			return;
		mPingTimer.cancel();
		try {

			if (pingLock.isLocked()) {
				return;
			}
			pingLock.lock();
			mConnectState = new MQTTStopedState();
			try {
				if (null != mMqttClient) {
					mMqttClient.disconnect(0);
				}
			} catch (MqttException e) {
			}
			try {
				if (null != mMqttClient) {
					mMqttClient.destory(); // Ping failed, The socket may be
											// connect, But it can not work, We
											// need kill it by force
				}
			} catch (Exception e) {
			}
			restartConnectThread();
		} finally {
			try {
				pingLock.unlock();
			} catch (Exception e) {

			}
		}
	}

	private void handleConnectFailed() {
		releaseConnecThread();
		tryReconnect();
	}

	private void handleConnectTimeOut() {
		setRunFlag(false);
		handlePingFailed();
	}

	private void releaseConnecThread() {
		clearMQTTClient();
	}

	private boolean checkIsSetAliasAction(final IMqttToken asyncActionToken) {
		String topic = getTopic(asyncActionToken);
		if (!CommonUtil.isEmpty(topic)
				&& topic.equals(Constants.TOPIC_SET_ALIAS)) {
			return true;
		}
		return false;
	}

	private String getTopic(IMqttToken asyncActionToken) {
		if (null != asyncActionToken) {
			String[] topics = asyncActionToken.getTopics();
			if (null != topics && topics.length > 0) {
				return topics[0];
			}
		}
		return null;
	}

	private void clearMQTTClient() {
		mConnectState = new MQTTStopedState();
		if (null != mMqttClient) {
			try {
				mPingTimer.cancel();
				mPingTimerTask.cancel();
				mMqttClient.close();
			} catch (Exception e) {
			}
		}
	}

	public class PushCallback implements MqttCallback {

		@Override
		public void connectionLost(Throwable cause) {
			releaseConnecThread();
			tryReconnect();
			mDelivery.postDisConnected();
		}

		@Override
		public void messageArrived(String topic, MqttMessage message)
				throws Exception {
			mLastPingTime = System.currentTimeMillis();// do not need ping again
														// in 10s
			String msgStr = new String(message.getPayload(), Constants.UTF_8);
			if (!CommonUtil.isEmpty(topic) && topic.endsWith("/p")) {
				topic = topic.substring(0, topic.lastIndexOf("/p"));
				mDelivery.postReceivedPresence(topic, msgStr);
			} else {
				mDelivery.postReceivedMessage(topic, msgStr);
			}
		}

		@Override
		public void presenceMessageArrived(String topic, MqttMessage message)
				throws Exception {
			String msgStr = new String(message.getPayload(), Constants.UTF_8);
			mDelivery.postReceivedPresence(topic, msgStr);
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {

		}
	}

	private class MQTTMsgHandleThread extends Thread {
		private final BlockingQueue<MQTTMessage> mQueue;
		private volatile boolean mQuit = false;

		public MQTTMsgHandleThread(BlockingQueue<MQTTMessage> queue) {
			mQueue = queue;
		}

		@Override
		public void run() {
			while (true) {
				try {
					MQTTMessage msg = mQueue.take();
					handMQTTMsg(msg);
				} catch (InterruptedException e) {
					e.printStackTrace();
					if (mQuit) {
						return;
					}
					continue;
				}
			}
		}

		private void handMQTTMsg(MQTTMessage msg) {
			mConnectState.handMQTTMsg(msg);
		}

		public void quit() {
			mQuit = true;
			interrupt();
		}
	}

	private class MQTTThread extends Thread {
		public MQTTThread() {
			setRunFlag(true);
		}

		@Override
		public void run() {
			JSONObject obj = new JSONObject();
			try {
				obj.put("a", MQTTStack.this.appKey);
				obj.put("p", 3);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			MqttUtil.register(obj);
			connectMQTTServer(this);
		}
	}

	private TimerTask getConnectTimeOutTask() {
		return new TimerTask() {

			@Override
			public void run() {
				handleConnectTimeOut();
			}
		};
	}

	public abstract class MQTTState {

		public abstract void publish(MQTTMessage msg);

		public abstract void subscribe(MQTTMessage msg);

		public abstract void unsubscribe(MQTTMessage msg);

		public abstract void expand(MQTTMessage msg);

		public abstract void ping(boolean isForcePing);

		protected TimerTask getTimeOutTask(final int hanldeTimeOut,
				final MQTTMessage msg) {
			if (Constants.TOPIC_GET_ALIAS.equals(msg.topic)) {
				return new TimerTask() {
					@Override
					public void run() {
						handleGetAliasTimeOut();
					}
				};
			} else if (null != msg.callback) {
				return new TimerTask() {
					@Override
					public void run() {
						handleTimeOut(hanldeTimeOut, msg);
					}
				};
			}
			return new TimerTask() {

				@Override
				public void run() {
					return;
				}
			};
		}

		protected TimerTask getPingTimeOutTask() {
			return new TimerTask() {

				@Override
				public void run() {
					mPingLostCount += 1;
					if (mPingLostCount < MAX_PING_LOST_COUNT) {
						return;
					}
					if (mPingFailed.compareAndSet(false, true)) {
						handlePingTimeOut();
					}
				}
			};
		}

		protected TimerTask getExpandTimeOutTask(final MQTTMessage msg) {
			return new TimerTask() {

				@Override
				public void run() {
					if (msg.callbackId != -1
							&& YunBaManager.getTagAliasCallback(msg.callbackId) != null) {
						handleTimeOut(HANDLER_EXPAND_TIMEOUT, msg);
					}
				}
			};
		}

		public void handMQTTMsg(MQTTMessage msg) {
			switch (msg.type) {
			case MQTTMessage.TYPE_PUBLISH:
				publish(msg);
				break;

			case MQTTMessage.TYPE_SUBSCRIBE:
				subscribe(msg);
				break;

			case MQTTMessage.TYPE_UNSUBSCRIBE:
				unsubscribe(msg);
				break;
			case MQTTMessage.TYPE_EXPAND:
				expand(msg);
				break;
			default:
				break;
			}
		}

		protected void handleTimeOut(int handlerTimeout, MQTTMessage msg) {
			final IMqttActionListener callback = msg.callback;
			if (null != callback) {
				executor.execute(new Runnable() {
					public void run() {
						callback.onFailure(new MqttToken(), new MqttException(
								MqttException.REASON_CODE_CLIENT_TIMEOUT));
					}
				});
			}
		}

		protected void handleDisconnect(MQTTMessage msg) {
			final IMqttActionListener callback = msg.callback;
			if (null != callback) {
				executor.execute(new Runnable() {
					public void run() {
						callback.onFailure(
								new MqttToken(),
								new MqttException(
										MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED));
					}
				});
			}
		}

		protected void handlePingTimeOut() {
			handlePingFailed();
		}

		private void handleGetAliasTimeOut() {
		}
	}

	public class MQTTConnectedState extends MQTTState {

		@Override
		public void publish(final MQTTMessage msg) {
			final TimerTask timeoutTask = getTimeOutTask(
					HANDLER_PUBLIC_TIMEOUT, msg);
			if (timeoutTask != null) {
				mTimer.schedule(timeoutTask, CALLBACK_TIMEOUT);
			}
			try {
				byte[] payload = null;
				if (null != msg && !CommonUtil.isEmpty(msg.msg)) {
					payload = msg.msg.getBytes("utf-8");
				} else {
					payload = "".getBytes("utf-8");
				}
				mMqttClient.publish(msg.topic, payload, msg.qos, false, null,
						new IMqttActionListener() {
							@Override
							public void onSuccess(
									final IMqttToken asyncActionToken) {
								timeoutTask.cancel();
								// do not need ping again in 10s
								mLastPingTime = System.currentTimeMillis();
								final IMqttActionListener listener = msg.callback;
								if (null != listener) {
									executor.execute(new Runnable() {
										public void run() {
											if (checkIsSetAliasAction(asyncActionToken)) {// for
												MqttToken mqttToken = new MqttToken();
												mqttToken.setAlias(msg.msg);
												listener.onSuccess(mqttToken);
											} else {
												listener.onSuccess(asyncActionToken);
											}

										}
									});
								}
							}

							@Override
							public void onFailure(
									final IMqttToken asyncActionToken,
									final Throwable exception) {
								timeoutTask.cancel();
								final IMqttActionListener listener = msg.callback;
								if (null != listener) {
									executor.execute(new Runnable() {
										public void run() {
											if (checkIsSetAliasAction(asyncActionToken)) {
												MqttToken mqttToken = new MqttToken();
												mqttToken.setAlias(msg.msg);
												listener.onFailure(mqttToken,
														exception);
											} else {
												listener.onFailure(
														asyncActionToken,
														exception);
											}
										}
									});
								}
							}
						});
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			} catch (MqttException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void expand(MQTTMessage msg) {
			final TimerTask timeoutTask = getExpandTimeOutTask(msg);
			mTimer.schedule(timeoutTask, CALLBACK_TIMEOUT);
			try {
				mMqttClient.expand(msg);
			} catch (Exception e) {
			}
		}

		@Override
		public void subscribe(final MQTTMessage msg) {
			String[] topics = msg.topic.split("\\$\\$\\$");
			int qos[] = new int[topics.length];
			for (int i = 0; i < qos.length; i++) {
				qos[i] = msg.qos;
			}
			final TimerTask timeoutTask = getTimeOutTask(HANDLER_SUB_TIMEOUT,
					msg);
			mTimer.schedule(timeoutTask, CALLBACK_TIMEOUT);

			try {
				mMqttClient.subscribe(topics, qos, null,
						new IMqttActionListener() {

							public void onSuccess(
									final IMqttToken asyncActionToken) {
								timeoutTask.cancel();
								// do not need ping again in 10s
								mLastPingTime = System.currentTimeMillis();
								final IMqttActionListener listener = msg.callback;
								if (null != listener) {
									executor.execute(new Runnable() {
										public void run() {
											listener.onSuccess(asyncActionToken);
										}
									});
								}
							}

							@Override
							public void onFailure(
									final IMqttToken asyncActionToken,
									final Throwable exception) {
								timeoutTask.cancel();
								final IMqttActionListener listener = msg.callback;
								if (null != listener) {
									executor.execute(new Runnable() {
										public void run() {
											listener.onFailure(
													asyncActionToken, exception);
										}
									});
								}
							}
						});
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void unsubscribe(final MQTTMessage msg) {
			final TimerTask timeOutTask = getTimeOutTask(HANDLER_UNSUB_TIMEOUT,
					msg);
			mTimer.schedule(timeOutTask, CALLBACK_TIMEOUT);

			try {
				String[] topics = msg.topic.split("\\$\\$\\$");
				mMqttClient.unsubscribe(topics, null,
						new IMqttActionListener() {
							public void onSuccess(
									final IMqttToken asyncActionToken) {
								timeOutTask.cancel();
								// do not need ping again in 10s
								mLastPingTime = System.currentTimeMillis();
								final IMqttActionListener callback = msg.callback;
								if (null != callback) {
									executor.execute(new Runnable() {
										public void run() {
											callback.onSuccess(asyncActionToken);
										}
									});
								}
								timeOutTask.cancel();
							}

							@Override
							public void onFailure(
									final IMqttToken asyncActionToken,
									final Throwable exception) {
								timeOutTask.cancel();
								final IMqttActionListener callback = msg.callback;
								if (null != callback) {
									executor.execute(new Runnable() {
										public void run() {
											callback.onFailure(
													asyncActionToken, exception);
										}
									});
								}
								timeOutTask.cancel();
							}
						});

			} catch (final Exception e) {
				final IMqttActionListener callback = msg.callback;
				if (null != callback) {
					executor.execute(new Runnable() {
						public void run() {
							callback.onFailure(new MqttToken(), e);
						}
					});
					timeOutTask.cancel();
				}
			}
		}

		public void ping(boolean isForcePing) {
			try {
				// no need to ping, Because it have succeed recently
				if (!isForcePing
						&& (Math.abs(System.currentTimeMillis() - mLastPingTime)) < 15 * 1000)
					return;
				final TimerTask pingTimeOutTask = getPingTimeOutTask();
				mTimer.schedule(pingTimeOutTask, CONNECT_ACK_TIMEOUT);

				mMqttClient.ping(new IMqttActionListener() {

					@Override
					public void onSuccess(IMqttToken asyncActionToken) {
						pingTimeOutTask.cancel();
						mPingLostCount = 0;
						mLastPingTime = System.currentTimeMillis();
					}

					@Override
					public void onFailure(IMqttToken asyncActionToken,
							Throwable exception) {
						pingTimeOutTask.cancel();
						if (mPingFailed.compareAndSet(false, true)) {
							handlePingFailed();
						}
					}
				});
			} catch (Exception e) {
			}
		}
	}

	public class MQTTConnectingState extends MQTTState {

		@Override
		public void publish(MQTTMessage msg) {
			mCacheMessages.add(msg);
		}

		@Override
		public void subscribe(MQTTMessage msg) {
			mCacheMessages.add(msg);
		}

		@Override
		public void unsubscribe(MQTTMessage msg) {
			mCacheMessages.add(msg);
		}

		@Override
		public void expand(MQTTMessage msg) {
			mCacheMessages.add(msg);
		}

		@Override
		public void ping(boolean isForcePing) {
		}

	}

	public class MQTTIdleState extends MQTTState {

		@Override
		public void publish(MQTTMessage msg) {
			handleDisconnect(msg);
		}

		@Override
		public void subscribe(MQTTMessage msg) {
			handleDisconnect(msg);
		}

		@Override
		public void unsubscribe(MQTTMessage msg) {
			handleDisconnect(msg);
		}

		@Override
		public void expand(MQTTMessage msg) {
			handleDisconnect(msg);
		}

		@Override
		public void ping(boolean isForcePing) {
			if (!isForcePing
					&& (Math.abs(System.currentTimeMillis() - mLastPingTime)) < 15 * 1000)
				return;
			restartConnectThread();
		}

	}

	public class MQTTStopedState extends MQTTIdleState {
	}

}
