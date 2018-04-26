package fun.lib.actor.core;

import java.io.UnsupportedEncodingException;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.http.DFHttpContentType;
import fun.lib.actor.api.http.DFHttpHeader;
import fun.lib.actor.api.http.DFHttpSvrRsp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public final class DFHttpSvrRspWrap implements DFHttpSvrRsp{

	private FullHttpResponse response = null;
	private final DFTcpChannel channel;
	
	protected DFHttpSvrRspWrap(DFTcpChannel channel, int statusCode) {
		this.channel = channel;
		response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode));
	}
	protected DFHttpSvrRspWrap(int statusCode) {
		this(null, statusCode);
	}
	
	protected DFHttpSvrRspWrap(DFTcpChannel channel, String strData){
		this(channel, 200, strData);
		setContentType(DFHttpContentType.TEXT_PLAIN);  //default is text
	}
	protected DFHttpSvrRspWrap(String strData){
		this(null, 200, strData);
		setContentType(DFHttpContentType.TEXT_PLAIN);  //default is text
	}
	protected DFHttpSvrRspWrap(DFTcpChannel channel, JSONObject json){
		this(channel, 200, json);
	}
	protected DFHttpSvrRspWrap(JSONObject json){
		this(null, 200, json);
	}
	protected DFHttpSvrRspWrap(DFTcpChannel channel, int statusCode, JSONObject json){
		this(channel, statusCode, json.toJSONString());
		setContentType(DFHttpContentType.JSON);
	}
	protected DFHttpSvrRspWrap(int statusCode, JSONObject json){
		this(null, statusCode, json.toJSONString());
		setContentType(DFHttpContentType.JSON);
	}
	protected DFHttpSvrRspWrap(DFTcpChannel channel, int statusCode, String strData){
		this.channel = channel;
		try {
			byte[] arrBuf = strData.getBytes("utf-8");
			ByteBuf buf = Unpooled.wrappedBuffer(arrBuf);
			response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buf);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	protected DFHttpSvrRspWrap(DFTcpChannel channel, ByteBuf buf){
		this(channel, 200, buf);
	}
	protected DFHttpSvrRspWrap(DFTcpChannel channel, int statusCode, ByteBuf buf){
		this.channel = channel;
		response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buf);
		setContentType(DFHttpContentType.OCTET_STREAM);
	}
	
	//
	@Override
	public DFHttpSvrRsp putHeader(String key, String val){
		response.headers().set(key, val);
		return this;
	}
	@Override
	public DFHttpSvrRsp setContentType(String contentType){
		putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
		return this;
	}
	@Override
	public DFHttpSvrRsp setUserAgent(String userAgent){
		putHeader(HttpHeaderNames.USER_AGENT.toString(), userAgent);
		return this;
	}
	@Override
	public void send(){
		if(channel != null){
			channel.write(this);
		}
	}
	
	//
	protected FullHttpResponse getRawResponse(){
		return response;
	}
	
}
