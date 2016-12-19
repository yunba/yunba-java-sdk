package io.yunba.java.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CommonUtil {
	private final static String HEX = "0123456789ABCDEF";

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

	/**
	 * 0:未尝试 1:全部失败 2:dns失败，sys成功 3:dns成功
	 */
	public static byte DNS_REASON = 0;
	
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
