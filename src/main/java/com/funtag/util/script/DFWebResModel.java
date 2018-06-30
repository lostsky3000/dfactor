package com.funtag.util.script;


import fun.lib.actor.api.http.DFHttpContentType;

public final class DFWebResModel {

	public final String path;
	public final byte[] data;
	public final int dataLen;
	public final String contentType;  //default
	
	
	public DFWebResModel(String path, String contentType, byte[] data, int dataLen) {
		this.path = path;
		if(contentType != null){
			this.contentType = contentType;
		}else{
			this.contentType = DFHttpContentType.OCTET_STREAM;
		}
		this.data = data;
		this.dataLen = dataLen;
	}
	
}
