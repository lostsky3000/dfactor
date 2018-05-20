package com.funtag.util.net;

import java.util.regex.Pattern;

public final class DFIpUtil {

	private static Pattern ptipv4 = Pattern.compile("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$");
	
	public static boolean isIPv4(String ip){
		return ptipv4.matcher(ip).matches();
	}
	
	public static long ipToNumber(String ip){
		try{
			String[] arr = ip.trim().split("\\.");
			long num = 0;
			num = num|Integer.parseInt(arr[0]);
			num = (num<<8)|Integer.parseInt(arr[1]);
			num = (num<<8)|Integer.parseInt(arr[2]);
			return (num<<8)|Integer.parseInt(arr[3]);
		}catch(Throwable e){
			throw e;
		}
	}
	public static String numberToIp(long ipNum){
		short[] arr = new short[4];
		for(int i=3; i>-1; --i){
			arr[i] = (short) (ipNum&0xFF);
			ipNum = ipNum>>8;
		}
		StringBuilder sb = new StringBuilder(15); //xxx.xxx.xxx.xxx
		sb.append(arr[0])
			.append('.').append(arr[1])
			.append('.').append(arr[2])
			.append('.').append(arr[3]);
		return sb.toString();
	}
	
	public static boolean isLanIP(String ip){
		long n = ipToNumber(ip);
		return isLanIP(n);
	}
	
	public static boolean isLanIP(long ip){
		if( (ip>=LAN_1_BEGIN&&ip<=LAN_1_END) 
				|| (ip>=LAN_2_BEGIN&&ip<=LAN_2_END)
				|| (ip>=LAN_3_BEGIN&&ip<=LAN_3_END)){
			return true;
		}
		return false;
	}
	
	private static final long LAN_1_BEGIN = ipToNumber("10.0.0.0");
	private static final long LAN_1_END = ipToNumber("10.255.255.255");
	private static final long LAN_2_BEGIN = ipToNumber("172.16.0.0");
	private static final long LAN_2_END = ipToNumber("172.31.255.255");
	private static final long LAN_3_BEGIN = ipToNumber("192.168.0.0");
	private static final long LAN_3_END = ipToNumber("192.168.255.255");
	/*10.0.0.0/8：10.0.0.0～10.255.255.255 
	　　172.16.0.0/12：172.16.0.0～172.31.255.255 
	　　192.168.0.0/16：192.168.0.0～192.168.255.255
	*/
}
