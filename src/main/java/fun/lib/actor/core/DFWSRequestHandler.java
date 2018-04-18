package fun.lib.actor.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

public class DFWSRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> { //1
    
	private final String _wsUri;
    
    public DFWSRequestHandler(final String wsUri) {
    	_wsUri = wsUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		// Handle a bad request
		if (!req.decoderResult().isSuccess()) {
			_sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		// Allow only GET methods
		if (req.method() != HttpMethod.GET) {
			_sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
			return;
		}
		
		// Send the index page
		final String uri = req.uri();
		if (_wsUri.equals(uri)) {
//			String webSocketLocation = _getWebSocketLocation(ctx.pipeline(), req, _wsUri);
//			ByteBuf content = WebSocketServerIndexPage.getContent(webSocketLocation);
//			FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
//			
//			res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
//			HttpHeaders.setContentLength(res, content.readableBytes());
//			
//			_sendHttpResponse(ctx, req, res);
			
			ctx.fireChannelRead(req.setUri(_wsUri).retain());
		} else {
			_sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
		}
		
		
		
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
	
	private void _sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse rsp) {
		// Generate an error page if response getStatus code is not OK (200).
		if (rsp.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(rsp.status().toString(), CharsetUtil.UTF_8);
			rsp.content().writeBytes(buf);
			buf.release();
			 HttpUtil.setContentLength(rsp, rsp.content().readableBytes());
		}
		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(rsp);
		if (!HttpUtil.isKeepAlive(req) || rsp.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private static String _getWebSocketLocation(ChannelPipeline cp, HttpRequest req, String path) {
		String protocol = "ws";
		if (cp.get(SslHandler.class) != null) {
			// SSL in use so use Secure WebSockets
			protocol = "wss";
		}
		return protocol + "://" + req.headers().get(HttpHeaderNames.HOST) + path;
	}
	
	
}



