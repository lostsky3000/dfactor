package fun.lib.actor.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.api.http.DFHttpSvrRsp;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.ReferenceCountUtil;

public final class DFHttpSvrReqWrap implements DFHttpSvrReq{

	private final String method;
	private final String contentType;
	private final String uri;
	private final boolean keepAlive;
	private HttpHeaders headers = null;
	private Map<String,String> mapQueryData = null;
	private Object appData = null;
	
	private volatile DFHttpSvrRspWrap response = null;
	private final DFTcpChannel channel;
	
	protected DFHttpSvrReqWrap(DFTcpChannel channel, String uri, String method, boolean keepAlive, String contentType, Object appData) {
		this.channel = channel;
		this.uri = uri;
		this.method = method;
		this.contentType = contentType;
		this.keepAlive = keepAlive;
		this.appData = appData;
	}
	@Override
	public Iterator<Entry<String,String>> getQueryDataIterator(){
		if(mapQueryData != null){
			return mapQueryData.entrySet().iterator();
		}
		return null;
	}
	@Override
	public String getQueryData(String name){
		if(mapQueryData != null){
			return mapQueryData.get(name);
		}
		return null;
	}
	@Override
	public Iterator<Entry<String,String>> getHeaderIterator(){
		if(headers != null){
			return headers.iteratorAsString();
		}
		return null;
	}
	@Override
	public String getHeaderValue(String name){
		if(headers != null){
			return headers.get(name);
		}
		return null;
	}
	@Override
	public Object getApplicationData() {
		return appData;
	}
	@Override
	public String getMethod() {
		return method;
	}
	@Override
	public String getContentType() {
		return contentType;
	}
	@Override
	public String getUri() {
		return uri;
	}
	@Override
	public boolean isKeepAlive() {
		return keepAlive;
	}
	@Override
	public void release(){
		if(appData != null && appData instanceof ByteBuf){
			ReferenceCountUtil.release(appData);
			appData = null;
		}
	}
	
	//
	protected void setHeaders(HttpHeaders headers){
		this.headers = headers;
	}
	protected void addQueryDatas(Map<String,String> map){
		if(mapQueryData == null){
			mapQueryData = new HashMap<>();
		}
		mapQueryData.putAll(map);
	}
	protected void addQueryData(String name, String value){
		if(mapQueryData == null){
			mapQueryData = new HashMap<>();
		}
		mapQueryData.put(name, value);
	}
	
	//response
	@Override
	public DFHttpSvrRsp response(int statusCode){
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, statusCode);
		}
		return response;
	}
	@Override
	public DFHttpSvrRsp response(String rspData){
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, rspData);
		}
		return response;
	}
	@Override
	public DFHttpSvrRsp response(int statusCode, String rspData){
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, statusCode, rspData);
		}
		return response;
	}
	@Override
	public DFHttpSvrRsp response(JSONObject rspData){
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, rspData);
		}
		return response;
	}
	@Override
	public DFHttpSvrRsp response(int statusCode, JSONObject rspData){
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, statusCode, rspData);
		}
		return response;
	}
	@Override
	public DFHttpSvrRsp response(ByteBuf rspData) {
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, rspData);
		}
		return response;
	}
	@Override
	public DFHttpSvrRsp response(int statusCode, ByteBuf rspData) {
		if(response == null){
			response = new DFHttpSvrRspWrap(channel, statusCode, rspData);
		}
		return response;
	}
	
}
