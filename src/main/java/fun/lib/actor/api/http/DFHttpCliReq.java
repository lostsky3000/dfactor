package fun.lib.actor.api.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;

public interface DFHttpCliReq {

	public DFHttpCliReq end();
	
	public DFHttpCliReq uri(String uri);
	
	public DFHttpCliReq method(String method);
	
	public DFHttpCliReq addHeader(String name, String val);
	
	public DFHttpCliReq setReqData(String data);
	
	public DFHttpCliReq setReqData(ByteBuf data);
	
	public DFHttpCliReq useDefaultHeader(boolean use);
}
