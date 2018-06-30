package fun.lib.actor.core;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import fun.lib.actor.api.http.DFHttpCliRsp;
import fun.lib.actor.api.http.DFHttpContentType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public final class DFHttpSyncHandler extends ChannelInboundHandlerAdapter{

	private volatile ChannelHandlerContext _ctx = null;
	private final CountDownLatch _cdActive;
	protected DFHttpSyncHandler(CountDownLatch cdActive) {
		_cdActive = cdActive;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		_ctx = ctx;
		super.channelActive(ctx);
		_cdActive.countDown();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		_ctx = null;
		super.channelInactive(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try{
			if(msg instanceof FullHttpResponse){
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
				_recvData = dfRsp;
				if(!isString && buf != null){
        			buf.retain();
        		}
			}
		}finally{
			if(_recvPromise != null){
				_recvPromise.setSuccess();
			}
			ReferenceCountUtil.release(msg);
		}
	}
	
	private volatile Object _recvData = null;
	protected Object getRecvData(){
		return _recvData;
	}
	protected void release(){
		if(_recvData != null){
			DFHttpCliRspWrap wrap = (DFHttpCliRspWrap) _recvData;
			wrap.release();
			_recvData = null;
		}
	}
	private volatile ChannelPromise _recvPromise = null;
	protected ChannelPromise sendMsg(Object msg){
		if(_ctx != null){
			_recvPromise = _ctx.writeAndFlush(msg).channel().newPromise();
		}
		return _recvPromise;
	}
	protected ChannelHandlerContext getContext(){
		return _ctx;
	}
	private boolean contentIsString(String contentType){
		if(contentType != null && 
				(
				contentType.equals(DFHttpContentType.TEXT_HTML)
				||contentType.equals(DFHttpContentType.JSON)
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
