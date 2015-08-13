package org.eclipse.paho.client.mqttv3.logging;

import java.util.ResourceBundle;

/**
 * Just use to ingore the Log of MQTT source code, so It can save resource
 * 
 * @author quentin
 *
 */
public class MLogger implements Logger {

	@Override
	public void initialise(ResourceBundle messageCatalog, String loggerID,
			String resourceName) {
		return;
	}

	@Override
	public void setResourceName(String logContext) {
		return;
	}

	@Override
	public boolean isLoggable(int level) {
		return false;
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg) {
		return;
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable thrown) {
		return;
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg) {
		return;
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable thrown) {
		return;
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg) {
		return;
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable thrown) {
		return;
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg) {
		return;
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable thrown) {
		return;
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg) {
		return;
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable ex) {
		return;
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg) {
		return;		
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;		
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable ex) {
		return;		
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg) {
		return;		
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg,
			Object[] inserts) {
		return;		
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg,
			Object[] inserts, Throwable ex) {
		return;		
	}

	@Override
	public void log(int level, String sourceClass, String sourceMethod,
			String msg, Object[] inserts, Throwable thrown) {
		return;		
	}

	@Override
	public void trace(int level, String sourceClass, String sourceMethod,
			String msg, Object[] inserts, Throwable ex) {
		return;		
	}

	@Override
	public String formatMessage(String msg, Object[] inserts) {
		return "";
	}

	@Override
	public void dumpTrace() {
		return;		
	}

}
