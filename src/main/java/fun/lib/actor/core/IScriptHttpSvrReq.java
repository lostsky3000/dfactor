package fun.lib.actor.core;

import java.util.Iterator;
import java.util.Map.Entry;

import fun.lib.actor.api.http.DFHttpSvrRsp;

public interface IScriptHttpSvrReq {

	public String getMethod();
	public String getUri();
	public String getContentType();
	public int getContentLength();
	public boolean isIsStr();
	public Object getContent();
	public String header(String key);
	public String getUserAgent();
	
	public Iterator<Entry<String,String>> getAttrIterator();
	
	public DFHttpSvrRsp response(Object data);
}
