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
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.http.DFHttpContentType;
import fun.lib.actor.api.http.DFHttpHeader;
import fun.lib.actor.api.http.DFHttpMethod;
import fun.lib.actor.api.http.DFHttpReponse;
import fun.lib.actor.api.http.DFHttpRequest;
import fun.lib.actor.api.http.DFHttpServerHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
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

public final class DFHttpHandler extends ChannelInboundHandlerAdapter{
	private final int _actorIdDef;
	private final int _requestId;
	private final DFTcpDecoder _decoder;
	private final DFHttpDispatcher _dispatcher;
	private final DFHttpServerHandler _svrHandler;
	//
	private volatile DFTcpChannelWrapper _session = null;
	private volatile int _sessionId = 0;
	private volatile InetSocketAddress _addrRemote = null;
	
	protected DFHttpHandler(int actorIdDef, int requestId, DFTcpDecoder decoder, DFHttpDispatcher dispatcher,
			DFHttpServerHandler svrHandler) {
		_actorIdDef = actorIdDef;
		_requestId = requestId;
		_decoder = decoder;
		_dispatcher = dispatcher;
		_svrHandler = svrHandler;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		_addrRemote = (InetSocketAddress) ctx.channel().remoteAddress();
		_session = new DFTcpChannelWrapper(_addrRemote.getHostString(), _addrRemote.getPort(), 
				ctx.channel(), DFActorDefine.TCP_DECODE_HTTP, null);
		_session.setOpenTime(System.currentTimeMillis());
		_sessionId = _session.getChannelId();
		//
//		int actorId = 0;
//		if(_dispatcher != null){
//			actorId = _dispatcher.onConnActiveUnsafe(_requestId, _sessionId, _addrRemote);
//		}else{  //没有notify指定
//			actorId = _actorIdDef;
//		}
//		if(actorId != 0){ //actor有效
//			_session.setStatusActor(actorId);
//			_session.setMessageActor(actorId);
//			//notify actor
//			DFActorManager.get().send(0, actorId, _requestId, 
//					DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_OPEN, _session, true);
//		}
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		_session.onClose();
//		int actorId = 0;
//		if(_dispatcher != null){
//			actorId = _dispatcher.onConnInactiveUnsafe(_requestId, _sessionId, _addrRemote);
//		}else{
//			actorId = _session.getStatusActor();
//		}
//		if(actorId != 0){
//			//notify actor
//			DFActorManager.get().send(0, actorId, _requestId, 
//					DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_CLOSE, _session, true);
//		}
		_session = null;
		super.channelInactive(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try{
			if (msg instanceof FullHttpRequest) {
				DFHttpRequest dfReq = null;
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
	            	dfReq = new DFHttpRequest(_session, uri, DFHttpMethod.GET, keepAlive, null, null);
	            	//headers
	            	Iterator<Entry<String,String>> it = headers.iteratorAsString();
	            	while(it.hasNext()){
	            		final Entry<String,String> en = it.next();
	            		dfReq.addHeader(en.getKey(), en.getValue());
	            	}
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
	            	//data
	            	if(contentType.equalsIgnoreCase(DFHttpContentType.FORM)){ //表单请求
	            		dfReq = new DFHttpRequest(_session, uri, DFHttpMethod.POST, keepAlive, 
	            						contentType==null?DFHttpContentType.UNKNOWN:contentType, null);
	            		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
	                	List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
	                    for (InterfaceHttpData parm : parmList) {
	                        Attribute data = (Attribute) parm;
	                        dfReq.addQueryData(data.getName(), data.getValue());
	                    }
	            	}else{ //application 请求
	            		ByteBuf buf = req.content();
	            		final String str = (String) buf.readCharSequence(buf.readableBytes(), Charset.forName("utf-8"));
	            		dfReq = new DFHttpRequest(_session, uri, DFHttpMethod.POST, keepAlive, 
        								contentType==null?DFHttpContentType.UNKNOWN:contentType, str);
	            	}
	            	//headers
	            	Iterator<Entry<String,String>> it = headers.iteratorAsString();
	            	while(it.hasNext()){
	            		final Entry<String,String> en = it.next();
	            		dfReq.addHeader(en.getKey(), en.getValue());
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
	            	if(DFActorManager.get().send(_requestId, actorId, _sessionId, 
							DFActorDefine.SUBJECT_NET, 
							DFActorDefine.NET_TCP_MESSAGE,  //DFActorDefine.NET_TCP_MESSAGE_CUSTOM, 
							msgWrap, true, _session, actorId==_actorIdDef?_svrHandler:null, false) != 0){ //send to queue failed
						//处理失败  返回503  HttpResponseStatus.SERVICE_UNAVAILABLE
	            		_session.write(new DFHttpReponse(503));
					}
	            }else{ //无法处理,返回404 HttpResponseStatus.NOT_FOUND
	            	_session.write(new DFHttpReponse(404));
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
