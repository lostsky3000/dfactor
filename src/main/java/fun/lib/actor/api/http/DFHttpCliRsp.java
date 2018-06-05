package fun.lib.actor.api.http;

import java.util.Iterator;
import java.util.Map.Entry;

import fun.lib.actor.core.IScriptBuffer;
import io.netty.buffer.ByteBuf;

public interface DFHttpCliRsp {
	
	public boolean isBinary();
	public boolean isIsStr();
	public int getContentLength();
	
	public Object getContent();
	public ByteBuf getContentBuf();
	public String getContentStr();
	
	public String getContentType();
	public Iterator<Entry<String,String>> getHeaderIterator();
	public String header(String name);
	public int getStatus();
	public void release();
}
