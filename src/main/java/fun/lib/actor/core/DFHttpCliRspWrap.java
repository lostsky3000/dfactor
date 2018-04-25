package fun.lib.actor.core;

import java.util.Iterator;
import java.util.Map.Entry;

import fun.lib.actor.api.http.DFHttpCliResponse;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

public final class DFHttpCliRspWrap implements DFHttpCliResponse{
	
	private final int statusCode;
	private final HttpHeaders headers;
	private final ByteBuf dataBuf;
	private final String dataStr;
	private final String contentType;
	private final int contentLen;
	private final boolean isBinary;
	
	public DFHttpCliRspWrap(int statusCode, HttpHeaders headers, String contentType, int contentLen, ByteBuf dataBuf, String dataStr) {
		this.statusCode = statusCode;
		this.headers = headers;
		this.dataBuf = dataBuf;
		this.dataStr = dataStr;
		this.contentType = contentType;
		this.contentLen = contentLen;
		if(this.dataBuf != null){
			isBinary = true;
		}else{
			isBinary = false;
		}
	}
	
	@Override
	public boolean isBinary(){
		return isBinary;
	}
	@Override
	public int getContentLength(){
		return contentLen;
	}
	@Override
	public ByteBuf getContentBuf(){
		return dataBuf;
	}
	@Override
	public String getContentStr(){
		return dataStr;
	}
	@Override
	public String getContentType(){
		return contentType;
	}
	@Override
	public Iterator<Entry<String,String>> getHeaderIterator(){
		if(headers != null){
			return headers.iteratorAsString();
		}
		return null;
	}
	@Override
	public String getHeader(String name){
		if(headers != null){
			return headers.get(name);
		}
		return null;
	}
	@Override
	public int getStatusCode() {
		return statusCode;
	}
	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

}
