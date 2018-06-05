package fun.lib.actor.api.http;


public interface DFHttpSvrRsp {
	
	//
	public DFHttpSvrRsp header(String key, String val);
	public DFHttpSvrRsp contentType(String contentType);
	public DFHttpSvrRsp userAgent(String userAgent);
	public void send();
}
