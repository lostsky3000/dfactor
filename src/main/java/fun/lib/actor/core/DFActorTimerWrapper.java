package fun.lib.actor.core;

import fun.lib.actor.api.DFActorTimer;
import fun.lib.actor.api.cb.CbTimeout;

public final class DFActorTimerWrapper implements DFActorTimer{
	
	private final int id;
	private final DFActorManager _mgr;
	
	protected DFActorTimerWrapper(int id) {
		this.id = id;
		_mgr = DFActorManager.get();
	}
	
	@Override
	public void timeout(int delayMilli, int requestId) {
		_mgr.addTimeout(id, DFActor.transTimeRealToTimer(delayMilli), requestId, null);
	}

	@Override
	public void timeout(int delayMilli, CbTimeout cb) {
		_mgr.addTimeout(id, DFActor.transTimeRealToTimer(delayMilli), 0, cb);
	}

	@Override
	public long getTimeStart() {
		return _mgr.getTimerStartNano();
	}

	@Override
	public long getTimeNow() {
		return _mgr.getTimerNowNano();
	}

}
