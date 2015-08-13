package org.eclipse.paho.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Properties;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttUtil {
	public final static String RIGSTER_URL = "http://reg.yunba.io:8383/device/reg/";
	public static  String BROKER_URL = "tcp://182.92.154.3:1883";
	public static final String		MQTT_HOST = "b1.wei60.com";
	public static final String		DATA_FILE = ".opts";
	private static Properties pro;
	public static void register(JSONObject obj ) {
	    pro = getOptsPro();
		if(null != pro && pro.getProperty("a", null).equals(obj.optString("a", ""))) {
			return;
		}
		
		URL url = null;
		HttpURLConnection connection = null;
		OutputStream out = null ;
		BufferedReader reader = null;
		try {
			url = new URL(RIGSTER_URL);

			connection = (HttpURLConnection) url
					.openConnection();

			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);

			connection.setRequestProperty("Content-Type", "application/json");
			// connection.setRequestProperty("Content-Type", "text/xml");
			connection.setRequestProperty("Accept", "application/json");
			connection.connect();
           // System.out.println(obj.toString());
			out = connection.getOutputStream();
			out.write(obj.toString().getBytes());
			out.flush();
			

			reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String lines;
			StringBuffer sb = new StringBuffer("");
			while ((lines = reader.readLine()) != null) {
				lines = new String(lines.getBytes(), "utf-8"); // 中文
				sb.append(lines);
			}
			//System.out.println(sb.toString());
			JSONObject obj2= new JSONObject(sb.toString());
			obj2.put("a", obj.get("a"));
		    saveOpts(obj2);
			//return sb.toString();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally{
			try {
				if (null != out)out.close();
			} catch (Exception e) {
			}
			try {
				if (null != reader)reader.close();
			} catch (Exception e) {
			}
			connection.disconnect();
		}
	}

	private static void saveOpts(JSONObject obj) {
		String fileDir = System.getProperty("user.dir")  ;
		File file = new File(fileDir, DATA_FILE);
		FileOutputStream fos = null;
		try {
			 fos =new FileOutputStream(file);
			 Properties pro = new Properties();
			 Iterator it = obj.keys();  
	            while (it.hasNext()) {  
	            	 String key = (String) it.next();  
	                 String value = obj.getString(key); 
	            	pro.setProperty(key, value);
	            }
			 pro.store(fos," ");  
		     fos.close(); 
		} catch (Exception e) {
			
		} finally {
			try {
				if (null != fos) fos.close();
			} catch (IOException e) {
				
			}
		}
	}

	
	public static Properties getOptsPro(){  
		if (null != pro) return pro;
		Reader fis = null;
        try {  
        	String file = System.getProperty("user.dir") + File.separator + DATA_FILE;
    		//File file = new File(fileDir, DATA_FILE); 
        	fis = new FileReader(file);
            pro = new Properties();  
            pro.load(fis);
            String uName = pro.getProperty("u", null);
			String pwd = pro.getProperty("p", null);
			String cid = pro.getProperty("c", null);
			String appkey = pro.getProperty("a", null);
			
			if(isEmpty(uName) || isEmpty(pwd) || isEmpty(cid) || isEmpty(appkey)) {
				return null;
			}
            return pro;
            
        } catch (Exception e) {  
           // System.out.println("文件不存在:"+ e.toString());  
            return null;
        }   finally {
        	if (null !=fis) {
        		try {
					fis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	
        }
    }  
      
	public static MqttConnectOptions getOpts() {
		Properties pro = getOptsPro();
		if(null != pro) {
			String uName = pro.getProperty("u", null);
			String pwd = pro.getProperty("p", null);
			String cid = pro.getProperty("c", null);
			
			if(!isEmpty(uName) && !isEmpty(pwd) && !isEmpty(cid)) {
				MqttConnectOptions opts = new MqttConnectOptions();
				opts.setKeepAliveInterval(200);
				opts.setUserName(uName);
				opts.setPassword(pwd.toCharArray());
				return opts;
			}
			
		}
		return null;
	}
	
	public static String getCid() {
		Properties pro = getOptsPro();
		if(null != pro) {
		
			String cid = pro.getProperty("c", null);
			
			if( !isEmpty(cid)) {
				return cid;
			}
			
		}
		return null;
	}
	/**
	 * will trim the string
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isEmpty(String s) {
	    if (null == s)
	        return true;
	    if (s.length() == 0)
	        return true;
	    if (s.trim().length() == 0)
	        return true;
	    return false;
	}

	public static String hostToIp(String host) {
		InetAddress address;
		try {
			address = InetAddress.getByName(host);
			return address.getHostAddress(); 
		} catch (UnknownHostException e) {
			return null;
		} 
	
	}
	
	public static String getBroker() {
		String ip = hostToIp(MQTT_HOST);
		if(isEmpty(ip)) return BROKER_URL;
		return new StringBuilder().append("tcp://").append(ip).append(":1883").toString();
	}

	
	
	public static void main(String[] args) throws JSONException, MqttException {
		
	
		final MqttAsyncClient mqtt = MqttAsyncClient.createMqttClient("52fcc04c4dc903d66d6f8f92");
//		System.out.println(getCid());
		
		
	}
}
