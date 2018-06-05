package fun.lib.actor.core;

public interface IScriptHttpSvrRsp {

	public IScriptHttpSvrRsp headers(Object headers);
	public IScriptHttpSvrRsp status(int statusCode);
	public int send();
	
}
