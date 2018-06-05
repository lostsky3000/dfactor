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
import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.define.RpcError;
import fun.lib.actor.po.ActorProp;
import io.netty.buffer.ByteBuf;

public final class DFActorSystemWrap implements DFActorSystem{
	
	private static final int RPC_TIMEOUT = 60000;
	private static final int CB_TIMEOUT = 60000;
	
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
	public int to(int dstId, int cmd, Object payload) {
		return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
	}
	@Override
	public int to(String dstName, int cmd, Object payload) {
		return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null);
	}
	@Override
	public int ret(int cmd, Object payload) {
		if(actor._hasRet){
			return -1;
		}
		actor._hasRet = true;
		if(actor._lastRpcCtx != null){
			actor._lastRpcCtx.response(cmd, payload);
			actor._lastRpcCtx = null;
			cmd = 0;
		}else{
			if(actor._lastSessionId > 0){  //has callback
				cmd = _mgr.send(id, actor._lastSrcId, actor._lastSessionId, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, true);
			}else{
				cmd = _mgr.send(id, actor._lastSrcId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
			}
		}
		actor._lastSrcId = 0;
		actor._lastSessionId = 0;
		return cmd;
	}
	@Override
	public int call(int dstId, int cmd, Object payload, Cb cb) {
		if(cb == null){
			return 1;
		}
		int sid = _createCbId();
		int ret = _mgr.send(id, dstId, sid, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
		if(ret == 0){  //send succ
			addCallback(cb, sid, CB_TIMEOUT);
		}
		return ret;
	}
	@Override
	public int call(String dstName, int cmd, Object payload, Cb cb) {
		if(cb == null){
			return 1;
		}
		int sid = _createCbId();
		int ret = _mgr.send(id, dstName, sid, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, null, null, false);
		if(ret == 0){  //send succ
			addCallback(cb, sid, CB_TIMEOUT);
		}
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
	@Override
	public RpcFuture rpc(int dstId, String dstMethod, int cmd, Object payload) {
		RpcFuture f = null;
		int sid = _createCbId();
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
	public RpcFuture rpc(String dstName, String dstMethod, int cmd, Object payload) {
		RpcFuture f = null;
		int sid = _createCbId();
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
	public int toNode(String dstNode, String dstActor, int cmd, String payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int toNode(String dstNode, String dstActor, int cmd, byte[] payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int toNode(String dstNode, String dstActor, int cmd, ByteBuf payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int toNode(String dstNode, String dstActor, int cmd, JSONObject payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	@Override
	public int toNode(String dstNode, String dstActor, int cmd, DFSerializable payload) {
		return DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, null, 0, cmd, payload);
	}
	//
	@Override
	public int toNodeByType(String dstNodeType, String dstActor, int cmd, String payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int toNodeByType(String dstNodeType, String dstActor, int cmd, JSONObject payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int toNodeByType(String dstNodeType, String dstActor, int cmd, byte[] payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int toNodeByType(String dstNodeType, String dstActor, int cmd, ByteBuf payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	@Override
	public int toNodeByType(String dstNodeType, String dstActor, int cmd, DFSerializable payload) {
		return DFClusterManager.get().broadcast(actor.name, dstNodeType, dstActor, cmd, payload);
	}
	//sendToClusterAll
	@Override
	public int toAllNode(String dstActor, int cmd, String payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int toAllNode(String dstActor, int cmd, JSONObject payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int toAllNode(String dstActor, int cmd, byte[] payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int toAllNode(String dstActor, int cmd, ByteBuf payload) {
		return DFClusterManager.get().broadcast(actor.name, null, dstActor, cmd, payload);
	}
	@Override
	public int toAllNode(String dstActor, int cmd, DFSerializable payload) {
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
	public RpcFuture rpcNode(String dstNode, String dstActor, String dstMethod, int cmd, String payload) {
		RpcFuture f = null;
		int sid = _createCbId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture rpcNode(String dstNode, String dstActor, String dstMethod, int cmd, byte[] payload) {
		RpcFuture f = null;
		int sid = _createCbId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture rpcNode(String dstNode, String dstActor, String dstMethod, int cmd, ByteBuf payload) {
		RpcFuture f = null;
		int sid = _createCbId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture rpcNode(String dstNode, String dstActor, String dstMethod, int cmd,
			DFSerializable payload) {
		RpcFuture f = null;
		int sid = _createCbId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	@Override
	public RpcFuture rpcNode(String dstNode, String dstActor, String dstMethod, int cmd, JSONObject payload) {
		RpcFuture f = null;
		int sid = _createCbId();
		int ret = DFClusterManager.get().sendToNode(actor.name, dstNode, dstActor, dstMethod, sid, cmd, payload);
		if(ret == 0){  //send succ
			f = new RpcFutureWrap(true, sid, this);
		}else{	//send failed, remove rpcCb
			f = new RpcFutureWrap(false, sid, this);
		}
		return f;
	}
	
	//
	private HashMap<Integer, Cb> _mapCb = null;
	private int _cbIdCount = 0;
	private int _createCbId(){
		int sid = ++_cbIdCount;
		if(_cbIdCount >= Integer.MAX_VALUE){
			_cbIdCount = 0;
		}
		return sid;
	}
	protected void addCallback(Cb cb, int sessionId, int timeoutMilli){
		if(_mapCb == null){
			_mapCb = new HashMap<>();
		}
		_mapCb.put(sessionId, cb);
		_mgr.addTimeout(id, (int) (timeoutMilli/DFActor.TIMER_UNIT_MILLI), DFActorDefine.SUBJECT_CB_FAILED, sessionId, null);
	}
	protected Cb procCb(int sessionId){
		if(_mapCb != null){
			return _mapCb.remove(sessionId);
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
	@Override
	public String getCurSrcNode() {
		if(actor._lastRpcCtx != null){
			return actor._lastRpcCtx.getSrcNode();
		}
		return null;
	}
	@Override
	public String getCurSrcActor() {
		if(actor._lastRpcCtx != null){
			return actor._lastRpcCtx.getSrcActor();
		}
		return null;
	}
	
	
}







