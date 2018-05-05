package fun.lib.actor.api.http;

import java.util.Iterator;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;

public interface DFHttpCliRsp {
	
	public boolean isBinary();
	
	public int getContentLength();
	public ByteBuf getContentBuf();
	public String getContentStr();
	
	public String getContentType();
	public Iterator<Entry<String,String>> getHeaderIterator();
	public String getHeader(String name);
	public int getStatusCode();
	public void release();
}
