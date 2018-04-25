package fun.lib.actor.api.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;

public interface DFHttpCliRequest {

	public DFHttpCliRequest end();
	
	public DFHttpCliRequest uri(String uri);
	
	public DFHttpCliRequest method(String method);
	
	public DFHttpCliRequest addHeader(String name, String val);
	
	public DFHttpCliRequest setReqData(String data);
	
	public DFHttpCliRequest setReqData(ByteBuf data);
	
	public DFHttpCliRequest useDefaultHeader(boolean use);
}
