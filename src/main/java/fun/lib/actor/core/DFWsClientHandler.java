package fun.lib.actor.core;

import java.net.InetSocketAddress;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.DFTcpEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public final class DFWsClientHandler extends SimpleChannelInboundHandler<Object>{
	
	private final WebSocketClientHandshaker handshaker;  
    private ChannelPromise handshakeFuture;  
    private final int _actorIdDef;
	private final int _requestId;
	private final int _decodeType;
	private final DFActorTcpDispatcher _dispatcher;
	private final DFTcpDecoder _decoder;
	private final DFTcpEncoder _encoder;
	private volatile DFTcpChannelWrap _session = null;
	private volatile int _sessionId = 0;
	private volatile InetSocketAddress _addrRemote = null;
	private final DFActorManager _mgrActor;
    
    public DFWsClientHandler(WebSocketClientHandshaker handshaker,int actorIdDef, int requestId, int decodeType, DFActorTcpDispatcher dispatcher, 
			DFTcpDecoder decoder, DFTcpEncoder encoder) {  
        this.handshaker = handshaker; 
        _actorIdDef = actorIdDef;
		_requestId = requestId;
		_decodeType = decodeType;
		_dispatcher = dispatcher;
		_decoder = decoder;
		_encoder = encoder;
		_mgrActor = DFActorManager.get();
    }  
  
    public ChannelFuture handshakeFuture() {  
        return handshakeFuture;  
    }  
  
    @Override  
    public void handlerAdded(ChannelHandlerContext ctx) {  
        handshakeFuture = ctx.newPromise();  
    }  
  
    @Override  
    public void channelActive(ChannelHandlerContext ctx) {  
        handshaker.handshake(ctx.channel()); 
        
    }  
  
    @Override  
    public void channelInactive(ChannelHandlerContext ctx) {  
    	_session.onClose();
		int actorId = 0;
		if(_dispatcher != null){
			actorId = _dispatcher.onConnInactiveUnsafe(_requestId, _sessionId, _addrRemote);
		}else{
			actorId = _session.getStatusActor();
		}
		if(actorId != 0){
			//notify actor
			_mgrActor.send(0, actorId, _requestId, 
					DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_CLOSE, _session, true);
		}
		_session = null;
//		try {
//			super.channelInactive(ctx);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
    }  
  
    @Override  
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {  
        Channel ch = ctx.channel();  
        if (!handshaker.isHandshakeComplete()) {  
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);  
//            System.out.println("WebSocket Client connected!");  
            handshakeFuture.setSuccess();  
            //
            //
            _addrRemote = (InetSocketAddress) ctx.channel().remoteAddress();
    		_session = new DFTcpChannelWrap(_addrRemote.getHostString(), _addrRemote.getPort(), 
    				ctx.channel(), _decodeType, _encoder);
    		_session.setOpenTime(System.currentTimeMillis());
    		_sessionId = _session.getChannelId();
    		//
    		int actorId = 0;
    		if(_dispatcher != null){
    			actorId = _dispatcher.onConnActiveUnsafe(_requestId, _sessionId, _addrRemote);
    		}else{  //没有notify指定
    			actorId = _actorIdDef;
    		}
    		if(actorId != 0){ //actor有效
    			_session.setStatusActor(actorId);
    			_session.setMessageActor(actorId);
    			//notify actor
    			_mgrActor.send(0, actorId, _requestId, 
    					DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_OPEN, _session, true);
    		}
            return;  
        }
        if (msg instanceof FullHttpResponse) {  
            FullHttpResponse response = (FullHttpResponse) msg;  
            throw new IllegalStateException(  
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +  
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');  
        }  
  
        WebSocketFrame msgRaw = (WebSocketFrame) msg;  
//        if (frame instanceof TextWebSocketFrame) {  
//            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;  
//            System.out.println("WebSocket Client received message: " + textFrame.text());  
//        } else if (frame instanceof PongWebSocketFrame) {  
//            System.out.println("WebSocket Client received pong");  
//        } else if (frame instanceof CloseWebSocketFrame) {  
//            System.out.println("WebSocket Client received closing");  
//            ch.close();  
//        }  
        
        try{
			if(msgRaw instanceof PongWebSocketFrame){
				
			}else if(msgRaw instanceof CloseWebSocketFrame){
				ch.close();
			}
			else{
				boolean msgIsBin = false;
				Object msgTmp = null;
				int msgType = 0;
				if(msgRaw instanceof BinaryWebSocketFrame){ 	//ByteBuf
					msgTmp = msgRaw.content().retain();
					msgType = DFActorDefine.NET_TCP_MESSAGE;
					msgIsBin = true;
				}else if(msgRaw instanceof TextWebSocketFrame){ //String
					msgTmp = ((TextWebSocketFrame) msgRaw).text();
					msgType = DFActorDefine.NET_TCP_MESSAGE_TEXT;
				}else{  //unknown frame
					return ;
				}
				//
				msg = null;
				if(_decoder != null){  //有自定义解码器
					msg = _decoder.onDecode(msgTmp);
					if(msg != null && msg != msgTmp){ //已经过解码
						msgType = DFActorDefine.NET_TCP_MESSAGE_CUSTOM;
						msgIsBin = false;
					}
				}else{
					msg = msgTmp;
				}
				int actorId = 0;
				if(_dispatcher == null){
					actorId = _session.getMsgActor();
				}else{
					actorId = _dispatcher.onQueryMsgActorId(_requestId, _sessionId, _addrRemote, msg);
				}
				if(actorId != 0){ //actor有效
					if(_mgrActor.send(_requestId, actorId, _sessionId, 
							DFActorDefine.SUBJECT_NET, 
							DFActorDefine.NET_TCP_MESSAGE, //msgType, 
							msg, true, _session, null, false) != 0){ //send to queue failed
						if(msgIsBin){ //release
							ReferenceCountUtil.release(msg);
						}
					}
				}
			} 
		}finally{
			
		}
    }  
  
    @Override  
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {  
        cause.printStackTrace();  
        if (!handshakeFuture.isDone()) {  
            handshakeFuture.setFailure(cause);  
        }  
        ctx.close();  
    }

}
