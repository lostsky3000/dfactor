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
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ReferenceCountUtil;

public final class DFHttpSvrReqWrap implements DFHttpSvrReq{

	private final HttpMethod method;
	private final String contentType;
	private final int contentLength;
	private String uri;
	private final boolean keepAlive;
	private HttpHeaders headers = null;
	private Map<String,Object> mapQueryData = null;
	private Object appData = null;
	
	private volatile DFHttpSvrRspWrap response = null;
	private final DFTcpChannel channel;
	private boolean _shouldNoRsp = false;
	private final boolean isMultipart;
	private boolean _isForward = false;
	
	protected DFHttpSvrReqWrap(DFTcpChannel channel, String uri, HttpMethod method, boolean keepAlive, 
				String contentType, int contentLength, Object appData, boolean isMultipart) {
		this.channel = channel;
		this.uri = uri;
		this.method = method;
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.keepAlive = keepAlive;
		this.appData = appData;
		this.isMultipart = isMultipart;
	}
	@Override
	public Iterator<Entry<String,Object>> getQueryDataIterator(){
		if(mapQueryData != null){
			return mapQueryData.entrySet().iterator();
		}
		return null;
	}
	@Override
	public Object getQueryData(String name){
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
	public HttpMethod getMethod() {
		return method;
	}
	@Override
	public String getContentType() {
		return contentType;
	}
	@Override
	public int getContentLength(){
		return contentLength;
	}
	@Override
	public boolean contentIsStr() {
		if(appData != null && appData instanceof String){
			return true;
		}
		return false;
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
	protected void addQueryDatas(Map<String,Object> map){
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
	
	protected DFHttpSvrRsp getResponse(){
		return response;
	}
	
	protected boolean shouldNoRsp(){
		return _shouldNoRsp;
	}
	protected void setShouldNoRsp(boolean b){
		_shouldNoRsp = b;
	}
	@Override
	public boolean isMultipart() {
		return isMultipart;
	}
	
	protected DFTcpChannel getChannel(){
		return channel;
	}
	protected boolean isForward(){
		return _isForward;
	}
	protected void setForward(boolean b){
		_isForward = b;
	}
	protected void setUri(String uri){
		this.uri = uri;
	}
}
