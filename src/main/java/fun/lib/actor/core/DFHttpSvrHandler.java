package fun.lib.actor.core;


import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.http.DFHttpContentType;
import fun.lib.actor.api.http.DFHttpHeader;
import fun.lib.actor.api.http.DFHttpMethod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ReferenceCountUtil;

public final class DFHttpSvrHandler extends ChannelInboundHandlerAdapter{
	private final int _actorIdDef;
	private final int _requestId;
	private final DFTcpDecoder _decoder;
	private final DFHttpDispatcher _dispatcher;
	private final CbHttpServer _svrHandler;
	//
	private volatile DFTcpChannelWrap _session = null;
	private volatile InetSocketAddress _addrRemote = null;
	
	protected DFHttpSvrHandler(int actorIdDef, int requestId, DFTcpDecoder decoder, DFHttpDispatcher dispatcher,
			CbHttpServer svrHandler) {
		_actorIdDef = actorIdDef;
		_requestId = requestId;
		_decoder = decoder;
		_dispatcher = dispatcher;
		_svrHandler = svrHandler;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		_addrRemote = (InetSocketAddress) ctx.channel().remoteAddress();
		_session = new DFTcpChannelWrap(_addrRemote.getHostString(), _addrRemote.getPort(), 
				ctx.channel(), DFActorDefine.TCP_DECODE_HTTP, null);
		_session.setOpenTime(System.currentTimeMillis());
		//
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		_session.onClose();
		_session = null;
		super.channelInactive(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try{
			if (msg instanceof FullHttpRequest) {
				DFHttpSvrReqWrap dfReq = null;
				int actorId = 0;
				Object msgWrap = null;
				//
				FullHttpRequest req = (FullHttpRequest) msg;
	            final HttpMethod method = req.method();
	            final String uri = req.uri();
	            final boolean keepAlive = HttpUtil.isKeepAlive(req);
	            //
	            if(method.equals(HttpMethod.GET)){
	            	HttpHeaders headers = req.headers();
	            	//
	            	dfReq = new DFHttpSvrReqWrap(_session, uri, method, keepAlive, null, 0, null);
	            	//headers
	            	dfReq.setHeaders(headers);
	            	//values
	            	QueryStringDecoder decoder = new QueryStringDecoder(uri, Charsets.toCharset(CharEncoding.UTF_8));
	            	Map<String,List<String>> mapAttr = decoder.parameters();
	            	for(Map.Entry<String, List<String>> attr : mapAttr.entrySet()){
	            		for(String val : attr.getValue()){
	            			dfReq.addQueryData(attr.getKey(), val);
	            		}
	            	}
	            	//
	            	if(_decoder != null){
	            		Object tmp = _decoder.onDecode(dfReq);
	            		if(tmp != null){
	            			msgWrap = tmp;
	            		}
	            	}
	            	if(msgWrap == null){  //没有解码
	            		msgWrap = dfReq;
	            	}
	            	//
	            	if(_dispatcher != null){  //通知分发
	            		actorId = _dispatcher.onQueryMsgActorId(_requestId, _addrRemote, msgWrap);
	            	}
	            }else if(method.equals(HttpMethod.POST)){
	            	HttpHeaders headers = req.headers();
	            	final String contentType = headers.get(DFHttpHeader.CONTENT_TYPE);    
	            	final int contentLen = (int) HttpUtil.getContentLength(req);
	            	//data
	            	if(contentType != null && 
	            			contentType.equalsIgnoreCase(DFHttpContentType.FORM)){ //表单请求
	            		dfReq = new DFHttpSvrReqWrap(_session, uri, method, keepAlive, 
	            						contentType,//==null?DFHttpContentType.UNKNOWN:contentType, 
	            						contentLen, null);
	            		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
	                	List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
	                    for (InterfaceHttpData parm : parmList) {
	                        Attribute data = (Attribute) parm;
	                        dfReq.addQueryData(data.getName(), data.getValue());
	                    }
	            	}else{ //application 请求
	            		Object appData = null;
	            		ByteBuf buf = req.content();
	            		if(contentIsString(contentType)){ //String
	            			appData = buf.readCharSequence(buf.readableBytes(), Charset.forName("utf-8"));
	            		}else{  //raw bin
	            			appData = buf;
	            			if(buf != null){
		            			buf.retain();
		            		}
	            		}
	            		dfReq = new DFHttpSvrReqWrap(_session, uri, method, keepAlive, 
        								contentType, //==null?DFHttpContentType.UNKNOWN:contentType, 
        								contentLen, appData);
	            	}
	            	//headers
	            	dfReq.setHeaders(headers);
	            	//
	            	if(_decoder != null){
	            		Object tmp = _decoder.onDecode(dfReq);
	            		if(tmp != null){
	            			msgWrap = tmp;
	            		}
	            	}
	            	if(msgWrap == null){  //没有解码
	            		msgWrap = dfReq;
	            	}
	            	//
	            	if(_dispatcher != null){  //通知分发
	            		actorId = _dispatcher.onQueryMsgActorId(_requestId, _addrRemote, msgWrap);
	            	}
//	            	if(HttpPostRequestDecoder.isMultipart(req)){
//	            	}else{
//	            	}
	            }else{  //unsurpport method
	            	ctx.close();
	            }
	            if(actorId == 0){
	            	actorId = _actorIdDef;
	            }
	            //
	            if(actorId != 0 && msgWrap != null){ //可以后续处理
	            	if(DFActorManager.get().send(_requestId, actorId, 1, 
							DFActorDefine.SUBJECT_NET, 
							DFActorDefine.NET_TCP_MESSAGE,  //DFActorDefine.NET_TCP_MESSAGE_CUSTOM, 
							msgWrap, true, _session, actorId==_actorIdDef?_svrHandler:null, false) != 0){ //send to queue failed
						//处理失败  返回503  HttpResponseStatus.SERVICE_UNAVAILABLE
	            		_session.write(new DFHttpSvrRspWrap(503));
					}
	            }else{ //无法处理,返回404 HttpResponseStatus.NOT_FOUND
	            	_session.write(new DFHttpSvrRspWrap(404));
	            }
	        }else{ //unsurpport request type
	        	ctx.close();
	        }	
		}finally{
			ReferenceCountUtil.release(msg);
		}
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	
	private boolean contentIsString(String contentType){
		if(contentType != null && 
				(contentType.equals(DFHttpContentType.JSON)
				|| contentType.equals(DFHttpContentType.XML)
				|| contentType.equals(DFHttpContentType.TEXT_PLAIN)
				)){
			return true;
		}
		return false;
	}
	//
//	private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
//	private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
//    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
//    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
//    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
//    private static final String URI_FAVICON = "/favicon.ico";
//	private void _ttt(ChannelHandlerContext ctx){
//		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, 
//        		HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT));
//        response.headers().set(CONTENT_TYPE, "text/plain");
//        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
//
//        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
////        if (!keepAlive) {
////            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
////        } else {
////            response.headers().set(CONNECTION, KEEP_ALIVE);
////            ctx.writeAndFlush(response);
////        }
//	}
}
