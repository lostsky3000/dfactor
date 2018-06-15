package fun.lib.actor.api.http;

import io.netty.handler.codec.http.HttpHeaderNames;

public final class DFHttpHeader {

	public static final String CONTENT_TYPE = HttpHeaderNames.CONTENT_TYPE.toString(); //"Content-Type";
	public static final String CONTENT_LENGTH = HttpHeaderNames.CONTENT_LENGTH.toString() ; //"Content-Length";
	public static final String CONNECTION = HttpHeaderNames.CONNECTION.toString(); //"Connection";
	public static final String USER_AGENT = HttpHeaderNames.USER_AGENT.toString(); //"User-Agent";
	public static final String KEEP_ALIVE = "keep-alive";
	
}
