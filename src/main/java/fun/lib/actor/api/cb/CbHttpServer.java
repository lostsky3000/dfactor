package fun.lib.actor.api.cb;

public interface CbHttpServer {

	public void onListenResult(boolean isSucc, String errMsg);
	
	public int onHttpRequest(Object msg);
}
