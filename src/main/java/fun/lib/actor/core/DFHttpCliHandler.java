package fun.lib.actor.core;

import java.net.InetSocketAddress;

import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.http.DFHttpCliRsp;
import fun.lib.actor.api.http.DFHttpContentType;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public final class DFHttpCliHandler extends ChannelInboundHandlerAdapter{
	private final int actorIdDef;
	private final int requestId;
	private final DFTcpDecoder decoder;
	private final DFHttpDispatcher dispatcher;
	private final CbHttpClient userHandler;
	private final DFHttpCliReqWrap request;
	private volatile boolean hasRsp = false;
	private volatile DFTcpChannelWrap session = null;
	private volatile InetSocketAddress addrRemote = null;
	//
	public DFHttpCliHandler(int actorIdDef, int requestId, DFTcpDecoder decoder, 
			DFHttpDispatcher dispatcher, CbHttpClient userHandler, DFHttpCliReqWrap request) {
		this.actorIdDef = actorIdDef;
		this.requestId = requestId;
		this.decoder = decoder;
		this.dispatcher = dispatcher;
		this.userHandler = userHandler;
		this.request = request;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		addrRemote = (InetSocketAddress) ctx.channel().remoteAddress();
		session = new DFTcpChannelWrap(addrRemote.getHostString(), addrRemote.getPort(), 
						ctx.channel(), DFActorDefine.TCP_DECODE_HTTP, null);
		//
		session.write(request);
		//
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		session.onClose();
		if(!hasRsp){ //还未收到响应 断开  通知错误
			final String errMsg = "disconnect with server";
			final DFActorEvent event = new DFActorEvent(DFActorErrorCode.FAILURE, errMsg)
					.setExtString1(addrRemote.getHostString()).setExtInt1(addrRemote.getPort());
			DFActorManager.get().send(0, actorIdDef, requestId, DFActorDefine.SUBJECT_NET, 
					DFActorDefine.NET_TCP_CONNECT_RESULT, event, true, null, userHandler, false);
		}
		session = null;
		//
		super.channelInactive(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		hasRsp = true;
		try{
			if(msg instanceof FullHttpResponse){
				int actorId = 0;
				FullHttpResponse rsp = (FullHttpResponse) msg;
				HttpHeaders headers = rsp.headers();
				long contentLen = HttpUtil.getContentLength(rsp);
				String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
				//
				DFHttpCliRsp dfRsp = null;
				ByteBuf buf = rsp.content();
				//parse msg
				boolean isString = contentIsString(contentType);
				if(isString){  //String
					String str = null;
					if(buf != null){
						str = (String) buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
					}
					dfRsp = new DFHttpCliRspWrap(rsp.status().code(), headers,
							contentType, (int) contentLen, 
							null, str);
				}else{  //binary
					dfRsp = new DFHttpCliRspWrap(rsp.status().code(), headers,
							contentType, (int) contentLen, 
							buf, null);
				}
				//
				Object msgWrap = null;
				//decode
				if(decoder != null){
					Object tmp = decoder.onDecode(dfRsp);
					if(tmp != null){
            			msgWrap = tmp;
            		}
				}
				if(msgWrap == null){  //没有解码
            		msgWrap = dfRsp;
            		if(!isString && buf != null){
            			buf.retain();
            		}
            	}
				//检测分发
				if(dispatcher != null){
					actorId = dispatcher.onQueryMsgActorId(addrRemote.getPort(), addrRemote, msgWrap);
				}
				//
				if(actorId == 0){
	            	actorId = actorIdDef;
	            }
				//
				if(actorId != 0 && msgWrap != null){ //可以后续处理
					DFActorManager.get().send(requestId, actorId, 2, 
							DFActorDefine.SUBJECT_NET, 
							DFActorDefine.NET_TCP_MESSAGE,  
							msgWrap, true, session, actorId==actorIdDef?userHandler:null, false);
	            }
			}
		}finally{
			ReferenceCountUtil.release(msg);
			ctx.close();
		}
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
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
