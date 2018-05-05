package fun.lib.actor.core;

import java.nio.charset.Charset;

import fun.lib.actor.api.http.DFHttpCliReq;
import fun.lib.actor.api.http.DFHttpHeader;
import fun.lib.actor.api.http.DFHttpMethod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public final class DFHttpCliReqWrap implements DFHttpCliReq{
	private String uri = "/";
	private HttpMethod method = HttpMethod.GET;
	private DefaultFullHttpRequest reqRaw = null;
	private HttpHeaders headers = new DefaultHttpHeaders();
	private String reqDataStr = null;
	private ByteBuf reqDataBuf = null;
	private boolean useDefHeader = true;
	
	protected DFHttpCliReqWrap() {
	
	}
	
	@Override
	public DFHttpCliReq useDefaultHeader(boolean use){
		useDefHeader = use;
		return this;
	}
	@Override
	public DFHttpCliReq setReqData(String data){
		if(reqDataBuf != null){
			return this;
		}
		reqDataStr = data;
		return this;
	}
	@Override
	public DFHttpCliReq setReqData(ByteBuf data){
		if(reqDataStr != null){
			return this;
		}
		reqDataBuf = data;
		return this;
	}
	
	@Override
	public DFHttpCliReq addHeader(String name, String val){
		headers.add(name, val);
		return this;
	}
	@Override
	public DFHttpCliReq method(String method){
		if(method.equals(DFHttpMethod.POST)){
			this.method = HttpMethod.POST;
		}else{
			this.method = HttpMethod.GET;
		}
		return this;
	}
	@Override
	public DFHttpCliReq uri(String uri){
		this.uri = uri;
		return this;
	}
	@Override
	public DFHttpCliReq end(){
		if(reqRaw != null){
			return this;
		}
		//
		if(useDefHeader){
			addHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
			addHeader(HttpHeaderNames.USER_AGENT.toString(), "Mozilla/5.0(WindowsNT6.1;rv:2.0.1)Gecko/20100101Firefox/4.0.1");
		}
		//
		if(reqDataBuf != null){ //bin data
			reqRaw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, reqDataBuf, headers, null);
		}else if(reqDataStr != null){ //string data
			byte[] bufTmp = reqDataStr.getBytes(Charset.forName("utf-8"));
			ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.ioBuffer(bufTmp.length);
			buf.writeBytes(bufTmp);
			reqRaw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, buf, headers, null);
		}else{
			reqRaw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
		}
		return this;
	}
	protected DefaultHttpRequest getReqRaw(){
		return reqRaw;
	}
	
}
