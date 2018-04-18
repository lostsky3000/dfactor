package fun.lib.actor.core;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;

public final class DFHttpHandler extends ChannelInboundHandlerAdapter{
	

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelActive: "+ctx.channel().remoteAddress());
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelInactive: "+ctx.channel().remoteAddress());
		super.channelInactive(ctx);
	}
	
	private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
	private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
    
    private static final String URI_FAVICON = "/favicon.ico";
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            final HttpMethod method = req.method();
            final String uri = req.uri();
            final boolean keepAlive = HttpUtil.isKeepAlive(req);
            if(method.equals(HttpMethod.GET)){
            	if(uri.equalsIgnoreCase(URI_FAVICON)){
            		return ;
            	}
            	System.out.println("channelRead: get "+ctx.channel().remoteAddress());
            	QueryStringDecoder decoder = new QueryStringDecoder(uri, Charsets.toCharset(CharEncoding.UTF_8));
            	Map<String,List<String>> mapAttr = decoder.parameters();
            	for(Map.Entry<String, List<String>> attr : mapAttr.entrySet()){
            		for(String val : attr.getValue()){
//            			System.out.println("attr: key="+attr.getKey()+", value="+val);
            		}
            	}
            	HttpHeaders headers = req.headers();
            	Iterator<Entry<String,String>> it = headers.iteratorAsString();
            	while(it.hasNext()){
            		final Entry<String,String> en = it.next();
//            		System.out.println("header: key="+en.getKey()+", value="+en.getValue());
            	}
            	
            	FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, 
                		HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT));
                response.headers().set(CONTENT_TYPE, "text/plain");
                response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//                if (!keepAlive) {
//                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//                } else {
//                    response.headers().set(CONNECTION, KEEP_ALIVE);
//                    ctx.writeAndFlush(response);
//                }
            	
            }else if(method.equals(HttpMethod.POST)){
            	
            }else{
            	
            }
            
        }else{
        	System.out.println("channelRead: unknown msg "+ctx.channel().remoteAddress());
        }
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
