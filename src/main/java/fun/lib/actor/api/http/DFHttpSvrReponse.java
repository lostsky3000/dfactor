package fun.lib.actor.api.http;

import java.io.UnsupportedEncodingException;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.api.DFTcpChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public final class DFHttpSvrReponse {

	private FullHttpResponse response = null;
	private final DFTcpChannel channel;
	
	public DFHttpSvrReponse(DFTcpChannel channel, int statusCode) {
		this.channel = channel;
		response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode));
	}
	public DFHttpSvrReponse(int statusCode) {
		this(null, statusCode);
	}
	
	public DFHttpSvrReponse(DFTcpChannel channel, String strData){
		this(channel, 200, strData);
		setContentType(DFHttpContentType.TEXT_PLAIN);  //default is text
	}
	public DFHttpSvrReponse(String strData){
		this(null, 200, strData);
		setContentType(DFHttpContentType.TEXT_PLAIN);  //default is text
	}
	public DFHttpSvrReponse(DFTcpChannel channel, JSONObject json){
		this(channel, 200, json);
	}
	public DFHttpSvrReponse(JSONObject json){
		this(null, 200, json);
	}
	public DFHttpSvrReponse(DFTcpChannel channel, int statusCode, JSONObject json){
		this(channel, statusCode, json.toJSONString());
		setContentType(DFHttpContentType.JSON);
	}
	public DFHttpSvrReponse(int statusCode, JSONObject json){
		this(null, statusCode, json.toJSONString());
		setContentType(DFHttpContentType.JSON);
	}
	public DFHttpSvrReponse(DFTcpChannel channel, int statusCode, String strData){
		this.channel = channel;
		try {
			byte[] arrBuf = strData.getBytes("utf-8");
			ByteBuf buf = Unpooled.wrappedBuffer(arrBuf);
			response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buf);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	//
	public DFHttpSvrReponse putHeader(String key, String val){
		response.headers().set(key, val);
		return this;
	}
	
	public DFHttpSvrReponse setContentType(String contentType){
		putHeader(DFHttpHeader.CONTENT_TYPE, contentType);
		return this;
	}
	public DFHttpSvrReponse setUserAgent(String userAgent){
		putHeader(DFHttpHeader.USER_AGENT, userAgent);
		return this;
	}
	
	/**
	 * send response
	 */
	public void send(){
		if(channel != null){
			channel.write(this);
		}
	}
	
	
	public FullHttpResponse getRawResponse(){
		return response;
	}
	
}
