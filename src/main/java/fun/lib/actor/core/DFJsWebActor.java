package fun.lib.actor.core;

import fun.lib.actor.api.DFActorDb;
import fun.lib.actor.api.DFTcpChannel;

public final class DFJsWebActor extends DFActor implements IWebExtAPI{
	
	private final WebMysql mysql;
	public DFJsWebActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		//
		mysql = new WebMysql(db);
	}
	
	private DFVirtualHostManager _mgrHost = null;
	
	@Override
	public void onStart(Object param) {
		_mgrHost = DFVirtualHostManager.get();
	}
	
	@Override
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
		DFVirtualHost host = _mgrHost.getHost(requestId);
		if(host != null){
			DFHttpSvrReqWrap req = (DFHttpSvrReqWrap) msg;
			host.onMsg(req, this);
		}else{  //error
			log.error("virtualHost not found on port: "+requestId);
		}
		return 0;
	}

	@Override
	public IWebMysqlAPI getMysql() {
		return mysql;
	}

	private class WebMysql implements IWebMysqlAPI{
		private final DFActorDb db;
		public WebMysql(DFActorDb db) {
			this.db = db;
		}
	}
}








