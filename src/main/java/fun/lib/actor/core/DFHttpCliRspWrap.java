package fun.lib.actor.core;

import java.util.Iterator;
import java.util.Map.Entry;

import fun.lib.actor.api.http.DFHttpCliRsp;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

public final class DFHttpCliRspWrap implements DFHttpCliRsp{
	
	private final int statusCode;
	private final HttpHeaders headers;
	private final ByteBuf dataBuf;
	private final IScriptBuffer dataBufScript;
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
			dataBufScript = DFJsBuffer.newBuffer(this.dataBuf);
		}else{
			isBinary = false;
			dataBufScript = null;
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
	public Object getContent() {
		if(!isBinary){ //string
			return dataStr;
		}else{ //buf
			return dataBufScript;
		}
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
	public String header(String name){
		if(headers != null){
			return headers.get(name);
		}
		return null;
	}
	@Override
	public int getStatus() {
		return statusCode;
	}
	@Override
	public void release() {
		if(dataBuf != null){
			dataBuf.release(dataBuf.refCnt());
		}
	}

	@Override
	public boolean isIsStr() {
		return !isBinary;
	}

}
