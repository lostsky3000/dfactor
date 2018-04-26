package fun.lib.actor.api.http;


public interface DFHttpSvrRsp {
	
	//
	public DFHttpSvrRsp putHeader(String key, String val);
	public DFHttpSvrRsp setContentType(String contentType);
	public DFHttpSvrRsp setUserAgent(String userAgent);
	public void send();
}
