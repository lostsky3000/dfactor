package fun.lib.actor.core;

import fun.lib.actor.api.http.DFHttpSvrReq;

public final class DFJsHttpSvrReq implements IScriptHttpSvrReq{

	private final DFHttpSvrReq req;
	public DFJsHttpSvrReq(DFHttpSvrReq req) {
		this.req = req;
	}
	
	
	
	@Override
	public String getMethod() {
		return req.getMethod().name().toLowerCase();
	}

	@Override
	public String getUri() {
		return req.getUri();
	}
}
