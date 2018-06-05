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
import io.netty.handler.codec.http.LastHttpContent;

public final class DFHttpCliReqWrap implements DFHttpCliReq{
	private String uri = "/";
	private HttpMethod method = HttpMethod.GET;
	private DefaultFullHttpRequest reqRaw = null;
	private HttpHeaders headers = new DefaultHttpHeaders();
	private String reqDataStr = null;
	private ByteBuf reqDataBuf = null;
	private boolean useDefHeader = true;
	private String contentType = null;
	private boolean isForm = false;
	
	protected DFHttpCliReqWrap() {
	
	}
	
	@Override
	public DFHttpCliReq useDefaultHeader(boolean use){
		useDefHeader = use;
		return this;
	}
	
	@Override
	public DFHttpCliReq content(Object data){
		if(data != null){
			if(data instanceof String){
				method(DFHttpMethod.POST);
				return _content((String)data);
			}else if(data instanceof IScriptBuffer){
				method(DFHttpMethod.POST);
				return _content((IScriptBuffer)data);
			}else if(data instanceof ByteBuf){
				method(DFHttpMethod.POST);
				return _content((ByteBuf)data);
			}
		}
		return this;
	}
	
	public DFHttpCliReq _content(String data){
		if(reqDataBuf != null){
			return this;
		}
		reqDataStr = data;
		return this;
	}
	public DFHttpCliReq _content(ByteBuf data){
		if(reqDataStr != null){
			return this;
		}
		reqDataBuf = data;
		return this;
	}
	public DFHttpCliReq _content(IScriptBuffer data) {
		if(reqDataStr != null){
			return this;
		}
		reqDataBuf = ((DFJsBuffer)data).getBuf();
		return this;
	}
	
	@Override
	public DFHttpCliReq header(String name, String val){
		headers.add(name, val);
		return this;
	}
	@Override
	public DFHttpCliReq method(String method){
		if(method.equalsIgnoreCase(DFHttpMethod.POST)){
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
			header(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
			header(HttpHeaderNames.USER_AGENT.toString(), "Mozilla/5.0(WindowsNT6.1;rv:2.0.1)Gecko/20100101Firefox/4.0.1");
		}
		//
		boolean isPost = true;
		if(reqDataBuf != null){ //bin data
			reqRaw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, reqDataBuf);
			header(HttpHeaderNames.CONTENT_LENGTH.toString(), reqDataBuf.readableBytes()+"");
			reqRaw.headers().add(headers);
		}else if(reqDataStr != null){ //string data
			byte[] bufTmp = reqDataStr.getBytes(Charset.forName("utf-8"));
			ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.ioBuffer(bufTmp.length);
			buf.writeBytes(bufTmp);
			reqRaw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, buf);
			header(HttpHeaderNames.CONTENT_LENGTH.toString(), buf.readableBytes()+"");
			reqRaw.headers().add(headers);
			if(this.contentType == null){  //default 
				this.contentType = HttpHeaderValues.TEXT_PLAIN.toString();
			}
		}else{ //GET
			isPost = false;
			reqRaw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
		}
		if(isPost){
			if(isForm){
				reqRaw.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString());
			}else if(contentType != null){
				reqRaw.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType.trim());
			}
		}
		
		return this;
	}
	protected DefaultHttpRequest getReqRaw(){
		return reqRaw;
	}

	@Override
	public DFHttpCliReq contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public DFHttpCliReq form(Boolean isForm) {
		this.isForm = isForm;
		return this;
	}

	
	
}
