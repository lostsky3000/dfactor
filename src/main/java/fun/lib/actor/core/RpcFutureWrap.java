package fun.lib.actor.core;

import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.api.cb.RpcFuture;

public final class RpcFutureWrap implements RpcFuture{

	private Cb cb = null;
	private final boolean sendSucc;
	private final int sessionId;
	
	private final DFActorSystemWrap sysWrap;
	
	protected RpcFutureWrap(boolean sendSucc, int sessionId, DFActorSystemWrap sysWrap) {
		this.sendSucc = sendSucc;
		this.sessionId = sessionId;
		this.sysWrap = sysWrap;
	}
	
	@Override
	public boolean addListener(Cb cb, int timeoutMilli) {
		if(sendSucc){
			this.cb = cb;
			sysWrap.addCallback(cb, sessionId, timeoutMilli);
			return true;
		}
		return false;
	}

	@Override
	public boolean isSendSucc() {
		return sendSucc;
	}

}
