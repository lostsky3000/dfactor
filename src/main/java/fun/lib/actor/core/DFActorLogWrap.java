package fun.lib.actor.core;

import com.funtag.util.log.DFLogFactory;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.helper.ActorLogData;

public final class DFActorLogWrap implements DFActorLog{
	private final DFActorManager mgr;
	private final int id;
	private final String name;
	public DFActorLogWrap(int id, String name) {
		this.id = id;
		this.name = name;
		this.mgr = DFActorManager.get();
	}
	
	@Override
	public void verb(String msg) {
		_log(DFLogFactory.LEVEL_VERB, msg);
	}
	@Override
	public void debug(String msg) {
		_log(DFLogFactory.LEVEL_DEBUG, msg);
	}
	@Override
	public void info(String msg) {
		_log(DFLogFactory.LEVEL_INFO, msg);
	}
	@Override
	public void warn(String msg) {
		_log(DFLogFactory.LEVEL_WARN, msg);
	}
	@Override
	public void error(String msg) {
		_log(DFLogFactory.LEVEL_ERROR, msg);
	}
	@Override
	public void fatal(String msg) {
		_log(DFLogFactory.LEVEL_FATAL, msg);
	}
	private final void _log(int level, String msg){
		mgr.send(id, DFActorDefine.ACTOR_ID_LOG, 0, 0, 0, 
				new ActorLogData(level, msg, name), true, null, null, false);
	}
}
