package fun.lib.actor.core;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFActorMsgCallback;
import fun.lib.actor.api.DFActorSystem;

public final class DFActorSystemWrapper implements DFActorSystem{
	private final DFActorManager _mgr;
	private final int id;
	private final DFActorLog log;
	private final DFActor actor;
	
	public DFActorSystemWrapper(int id, DFActorLog log, DFActor actor) {
		_mgr = DFActorManager.get();
		this.id = id;
		this.log = log;
		this.actor = actor;
	}
	
	public final int createActor(String name, Class<? extends DFActor> classz){
		return _mgr.createActor(name, classz, null, 0, DFActorDefine.CONSUME_AUTO, false);
	}
	public final int createActor(String name, Class<? extends DFActor> classz, Object param){
		return _mgr.createActor(name, classz, param, 0, DFActorDefine.CONSUME_AUTO, false);
	}
	public final int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit){
		return _mgr.createActor(name, classz, param, scheduleUnit, DFActorDefine.CONSUME_AUTO, false);
	}
	public final int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType){
		return _mgr.createActor(name, classz, param, scheduleUnit, consumeType, false);
	}
	public final int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType, boolean isBlockActor){
		return _mgr.createActor(name, classz, param, scheduleUnit, consumeType, isBlockActor);
	}
	//
	@Override
	public int send(int dstId, int cmd, Object payload) {
		return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
	}
	@Override
	public int send(String dstName, int cmd, Object payload) {
		return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null);
	}
	@Override
	public int sendback(int cmd, Object payload) {
		return _mgr.send(id, actor.lastSrcId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
	}
	
	
	public final void exit(){
		_mgr.removeActor(id);
		log.verb("exit");
	}
	public final void timeout(int delay, int requestId){
		_mgr.addTimeout(id, delay, requestId);
	}
	@Override
	public long getTimeStart() {
		return _mgr.getTimerStartNano();
	}
	@Override
	public long getTimeNow() {
//		return System.currentTimeMillis();
		return _mgr.getTimerNowNano();
	}
	//
	@Override
	public int call(int dstId, int cmd, Object payload, DFActorMsgCallback cb) {
		return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb, false);
	}
	@Override
	public int call(String dstName, int cmd, Object payload, DFActorMsgCallback cb) {
		return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb);
	}
}
