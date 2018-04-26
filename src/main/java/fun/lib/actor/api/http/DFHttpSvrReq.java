package fun.lib.actor.api.http;

import java.util.Iterator;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.core.DFHttpSvrRspWrap;
import io.netty.buffer.ByteBuf;

public interface DFHttpSvrReq {

	public Iterator<Entry<String,String>> getQueryDataIterator();
	public String getQueryData(String name);
	public Iterator<Entry<String,String>> getHeaderIterator();
	public String getHeaderValue(String name);
	public Object getApplicationData();
	public String getMethod();
	public String getContentType();
	public String getUri();
	public boolean isKeepAlive();
	public void release();
	
	
	//response
	public DFHttpSvrRsp response(int statusCode);
	public DFHttpSvrRsp response(String rspData);
	public DFHttpSvrRsp response(int statusCode, String rspData);
	public DFHttpSvrRsp response(JSONObject rspData);
	public DFHttpSvrRsp response(int statusCode, JSONObject rspData);
	public DFHttpSvrRsp response(ByteBuf rspData);
	public DFHttpSvrRsp response(int statusCode, ByteBuf rspData);
	
}
