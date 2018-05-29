package fun.lib.actor.core;

import java.util.HashMap;
import java.util.LinkedList;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.GeneratedMessageV3;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFActorSystem;
import fun.lib.actor.api.DFSerializable;
import fun.lib.actor.api.cb.CbActorRsp;
import fun.lib.actor.api.cb.CbActorRspAsync;
import fun.lib.actor.api.cb.CbCallHere;
import fun.lib.actor.api.cb.CbCallHereBlock;
import fun.lib.actor.api.cb.CbNode;
import fun.lib.actor.api.cb.CbRpc;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.define.RpcError;
import fun.lib.actor.po.ActorProp;
import io.netty.buffer.ByteBuf;

public final class DFActorSystemWrap implements DFActorSystem{
	
	private static final int RPC_TIMEOUT = 60000;
	
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
	@Override
	public final void timeout(int delay, int requestId){
		_mgr.addTimeout(id, delay, DFActorDefine.SUBJECT_TIMER, requestId, null);
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
	//
	
	@Override
	public RpcFuture callMethod(int dstId, String dstMethod, int cmd, Object payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = _mgr.send(id, dstId, sid, DFActorDefine.SUBJECT_RPC, cmd, payload, true, 
				null, actor.name, false, null, dstMethod);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture callMethod(String dstName, String dstMethod, int cmd, Object payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = _mgr.send(id, dstName, sid, DFActorDefine.SUBJECT_RPC, cmd, payload, true, null, actor.name, null, dstMethod);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	
	@Override
	public void shutdown() {
		DFActorManager.get().shutdown();
	}
	@Override
	public int sendToCluster(String dstNode, String dstActor, int cmd, String payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int sendToCluster(String dstNode, String dstActor, int cmd, byte[] payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int sendToCluster(String dstNode, String dstActor, int cmd, ByteBuf payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int sendToCluster(String dstNode, String dstActor, int cmd, JSONObject payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int sendToCluster(String dstNode, String dstActor, int cmd, DFSerializable payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	//
	@Override
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, String payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, JSONObject payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, byte[] payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, ByteBuf payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, DFSerializable payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	//sendToClusterAll
	@Override
	public int sendToClusterAll(String dstActor, int cmd, String payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterAll(String dstActor, int cmd, JSONObject payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterAll(String dstActor, int cmd, byte[] payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterAll(String dstActor, int cmd, ByteBuf payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int sendToClusterAll(String dstActor, int cmd, DFSerializable payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	
	@Override
	public boolean isNodeOnline(String nodeName) {
		return DFClusterManager.get().isNodeOnline(nodeName);
	}
	@Override
	public int getNodeNumByType(String nodeType) {
		return DFClusterManager.get().getNodeNumByType(nodeType);
	}
	@Override
	public int getAllNodeNum() {
		return DFClusterManager.get().getAllNodeNum();
	}
	//
	@Override
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, String payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, byte[] payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, ByteBuf payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd,
			DFSerializable payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, JSONObject payload) {
		RpcFuture f = null;
		int sid = _createSessionId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	
	//
	private HashMap<Integer, CbRpc> _mapRpcCb = null;
	private int _rpcCbIdCount = 0;
	
	private int _createSessionId(){
		int sid = ++_rpcCbIdCount;
		if(_rpcCbIdCount >= Integer.MAX_VALUE){
			_rpcCbIdCount = 0;
		}
		return sid;
	}
	
	protected void addRpcCallback(CbRpc cb, int sessionId, int timeoutMilli){
		if(_mapRpcCb == null){
			_mapRpcCb = new HashMap<>();
		}
		_mapRpcCb.put(sessionId, cb);
		_mgr.addTimeout(id, (int) (timeoutMilli/DFActor.TIMER_UNIT_MILLI), DFActorDefine.SUBJECT_RPC_FAIL, sessionId, null);
	}
	
	protected CbRpc procRPCCb(int sessionId){
		if(_mapRpcCb != null){
			return _mapRpcCb.remove(sessionId);
		}
		return null;
	}
	@Override
	public int listenNodeAll(CbNode callback) {
		RegNodeReq req = new RegNodeReq(RegNodeReq.ALL, null, id, callback);
		return _mgr.send(id, DFClusterActor.NAME, 0, DFActorDefine.SUBJECT_USER, 
				DFClusterActor.CMD_REG_NODE_LISTENER, req, true, null, null);
	}
	@Override
	public int listenNodeByType(String nodeType, CbNode callback) {
		RegNodeReq req = new RegNodeReq(RegNodeReq.NODE_TYPE, nodeType, id, callback);
		return _mgr.send(id, DFClusterActor.NAME, 0, DFActorDefine.SUBJECT_USER, 
				DFClusterActor.CMD_REG_NODE_LISTENER, req, true, null, null);
	}
	@Override
	public int listenNodeByName(String nodeName, CbNode callback) {
		RegNodeReq req = new RegNodeReq(RegNodeReq.NODE_NAME, nodeName, id, callback);
		return _mgr.send(id, DFClusterActor.NAME, 0, DFActorDefine.SUBJECT_USER, 
				DFClusterActor.CMD_REG_NODE_LISTENER, req, true, null, null);
	}
	
	
	
}







