package fun.lib.actor.api.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.api.DFTcpChannel;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public final class DFHttpSvrRequest {

	private final String method;
	private final String contentType;
	private final String uri;
	private final boolean keepAlive;
	private Map<String,String> mapHeader = null;
	private Map<String,String> mapQueryData = null;
	private Object appData = null;
	
	private volatile DFHttpSvrReponse response = null;
	private final DFTcpChannel channel;
	
	public DFHttpSvrRequest(DFTcpChannel channel, String uri, String method, boolean keepAlive, String contentType, Object appData) {
		this.channel = channel;
		this.uri = uri;
		this.method = method;
		this.contentType = contentType;
		this.keepAlive = keepAlive;
		this.appData = appData;
	}
	
	public Iterator<Entry<String,String>> getQueryDataIterator(){
		if(mapQueryData != null){
			return mapQueryData.entrySet().iterator();
		}
		return null;
	}
	public void addQueryDatas(Map<String,String> map){
		if(mapQueryData == null){
			mapQueryData = new HashMap<>();
		}
		mapQueryData.putAll(map);
	}
	public void addQueryData(String name, String value){
		if(mapQueryData == null){
			mapQueryData = new HashMap<>();
		}
		mapQueryData.put(name, value);
	}
	public String getQueryData(String name){
		if(mapQueryData != null){
			return mapQueryData.get(name);
		}
		return null;
	}
	public boolean isQueryDataExist(String name){
		if(mapQueryData != null){
			return mapQueryData.containsKey(name);
		}
		return false;
	}
	
	public Iterator<Entry<String,String>> getHeaderIterator(){
		if(mapHeader != null){
			return mapHeader.entrySet().iterator();
		}
		return null;
	}
	public void addHeaders(Map<String,String> map){
		if(mapHeader == null){
			mapHeader = new HashMap<>();
		}
		mapHeader.putAll(map);
	}
	public void addHeader(String name, String value){
		if(mapHeader == null){
			mapHeader = new HashMap<>();
		}
		mapHeader.put(name, value);
	}
	public String getHeaderValue(String name){
		if(mapHeader != null){
			return mapHeader.get(name);
		}
		return null;
	}
	public boolean isHeaderExist(String name){
		if(mapHeader != null){
			return mapHeader.containsKey(name);
		}
		return false;
	}
	
	public Object getApplicationData() {
		return appData;
	}
	public String getMethod() {
		return method;
	}

	public String getContentType() {
		return contentType;
	}

	public String getUri() {
		return uri;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}
	
	//response
	public DFHttpSvrReponse response(int statusCode){
		if(response == null){
			response = new DFHttpSvrReponse(channel, statusCode);
		}
		return response;
	}
	public DFHttpSvrReponse response(String rspData){
		if(response == null){
			response = new DFHttpSvrReponse(channel, rspData);
		}
		return response;
	}
	public DFHttpSvrReponse response(int statusCode, String rspData){
		if(response == null){
			response = new DFHttpSvrReponse(channel, statusCode, rspData);
		}
		return response;
	}
	//
	public DFHttpSvrReponse response(JSONObject rspData){
		if(response == null){
			response = new DFHttpSvrReponse(channel, rspData);
		}
		return response;
	}
	public DFHttpSvrReponse response(int statusCode, JSONObject rspData){
		if(response == null){
			response = new DFHttpSvrReponse(channel, statusCode, rspData);
		}
		return response;
	}
	
	
	public void release(){
		if(appData != null && appData instanceof ByteBuf){
			ReferenceCountUtil.release(appData);
			appData = null;
		}
	}
}
