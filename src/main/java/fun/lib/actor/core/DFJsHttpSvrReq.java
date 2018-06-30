package fun.lib.actor.core;

import java.util.Iterator;
import java.util.Map.Entry;

import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.api.http.DFHttpSvrRsp;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;

public final class DFJsHttpSvrReq implements IScriptHttpSvrReq{

	private final DFHttpSvrReq req;
	private DFHttpSvrRsp rsp = null;
	private Object dataWrap = null;
	
	public DFJsHttpSvrReq(DFHttpSvrReq req) {
		this.req = req;
	}
	@Override
	public String getMethod() {
		return req.getMethod().name();
	}
	@Override
	public String getUri() {
		return req.getUri();
	}
	@Override
	public String getContentType() {
		return req.getContentType();
	}
	@Override
	public int getContentLength() {
		return req.getContentLength();
	}
	@Override
	public boolean isIsStr() {
		return req.contentIsStr();
	}

	@Override
	public Object getContent() {
		if(dataWrap != null){
			return dataWrap;
		}
		dataWrap = req.getApplicationData();
		if(dataWrap == null){  //no app data
			return null;
		}
		if(dataWrap instanceof ByteBuf){
			dataWrap = DFJsBuffer.newBuffer((ByteBuf)dataWrap);
		}
		return dataWrap;
	}
	@Override
	public String header(String key) {
		return req.getHeaderValue(key);
	}
	@Override
	public String getUserAgent() {
		return req.getHeaderValue(HttpHeaderNames.USER_AGENT.toString());
	}
	
	
	@Override
	public DFHttpSvrRsp response(Object data) {
		if(rsp == null){
			if(data instanceof Integer){ //status code
				rsp = req.response((Integer)data);
			}else if(data instanceof String){
				rsp = req.response((String)data);
			}else if(data instanceof IScriptBuffer){
				ByteBuf bufOut = ((DFJsBuffer)data).getBuf();
				rsp = req.response(bufOut);
			}
		}
		return rsp;
	}
	@Override
	public Iterator<Entry<String, Object>> getFieldIterator() {
		return req.getQueryDataIterator();
	}


	

	


	
}
