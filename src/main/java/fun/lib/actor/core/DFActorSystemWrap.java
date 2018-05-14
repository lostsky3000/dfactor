package fun.lib.actor.core;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFActorSystem;
import fun.lib.actor.api.cb.CbActorRsp;
import fun.lib.actor.api.cb.CbActorRspAsync;
import fun.lib.actor.api.cb.CbCallHere;
import fun.lib.actor.api.cb.CbCallHereBlock;
import fun.lib.actor.po.ActorProp;

public final class DFActorSystemWrap implements DFActorSystem{
	private final DFActorManager _mgr;
	private final int id;
	private final DFActorLog log;
	private final DFActor actor;
	
	public DFActorSystemWrap(int id, DFActorLog log, DFActor actor) {
		_mgr = DFActorManager.get();
		this.id = id;
		this.log = log;
		this.actor = actor;
	}
	@Override
	public final int createActor(String name, Class<? extends DFActor> classz){
		return _mgr.createActor(name, classz, null, 0, DFActorDefine.CONSUME_AUTO, false);
	}
	@Override
	public final int createActor(String name, Class<? extends DFActor> classz, Object param){
		return _mgr.createActor(name, classz, param, 0, DFActorDefine.CONSUME_AUTO, false);
	}
	@Override
	public int createActor(Class<? extends DFActor> classz) {
		return _mgr.createActor(null, classz, null, 0, DFActorDefine.CONSUME_AUTO, false);
	}
	@Override
	public int createActor(Class<? extends DFActor> classz, Object param) {
		return _mgr.createActor(null, classz, param, 0, DFActorDefine.CONSUME_AUTO, false);
	}
	@Override
	public int createActor(ActorProp prop) {
		return _mgr.createActor(prop.getName(), prop.getClassz(), prop.getParam(), 
					DFActor.transTimeRealToTimer(prop.getScheduleMilli()), 
					prop.getConsumeType(), prop.isBlock());
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
		int ret = _mgr.send(id, actor._lastSrcId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
		actor._lastSrcId = 0;
		return ret;
	}
	
	
	public final void exit(){
		_mgr.removeActor(id);
		log.verb("exit");
	}
	public final void timeout(int delay, int requestId){
		_mgr.addTimeout(id, delay, requestId, null);
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
	public int call(int dstId, int cmd, Object payload, CbActorRsp cb) {
		return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb, false);
	}
	@Override
	public int call(String dstName, int cmd, Object payload, CbActorRsp cb) {
		return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb);
	}
	@Override
	public int callHere(int dstId, int cmd, Object payload, CbCallHere cb) {
		return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb, false);
	}
	@Override
	public int callHere(String dstName, int cmd, Object payload, CbCallHere cb) {
		return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb);
	}
	@Override
	public int callHereBlock(int shardId, int cmd, Object payload, CbCallHereBlock cb) {
		return _mgr.callSysBlockActor(id, shardId, cmd, payload, cb); 
	}
	
	
}
