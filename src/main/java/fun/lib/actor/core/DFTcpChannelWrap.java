package fun.lib.actor.core;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFTcpEncoder;
import fun.lib.actor.api.http.DFHttpCliReq;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;

public final class DFTcpChannelWrap implements DFTcpChannel{

	private static int s_sessionIdCount = 1;
	//
	private final String _remoteHost;
	private final int _remotePort;
	private final Channel _channel;
	private final int _tcpDecodeType;
	private volatile boolean _isClosed = false;
	private volatile int _statusActorId = 0;
	private volatile int _msgActorId = 0;
	private volatile int _sessionId = 0;
	private long _openTime = 0;
	private final DFTcpEncoder _encoder;
	//
	protected DFTcpChannelWrap(String remoteHost, int remotePort, final Channel channel, 
			final int decodeType, DFTcpEncoder encoder){
		this._remoteHost = remoteHost;
		this._remotePort = remotePort;
		this._channel = channel;
		this._tcpDecodeType = decodeType;
		this._encoder = encoder;
		//
		synchronized (DFTcpChannelWrap.class) {
			this._sessionId = s_sessionIdCount;
			if(++s_sessionIdCount >= Integer.MAX_VALUE){
				s_sessionIdCount = 1;
			}
		}
		
	}
	
	protected void onClose(){
		_isClosed = true;
	}
	
	protected int getStatusActor(){
		return _statusActorId;
	}
	protected int getMsgActor(){
		return _msgActorId;
	}

	@Override
	public String getRemoteHost() {
		return _remoteHost;
	}
	@Override
	public int getRemotePort() {
		return _remotePort;
	}

	@Override
	public int write(Object msg) {
		return _doWrite(msg, true);
	}
	
	public int writeNoFlush(Object msg){
		return _doWrite(msg, false);
	}
	
	private int _doWrite(Object msg, boolean flushNow){
		if(_isClosed){
			return 1;
		}
		if(_tcpDecodeType == DFActorDefine.TCP_DECODE_WEBSOCKET){ //web socket frame
			if(_encoder != null){ //有编码器
				msg = _encoder.onEncode(msg);
			}
			if(msg instanceof String){
				if(flushNow){
					_channel.writeAndFlush(new TextWebSocketFrame((String) msg));
				}else{
					_channel.write(new TextWebSocketFrame((String) msg));
				}
			}else if(msg instanceof ByteBuf){
				if(flushNow){
					_channel.writeAndFlush(new BinaryWebSocketFrame((ByteBuf) msg));
				}else{
					_channel.write(new BinaryWebSocketFrame((ByteBuf) msg));
				}
			}else if(msg instanceof JSONObject){
				final String str = JSONObject.toJSONString(msg);
				if(flushNow){
					_channel.writeAndFlush(new TextWebSocketFrame(str));
				}else{
					_channel.write(new TextWebSocketFrame(str));
				}
			}else{  
				if(flushNow){
					_channel.writeAndFlush(msg);
				}else{
					_channel.write(msg);
				}
			}
		}else if(_tcpDecodeType == DFActorDefine.TCP_DECODE_LENGTH
					||_tcpDecodeType == DFActorDefine.TCP_DECODE_RAW 
				){ //底层为二进制buff
			if(_encoder != null){ //有编码器
				msg = _encoder.onEncode(msg);	
			}
			if(msg instanceof JSONObject){
				msg = ((JSONObject)msg).toJSONString();
			}
			if(msg instanceof String){
				byte[] bytes = ((String)msg).getBytes(CharsetUtil.UTF_8);
				int len = bytes.length;
				msg = PooledByteBufAllocator.DEFAULT.ioBuffer(len);
				((ByteBuf)msg).writeBytes(bytes);
				if(_tcpDecodeType == DFActorDefine.TCP_DECODE_LENGTH){
					ByteBuf bufHead = PooledByteBufAllocator.DEFAULT.ioBuffer(2);
					bufHead.writeShort(len);
					ByteBuf bufPayload = (ByteBuf) msg;
					msg = PooledByteBufAllocator.DEFAULT.compositeBuffer();
					((CompositeByteBuf)msg).addComponents(true, bufHead, bufPayload);
				}
			}else if( msg instanceof ByteBuf ){
				if(_tcpDecodeType == DFActorDefine.TCP_DECODE_LENGTH){
					ByteBuf bufHead = PooledByteBufAllocator.DEFAULT.ioBuffer(2);
					int len = ((ByteBuf)msg).readableBytes();
					bufHead.writeShort( len );
					ByteBuf bufPayload = (ByteBuf) msg;
//					_channel.write(bufHead);
					msg = PooledByteBufAllocator.DEFAULT.compositeBuffer();
					((CompositeByteBuf)msg).addComponents(true, bufHead, bufPayload);
				}
			}
			if(flushNow){
				_channel.writeAndFlush(msg);
			}else{
				_channel.write(msg);
			}
		}else if(_tcpDecodeType == DFActorDefine.TCP_DECODE_HTTP){
			if(msg instanceof DFHttpSvrRspWrap){
				DFHttpSvrRspWrap rsp = (DFHttpSvrRspWrap) msg;
				if(flushNow){
					_channel.writeAndFlush(rsp.getRawResponse()).addListener(ChannelFutureListener.CLOSE);
				}else{
					_channel.write(rsp.getRawResponse());//addListener(ChannelFutureListener.CLOSE);
				}
			}else if(msg instanceof DFHttpCliReqWrap){
				DFHttpCliReqWrap req = (DFHttpCliReqWrap) msg;
				if(flushNow){
					_channel.writeAndFlush(req.getReqRaw()).addListener(ChannelFutureListener.CLOSE);
				}else{
					_channel.write(req.getReqRaw());
				}
			}else{
				if(flushNow){
					_channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
				}else{
					_channel.write(msg);
				}
			}
		}
		return 0;
	}
	
	
	@Override
	public boolean isClosed() {
		return _isClosed;
	}

	@Override
	public void setStatusActor(int actorId) {
		_statusActorId = actorId;
	}

	@Override
	public void setMessageActor(int actorId) {
		_msgActorId = actorId;
	}

	@Override
	public int getChannelId() {
		// TODO Auto-generated method stub
		return _sessionId;
	}

	@Override
	public void close() {
		if(!_isClosed){
			_channel.close();
		}
	}

	@Override
	public long getOpenTime() {
		// TODO Auto-generated method stub
		return _openTime;
	}
	protected void setOpenTime(long tm){
		_openTime = tm;
	}
	protected int getTcpDecodeType() {
		return _tcpDecodeType;
	}

	
}
