package fun.lib.actor.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.concurrent.locks.StampedLock;

import com.funtag.util.concurrent.DFSpinLock;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFUdpChannel;
import fun.lib.actor.api.cb.CbActorRsp;
import fun.lib.actor.api.cb.CbCallHere;
import fun.lib.actor.api.cb.CbCallHereBlock;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.api.cb.RpcContext;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.cb.CbNode;
import fun.lib.actor.api.cb.CbRpc;
import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.api.http.DFHttpCliRsp;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import fun.lib.actor.po.DFNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public final class DFActorWrap {

	
	private final LinkedList<DFActorMessage>[] _arrQueue = new LinkedList[2];
	private int[] _arrQueueSize = new int[2];
	private LinkedList<DFActorMessage> _queueWrite = null;
	private byte _queueWriteIdx = 0;
	private byte _queueReadIdx = 1;
	
	private final ReentrantReadWriteLock _lockQueue = new ReentrantReadWriteLock();
//	private final ReadLock _lockQueueRead = _lockQueue.readLock();
	private final WriteLock _lockQueueWrite = _lockQueue.writeLock();
//	private final DFSpinLock _lockQueue = new DFSpinLock();
//	private final StampedLock _lockQueue = new StampedLock();
	
	private final DFActor _actor;
	private final byte _actorConsumeType;
	private boolean _bInGlobalQueue = false;
	private volatile boolean _bRemoved = false;
	private final int _actorId;
	private final boolean _isBlockActor;
	private final DFActorManager _actorMgr;
	private final String _consumeLock;
	private final boolean _isClusterActor;
	private HashMap<String,Method> _mapMethod = null;
	private final DFActorSystemWrap _wrapSys;
	
	protected DFActorWrap(final DFActor actor, final boolean isCluster) {
		this._actor = actor;
		_actorId = actor.getId();
		_actorConsumeType = (byte) actor.getConsumeType();
		_isBlockActor = actor.isBlockActor;
		_isClusterActor = isCluster;
		_consumeLock = _actorId + "_" + actor.name;
		//
		for(int i=0; i<2; ++i){
			_arrQueue[i] = new LinkedList<>();
			_arrQueueSize[i] = 0;
		}
		_queueWriteIdx = 0;
		_queueReadIdx = 1;
		_queueWrite = _arrQueue[_queueWriteIdx];
		//
		_actorMgr = DFActorManager.get();
		//
		_wrapSys = (DFActorSystemWrap) actor.sys;
	}
	
	protected int pushMsg(int srcId, int sessionId, 
			int subject, int cmd, Object payload, Object context, boolean addTail, 
			Object userHandler, boolean isCb, Object payload2, String method){
		if(_bRemoved){
			return 1;
		}
		final DFActorMessage msg = _actorMgr.newActorMessage(
				srcId, _actorId, sessionId, subject, cmd, payload, context, userHandler, isCb, payload2, method);
				//new DFActorMessage(srcId, _actorId, sessionId, subject, cmd, payload);
		//
		_lockQueueWrite.lock();
		try{
			if(addTail){
				_queueWrite.offer(msg);
			}else{
				_queueWrite.offerFirst(msg);
			}
			++_arrQueueSize[_queueWriteIdx];
			if(!_bInGlobalQueue){  //add to global queue
				_bInGlobalQueue = true;
				return 0;
			}
		}finally{
			_lockQueueWrite.unlock();
		}
		return 2;
	}
	protected int consumeMsg(int consumeType){
		if(_actorConsumeType != DFActorDefine.CONSUME_AUTO){
			consumeType = _actorConsumeType;
		}
		//
		int queueMsgLeft = 0;
		final LinkedList<DFActorMessage> queueRead;
		_lockQueueWrite.lock();
		try{
			final int curReadQueueSize = _arrQueueSize[_queueReadIdx];
			if(curReadQueueSize > 0){  //cur readQueue not empty, continue reading
				queueMsgLeft = curReadQueueSize;
				queueRead = _arrQueue[_queueReadIdx];
			}else{  //cur readQueue empty, swap queue
				queueMsgLeft = _arrQueueSize[_queueWriteIdx];
				queueRead = _arrQueue[_queueWriteIdx];
				//swap write queue
				_queueWriteIdx = _queueReadIdx;
				_queueReadIdx = (byte) ((_queueReadIdx + 1)%2);
				_queueWrite = _arrQueue[_queueWriteIdx];
			}
		}finally{
			_lockQueueWrite.unlock();
		}
		//
		int targetNum = 1;  //default proc num 
		if(queueMsgLeft > 1){
			if(consumeType == DFActorDefine.CONSUME_ALL){
				targetNum = queueMsgLeft;
			}else if(consumeType == DFActorDefine.CONSUME_HALF){
				targetNum = Math.max(1, queueMsgLeft/2);
			}
		}
		//consume
		synchronized (_consumeLock) {
			final Iterator<DFActorMessage> it = queueRead.iterator();
			while(it.hasNext()){
				if(_bRemoved){
					break;
				}
				final DFActorMessage msg = it.next();
				it.remove();
				--queueMsgLeft;
				try{
					final int subject = msg.subject;
					if(subject == DFActorDefine.SUBJECT_SCHEDULE){
						_actor.onSchedule(msg.cmd);
					}else if(subject == DFActorDefine.SUBJECT_TIMER){
						if(msg.userHandler != null){ //has callback
							((CbTimeout)msg.userHandler).onTimeout();
						}else{
							_actor.onTimeout(msg.sessionId);
						}
					}else if(DFActorDefine.SUBJECT_USER == msg.subject){
						_procUserMsg(msg);
					}
					else if(msg.subject == DFActorDefine.SUBJECT_NET){
						if(msg.cmd == DFActorDefine.NET_UDP_MESSAGE){ //udp msg
							final int ret = _actor.onUdpServerRecvMsg(msg.sessionId, (DFUdpChannel) msg.context, (DatagramPacket) msg.payload);
							if(ret != DFActorDefine.MSG_MANUAL_RELEASE){ //auto release
								ReferenceCountUtil.release(msg.payload);
							}
						}else if(msg.cmd == DFActorDefine.NET_TCP_MESSAGE){ //tcp msg
							final Object payload = msg.payload;
							DFTcpChannelWrap ch = (DFTcpChannelWrap) msg.context;
							if(ch.getTcpDecodeType() == DFActorDefine.TCP_DECODE_HTTP && 
									msg.userHandler != null){  //http, has callback
								if(msg.sessionId == 1){  //recv rsp as server
									CbHttpServer handler = (CbHttpServer) msg.userHandler;
									if(handler.onHttpRequest(payload) != DFActorDefine.MSG_MANUAL_RELEASE){
										if(payload instanceof DFHttpSvrReq){
											((DFHttpSvrReq)payload).release();
										}else{
											ReferenceCountUtil.release(payload);
										}
									}
								}else{  //recv rsp as client
									CbHttpClient handler = (CbHttpClient) msg.userHandler;
									if(handler.onHttpResponse(payload, true, null) != DFActorDefine.MSG_MANUAL_RELEASE){
										if(payload instanceof DFHttpCliRsp){
											((DFHttpCliRsp)payload).release();
										}else{
											ReferenceCountUtil.release(payload);
										}
									}
								}
							}else{
								DFTcpChannelWrap chWrap = (DFTcpChannelWrap) msg.context;
								int curDstActor = chWrap.getMsgActor();
								if(curDstActor == _actor.id || curDstActor == 0){
									int ret = _actor.onTcpRecvMsg(msg.srcId, chWrap, payload);
									if(ret != DFActorDefine.MSG_MANUAL_RELEASE && payload!=null){ //auto release
										ReferenceCountUtil.release(payload);
									}
								}else{  //dstActor has changed
									_actorMgr.send(msg.srcId, curDstActor, msg.sessionId, DFActorDefine.SUBJECT_NET, 
											DFActorDefine.NET_TCP_MESSAGE, payload, true);
								}
							}
						}
						else if(msg.cmd == DFActorDefine.NET_TCP_CONNECT_OPEN){
							_actor.onTcpConnOpen(msg.sessionId, (DFTcpChannel) msg.payload);
						}else if(msg.cmd == DFActorDefine.NET_TCP_CONNECT_CLOSE){
							DFTcpChannelWrap chWrap = (DFTcpChannelWrap) msg.payload;
							int curDstActor = chWrap.getStatusActor(); //当前actorId
							if(curDstActor == _actor.id || curDstActor == 0){
								_actor.onTcpConnClose(msg.sessionId, chWrap);
							}else{  //dstActorId has changed
								_actorMgr.send(msg.srcId, curDstActor, msg.sessionId, DFActorDefine.SUBJECT_NET, 
										DFActorDefine.NET_TCP_CONNECT_CLOSE, chWrap, true);
							}
						}else if(msg.cmd == DFActorDefine.NET_TCP_LISTEN_RESULT){
							final DFActorEvent event = (DFActorEvent) msg.payload;
							final boolean isSucc = event.getWhat()==DFActorErrorCode.SUCC?true:false;
							if(msg.userHandler != null && msg.userHandler instanceof CbHttpServer){
								CbHttpServer handler = (CbHttpServer) msg.userHandler;
								handler.onListenResult(isSucc, event.getMsg());
							}else{
								_actor.onTcpServerListenResult(msg.sessionId, isSucc, event.getMsg());
							}
						}else if(msg.cmd == DFActorDefine.NET_TCP_CONNECT_RESULT){
							final DFActorEvent event = (DFActorEvent) msg.payload;
							final boolean isSucc = event.getWhat()==DFActorErrorCode.SUCC?true:false;
							if(!isSucc && msg.userHandler != null && msg.userHandler instanceof CbHttpClient){
								CbHttpClient handler = (CbHttpClient) msg.userHandler;
								handler.onHttpResponse(null, false, event.getMsg());
							}else{
								_actor.onTcpClientConnResult(msg.sessionId, isSucc, event.getMsg());
							}
						}else if(msg.cmd == DFActorDefine.NET_UDP_LISTEN_RESULT){
							final DFActorEvent event = (DFActorEvent) msg.payload;
							final boolean isSucc = event.getWhat()==DFActorErrorCode.SUCC?true:false;
							_actor.onUdpServerListenResult(msg.sessionId, isSucc, event.getMsg(), 
									(DFUdpChannel) event.getExtObj1());
						}
					}else if(msg.subject == DFActorDefine.SUBJECT_CLUSTER){
						_actor.onClusterMessage((String)msg.payload2, (String)msg.context, (String)msg.userHandler, msg.cmd, msg.payload);
					}else if(msg.subject == DFActorDefine.SUBJECT_RPC){
						if(msg.method != null){   //call method
							Method method = null;
							if(_mapMethod == null){
								_mapMethod = new HashMap<>();
							}else{
								method = _mapMethod.get(msg.method);
							}
							if(method == null){
								method = _actor.getClass().getMethod(msg.method, int.class, Object.class, RpcContext.class);
								method.setAccessible(true);
								_mapMethod.put(msg.method, method);
							}
							method.invoke(_actor, msg.cmd, msg.payload, 
									new DFRpcContext((String)msg.context, (String)msg.userHandler, msg.sessionId));
						}else{   //reponse
							CbRpc cb = _wrapSys.procRPCCb(msg.sessionId);
							if(cb != null){
								cb.onResponse(msg.cmd, msg.payload);
							}
							cb = null;
						}
					}else if(msg.subject == DFActorDefine.SUBJECT_RPC_FAIL){
						CbRpc cb =_wrapSys.procRPCCb(msg.sessionId);
						if(cb != null){
							cb.onFailed(msg.cmd);
						}
					}else if(DFActorDefine.SUBJECT_NODE_EVENT == msg.subject){
						if(msg.userHandler != null){  //has callback
							CbNode cb = (CbNode) msg.userHandler;
							if(msg.cmd == 1){  //node remove
								cb.onNodeRemove((DFNode) msg.payload);
							}else{
								cb.onNodeAdd((DFNode) msg.payload);
							}
						}
					}
					else if(msg.subject == DFActorDefine.SUBJECT_START){
						_actor.onStart(msg.payload);
					}
					else{
						_procUserMsg(msg);
					}
				}catch(Throwable e){  //catch logic exception
					e.printStackTrace();
				}finally{
					if(--targetNum < 1){  //match target num
						break;
					}
					//
					_actorMgr.recycleActorMessage(msg);
				}
			}
		}
		_arrQueueSize[_queueReadIdx] = queueMsgLeft; //readQueue left num
		//
		if(_bRemoved){  //释放占用io内存的消息
			_release();
			return 1;
		}
		//check
		_lockQueueWrite.lock();
		try{
			if(queueMsgLeft > 0 || _arrQueueSize[_queueWriteIdx] > 0){ //still has msg in either queue, back to global queue
				_bInGlobalQueue = true;
				return 0;
			}else{	//both queue empty, mark removed from global queue
				_bInGlobalQueue = false;
			}
		}finally{
			_lockQueueWrite.unlock();
		}
		return 2;
	}
	
	private void _procUserMsg(DFActorMessage msg){
		_actor._lastSrcId = msg.srcId;
		boolean noCb = true;
		CbActorReq queryCb = null;
		Object userHandler = msg.userHandler;
		if(userHandler != null){ //callback
			if(userHandler instanceof CbCallHere){
				CbCallHere tmpCb = (CbCallHere) userHandler;
				if(msg.isCb){
					tmpCb.onCallback(msg.cmd, msg.payload);
				}else{
					_actor._hasCalledback = false;
					_actor._lastUserHandler = userHandler;
					tmpCb.inOtherActor(msg.cmd, msg.payload, _actor);
					_actor._hasCalledback = true;
					_actor._lastUserHandler = null;
				}
				noCb = false;
			}else if(userHandler instanceof CbCallHereBlock){
				CbCallHereBlock tmpCb = (CbCallHereBlock) userHandler;
				if(msg.isCb){
					tmpCb.onCallback(msg.cmd, msg.payload);
				}else{
					_actor._hasCalledback = false;
					_actor._lastUserHandler = userHandler;
					tmpCb.inBlockActor(msg.cmd, msg.payload, _actor);
					_actor._hasCalledback = true;
					_actor._lastUserHandler = null;
				}
				noCb = false;
			}
			else{
				if(msg.isCb){ //callback
					CbActorRsp cb = (CbActorRsp) userHandler;
					cb.onCallback(msg.cmd, msg.payload);
					noCb = false;
				}else{ //query, has callback func
					queryCb = new DFMsgBackWrap(msg.srcId, userHandler);
				}
			}
		}
		if(noCb){
			_actor.onMessage(msg.srcId, msg.cmd, msg.payload, queryCb); //msg.sessionId, msg.subject, 
		}
	}
	
	protected void markRemoved(){
		_bRemoved = true;
	}
	protected boolean isRemoved(){
		return _bRemoved;
	}
	
	protected int getActorId(){
		return _actor.id;
	}
	protected String getActorName(){
		return _actor.name;
	}
	protected boolean isBlockActor(){
		return _isBlockActor;
	}
	protected boolean isClusterActor(){
		return _isClusterActor;
	}
	
	private void _release(){
		_lockQueueWrite.lock();
		try{
			for(int i=0; i<2; ++i){
				final LinkedList<DFActorMessage> q = _arrQueue[i];
				final Iterator<DFActorMessage> itMsg = q.iterator();
				while(itMsg.hasNext()){
					final DFActorMessage m = itMsg.next();
					final Object payload = m.payload;
					if(payload != null){ //
						if(payload instanceof DFHttpSvrReq){
							((DFHttpSvrReq)payload).release();
						}else if(payload instanceof DFHttpCliRsp){
							((DFHttpCliRsp)payload).release();
						}else if(payload instanceof ByteBuf){
							ReferenceCountUtil.release(payload);
						}
						m.payload = null;
					}
				}
				q.clear();
			}
		}finally{
			_lockQueueWrite.unlock();
		}
	}
	
	class DFMsgBackWrap implements CbActorReq{
		private int srcId = 0;
		private Object userHandler = null;
		private boolean hasCalled = false;
		
		private DFMsgBackWrap(int srcId, Object userHandler) {
			this.srcId = srcId;
			this.userHandler = userHandler;
		}
		@Override
		public int callback(int cmd, Object payload) {
			if(hasCalled){ //已经回调过
				return -1;
			}
			hasCalled = true;
			return _actorMgr.sendCallback(_actorId, srcId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, userHandler);
		}
	}
	class DFRpcContext implements RpcContext{
		private boolean _hasRsp = false;
		private final String _srcNode;
		private final String _srcActor;
		private final int _sid;
		private final boolean _isRemote;
		private DFRpcContext(String srcNode, String srcActor, int sid) {
			this._srcNode = srcNode;
			this._srcActor = srcActor;
			this._sid = sid;
			if(srcNode == null){
				_isRemote = false;
			}else{
				_isRemote = true;
			}
		}
		@Override
		public void response(int cmd, Object payload) {
			if(_hasRsp){
				return ;
			}
			_hasRsp = true;
			if(_srcNode == null){  //local 
				_actorMgr.send(_actorId, _srcActor, _sid, DFActorDefine.SUBJECT_RPC, cmd, payload, true, null, null);
			}else{  //remote
				if(payload instanceof String){
					DFClusterManager.get().sendToNode(_actor.name, _srcNode, _srcActor, null, _sid, cmd, (String) payload);
				}else if(payload instanceof ByteBuf){
					DFClusterManager.get().sendToNode(_actor.name, _srcNode, _srcActor, null, _sid, cmd, (ByteBuf) payload);
				}else{
					DFClusterManager.get().sendToNode(_actor.name, _srcNode, _srcActor, null, _sid, cmd, (byte[]) payload);
				}
			}
		}
		@Override
		public String getSrcNode() {
			return _srcNode;
		}
		@Override
		public String getSrcActor() {
			return _srcActor;
		}
		@Override
		public boolean isRemote() {
			return _isRemote;
		}
		
	}
}
