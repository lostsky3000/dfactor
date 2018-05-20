package com.funtag.util.cipher;

import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

public final class DFCipherUtil {
	
	public static String getUUID(){
		return UUID.randomUUID().toString();
	}
	public static String getUUIDDigest(){
		String uuid = getUUID();
		return getMD5(uuid);
	}
	
	public static String getMD5(String src){
		return DigestUtils.md5Hex(src);
	}
	
	
}
