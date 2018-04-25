package fun.lib.actor.api.http;

public interface DFHttpServerHandler {

	public void onListenResult(boolean isSucc, String errMsg);
	
	public int onHttpRequest(Object msg);
}
