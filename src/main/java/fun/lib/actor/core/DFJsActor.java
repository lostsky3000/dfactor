package fun.lib.actor.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.funtag.util.fs.DFFileMonitor;
import com.funtag.util.fs.IFSMonitor;
import com.funtag.util.script.DFJsPageModel;
import com.funtag.util.script.DFJsUtil;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.GeneratedMessageV3.Builder;
import com.mongodb.client.MongoDatabase;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.cb.CbNode;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.api.http.DFHttpCliReq;
import fun.lib.actor.api.http.DFHttpCliRsp;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFDbCfg;
import fun.lib.actor.po.DFMongoCfg;
import fun.lib.actor.po.DFNode;
import fun.lib.actor.po.DFPageInfo;
import fun.lib.actor.po.DFRedisCfg;
import fun.lib.actor.po.DFSSLConfig;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import redis.clients.jedis.Jedis;

public final class DFJsActor extends DFActor implements IScriptAPI{

	public DFJsActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		this.isScriptActor = true;
	}
	private DFActorManagerJs _mgrActorJs = null;
	private ScriptObjectMirror _js = null;
	
	private boolean _regOnStart = true;
	private boolean _regOnMessage = true;
	private boolean _regOnTimeout = true;
	private boolean _regOnSchedule = true;
	private boolean _regOnTcpConnClose = true;
	private boolean _regOnTcpMsg = true;
	private boolean _regOnNodeMsg = true;
	//
	@Override
	public void onStart(Object param) {
		_mgrActorJs = DFActorManagerJs.get();
		HashMap<String,Object> mapParam = (HashMap<String, Object>) param;
		ScriptObjectMirror mirFunc = (ScriptObjectMirror) mapParam.get("template");
		Object startParam = mapParam.get("param");
		synchronized (mirFunc) {
			_js = (ScriptObjectMirror) mirFunc.newObject();
		}
		//
		if(!_checkFunction("onStart")) _regOnStart = false;
		if(!_checkFunction("onMsg")) _regOnMessage = false;
		if(!_checkFunction("onTimeout")) _regOnTimeout = false;
		if(!_checkFunction("onTick")) _regOnSchedule = false;
		if(!_checkFunction("onTcpMsg")) _regOnTcpMsg = false;
		if(!_checkFunction("onTcpClose")) _regOnTcpConnClose = false;
		if(!_checkFunction("onNodeMsg")) _regOnNodeMsg = false;
		//set df
		ScriptObjectMirror mirFnApi = (ScriptObjectMirror) mapParam.get("fnApi");
		ScriptObjectMirror mirApi = null;
		synchronized (mirFnApi) {
			mirApi = (ScriptObjectMirror) mirFnApi.newObject(this.id, this.name, (IScriptAPI)this);
		}
		_js.setMember("df", mirApi);
		//
		if(_regOnStart){
			_js.callMember("onStart", startParam);
		}
		//
		mapParam.clear(); mapParam = null;
	}
	
	@Override
	public int onMessage(int cmd, Object payload, int srcId) {
		if(_regOnMessage){
			_js.callMember("onMsg", cmd, payload, srcId);
		}
		return 0;
	}
	@Override
	public int onClusterMessage(String srcType, String srcNode, String srcActor, int cmd, Object payload) {
		if(_regOnNodeMsg){
			_js.callMember("onNodeMsg", cmd, payload, srcNode, srcActor, srcType);
		}
		return 0;
	}
	protected int onRpcCall(Object method, int cmd, Object payload){
		try{
			_js.callMember((String)method, cmd, payload);
		}catch(Throwable e){
			e.printStackTrace();
		}
		return 0;
	}
	@Override
	public void onTimeout(int requestId) {
		if(_regOnTimeout){
			_js.callMember("onTimeout", requestId);
		}
	}
	@Override
	public void onSchedule(long dltMilli) {
		if(_regOnSchedule){
			_js.callMember("onTick", dltMilli);
		}
	}
	
	private DFJsEvent _jsEvent = new DFJsEvent(null, false, null);
	@Override
	public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
		if(_jsEvent == null) _jsEvent = new DFJsEvent(null, false, null);
		ScriptObjectMirror cb = _mapTcpSvrJsFunc.get(requestId);
		if(cb != null){ //has cb
			if(!isSucc){
				_mapTcpSvrJsFunc.remove(requestId);
			}
			_jsEvent.type = "ret"; _jsEvent.succ = isSucc; _jsEvent.err = errMsg;
			cb.call(_js, _jsEvent);
		}
	}
	@Override
	public void onTcpClientConnResult(int requestId, boolean isSucc, String errMsg) {
		if(_jsEvent == null) _jsEvent = new DFJsEvent(null, false, null);
		if(_mapTcpCliJsFunc != null){
			ScriptObjectMirror cb = _mapTcpCliJsFunc.get(requestId);
			if(cb != null){ //has cb
				if(!isSucc){
					_mapTcpCliJsFunc.remove(requestId);
				}
				_jsEvent.type = "ret"; _jsEvent.succ = isSucc; _jsEvent.err = errMsg;
				cb.call(_js, _jsEvent);
			}
		}
	}
	@Override
	public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
		if(_jsEvent == null) _jsEvent = new DFJsEvent(null, false, null);
		ScriptObjectMirror cb = null;
		if(requestId > 0){ //as server
			cb = _mapTcpSvrJsFunc.get(requestId);
		}else{ //as client
			cb = _mapTcpCliJsFunc.get(requestId);
		}
		if(_mapChJsFunc==null) _mapChJsFunc = new HashMap<>();
		if(cb != null){
			JsTcpChannel chWrap = new JsTcpChannel(cb, channel);
			int chId = channel.getChannelId();
			_mapChJsFunc.put(chId, chWrap);
			_jsEvent.type = "open";
			cb.call(_js, _jsEvent, chId); //notify js
		}
	}
	@Override
	public void onTcpConnClose(int requestId, DFTcpChannel channel) {	
		if(_mapChJsFunc == null) _mapChJsFunc = new HashMap<>();
		int chId = channel.getChannelId();
		JsTcpChannel chWrap = _mapChJsFunc.remove(chId);
		if(chWrap != null){ //notify js
			_jsEvent.type = "close";
			chWrap.cbFunc.call(_js, _jsEvent, chId);
		}else if(_regOnTcpConnClose){  //没有指定回调，检测是否有默认回调
			_js.callMember("onTcpClose", chId);
		}
	}
	@Override
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
		if(_mapChJsFunc == null) _mapChJsFunc = new HashMap<>();
		int chId = channel.getChannelId();
		JsTcpChannel chWrap = _mapChJsFunc.get(chId);
		Object msgOut = msg;
		if(msg instanceof ByteBuf){
			msgOut = DFJsBuffer.newBuffer((ByteBuf)msg);
		}else if(msg instanceof DFHttpSvrReq){
			DFHttpSvrReq req = (DFHttpSvrReq) msg;
			msgOut = (IScriptHttpSvrReq)new DFJsHttpSvrReq(req);
		}
		if(chWrap != null){ //notify js
			_jsEvent.type = "msg";
			chWrap.cbFunc.call(_js, _jsEvent, chId, msgOut);
		}else if(_regOnTcpMsg){ //没有指定回调，检测是否有默认回调
			_js.callMember("onTcpMsg", chId, msgOut);
		}
		return 0;
	}
	//
	private HashMap<Integer, ScriptObjectMirror> _mapTcpCliJsFunc = null;
	private HashMap<Integer, ScriptObjectMirror> _mapTcpSvrJsFunc = null;
	private HashMap<Integer, JsTcpChannel> _mapChJsFunc = null;
	@Override
	public boolean tcpSvr(Object cfg, Object func) {
		boolean bRet = false;
		do {
			try{
				if(cfg == null || !(cfg instanceof ScriptObjectMirror) || ScriptObjectMirror.isUndefined(cfg)){
					log.error("invalid cfg for tcpSvr: "+cfg); break;
				}
				if(!DFJsUtil.isJsFunction(func)){
					log.error("invalid func for tcpSvr: "+func); break;
				}
				ScriptObjectMirror cfgWrap = (ScriptObjectMirror) cfg;
				int port = (Integer)cfgWrap.get("port");
				if(port <= 0){
					log.error("invalid port for tcpSvr: "+port); break;
				}
				if(_mapTcpSvrJsFunc == null) _mapTcpSvrJsFunc = new HashMap<>();
				if(_mapTcpSvrJsFunc.containsKey(port)){
					log.error("port already inuse: "+port); break;
				}
				int workerTh = 0, bossTh = 0;
				boolean websocket = false;
				Object obj = cfgWrap.get("worker");
				workerTh = obj==null?workerTh:(Integer)obj;
				obj = cfgWrap.get("boss");
				bossTh = obj==null?bossTh:(Integer)obj;
				obj = cfgWrap.get("ws");
				websocket = obj==null?false:true;
				//
				log.info("start try tcpSvr, port="+port+", worker="+workerTh+", boss="+bossTh+", isWebsocket="+websocket);
				DFTcpServerCfg svrCfg = new DFTcpServerCfg(port, workerTh, bossTh)
						.setTcpProtocol(websocket?DFActorDefine.TCP_DECODE_WEBSOCKET:DFActorDefine.TCP_DECODE_LENGTH);
				if(websocket){  //检测uri
					obj = ((ScriptObjectMirror)cfgWrap.get("ws")).get("uri");
					if(obj != null){
						svrCfg.setWsUri(((String)obj).trim());
					}
				}
				//check ssl
				obj = cfgWrap.get("ssl");
				if(obj!=null && !ScriptObjectMirror.isUndefined(obj)){  //use ssl
					DFSSLConfig sslCfg = DFSSLConfig.newCfg()
							.certPath((String)((ScriptObjectMirror)obj).get("cert"))
							.pemPath((String)((ScriptObjectMirror)obj).get("key"));
					svrCfg.setSslConfig(sslCfg);
				}
				//
				net.tcpSvr(svrCfg);
				_mapTcpSvrJsFunc.put(port, (ScriptObjectMirror) func);  //map port<->jsFunc 
			}catch(Throwable e){
				e.printStackTrace(); break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private int _tcpCliReqIdCount = 0;
	private int _genTcpCliReqId(){
		int reqId = --_tcpCliReqIdCount;
		if(_tcpCliReqIdCount == Integer.MIN_VALUE){
			_tcpCliReqIdCount = 0;
		}
		return reqId;
	}
	@Override
	public boolean tcpCli(Object cfg, Object func) {
		boolean bRet = false;
		do {
			try{
				if(cfg == null || !(cfg instanceof ScriptObjectMirror) || ScriptObjectMirror.isUndefined(cfg)){
					log.error("invalid cfg for tcpCli: "+cfg); break;
				}
				if(!DFJsUtil.isJsFunction(func)){
					log.error("invalid func for tcpCli: "+func); break;
				}
				ScriptObjectMirror cfgWrap = (ScriptObjectMirror) cfg;
				int port = (Integer)cfgWrap.get("port");
				if(port <= 0){
					log.error("invalid port for tcpCli: "+port); break;
				}
				String host = (String) cfgWrap.get("host");
				if(host == null){
					log.error("invalid host for tcpCli: "+host); break;
				}
				host = host.trim();
				int reqId = _genTcpCliReqId();
				Object obj = cfgWrap.get("ssl");
				boolean ssl = obj==null?false:(Boolean)obj;
				DFTcpClientCfg cfgCli = new DFTcpClientCfg(host, port);
				obj = cfgWrap.get("ws");
				boolean websocket = obj==null?false:true;
				if(websocket){
					cfgCli.setTcpProtocol(DFActorDefine.TCP_DECODE_WEBSOCKET);
					obj = ((ScriptObjectMirror)obj).get("uri");
					if(obj != null){
						cfgCli.setWsUri(ssl?"wss":"ws" + "://"+host+":"+port+"/"+obj);
					}else{
						cfgCli.setWsUri(ssl?"wss":"ws" + "://"+host+":"+port);
					}
				}else{
					cfgCli.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
				}		
				if(ssl){
					cfgCli.setSslCfg(DFSSLConfig.newCfg());
				}
				obj = cfgWrap.get("timeout");
				if(obj != null) cfgCli.setConnTimeout((Integer)obj);
				
				
				net.tcpCli(cfgCli, reqId);
				if(_mapTcpCliJsFunc == null) _mapTcpCliJsFunc = new HashMap<>();
				_mapTcpCliJsFunc.put(reqId, (ScriptObjectMirror)func);
				log.info("start try tcpCli, "+host+":"+port);
			}catch(Throwable e){
				e.printStackTrace(); break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	
	//
	private boolean _checkFunction(String name){
		Object obj = _js.getMember(name);
		if(obj != null && !ScriptObjectMirror.isUndefined(obj)){
			ScriptObjectMirror mir = (ScriptObjectMirror) obj;
			if(mir.isFunction()){
				return true;
			}
		}
		return false;
	}

	@Override
	public IScriptBuffer newBuf(int capacity) {
		return DFJsBuffer.newBuffer(capacity);
	}

	@Override
	public Object bufToProto(IScriptBuffer buf, String className) {
		try{
			GeneratedMessageV3 m = _mgrActorJs.getProtoType(className);
			if(m != null){
				DFJsBuffer bufWrap = (DFJsBuffer) buf;
				return m.getParserForType().parseFrom(bufWrap.getBuf().nioBuffer());
			}
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object getProtoBuilder(String className) {
		try{
			GeneratedMessageV3 m = _mgrActorJs.getProtoType(className);
			if(m != null){
				return m.newBuilderForType();
			}
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public IScriptBuffer protoToBuf(Builder<?> builder) {
		try{
			byte[] bytes = builder.build().toByteArray();
			ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(bytes.length);
			buf.writeBytes(bytes);
			IScriptBuffer bufOut = DFJsBuffer.newBuffer(buf);
			return bufOut;
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean lockWrite(Object var, Object func) {
		WriteLock lock = null;
		try{
			if( !(var instanceof ScriptObjectMirror) ){ //基本类型 无法加锁
				log.error("invalid dataType for lockWrite: "+var.getClass());
				return false;
			}
			if( !(func instanceof ScriptObjectMirror) ){
				log.error("invalid func for lockWrite: "+func.getClass());
				return false;
			}
			ScriptObjectMirror funcWrap = (ScriptObjectMirror) func;
			if(!funcWrap.isFunction()){
				log.error("invalid func for lockWrite: "+func.getClass());
				return false;
			}
			ReentrantReadWriteLock lockRW = _mapJsGlobalLock.get(var);
			if(lockRW == null){
				synchronized (var) {
					lockRW =  _mapJsGlobalLock.get(var);
					if(lockRW == null){
						lockRW = new ReentrantReadWriteLock();
						_mapJsGlobalLock.put(var, lockRW);
					}
				}
			}
			lock = lockRW.writeLock();
			lock.lock();
			funcWrap.call(_js);
		}catch(Throwable e){
			e.printStackTrace();
		}finally{
			if(lock != null){
				lock.unlock();
			}
		}
		return true;
	}
	@Override
	public boolean lockRead(Object var, Object func) {
		ReadLock lock = null;
		try{
			if( !(var instanceof ScriptObjectMirror) ){ //基本类型 无法加锁
				log.error("invalid dataType for lockRead: "+var.getClass());
				return false;
			}
			if( !(func instanceof ScriptObjectMirror) ){
				log.error("invalid func for lockRead: "+func.getClass());
				return false;
			}
			ScriptObjectMirror funcWrap = (ScriptObjectMirror) func;
			if(!funcWrap.isFunction()){
				log.error("invalid func for lockRead: "+func.getClass());
				return false;
			}
			ReentrantReadWriteLock lockRW = _mapJsGlobalLock.get(var);
			if(lockRW == null){
				synchronized (var) {
					lockRW =  _mapJsGlobalLock.get(var);
					if(lockRW == null){
						lockRW = new ReentrantReadWriteLock();
						_mapJsGlobalLock.put(var, lockRW);
					}
				}
			}
			lock = lockRW.readLock();
			lock.lock();
			funcWrap.call(_js);
		}catch(Throwable e){
			e.printStackTrace();
		}finally{
			if(lock != null){
				lock.unlock();
			}
		}
		return true;
	}
	
	private static final ConcurrentHashMap<Object, ReentrantReadWriteLock> _mapJsGlobalLock = new ConcurrentHashMap<>();
	
	private static class JsTcpChannel{
		private final ScriptObjectMirror cbFunc;
		private DFTcpChannel channel = null;
		private JsTcpChannel(ScriptObjectMirror cbFunc, DFTcpChannel channel) {
			this.cbFunc = cbFunc;
			this.channel = channel;
		}
	}
	
	//
	@Override
	public int newActor(Object template, Object name, Object param, Object initCfg) {
		return _mgrActorJs.createActor(template, name, param, initCfg);
	}
	@Override
	public int to(Object dst, int cmd, Object payload) {
		return _mgrActorJs.send(this.id, dst, cmd, payload);
	}
	@Override
	public int call(Object dst, int cmd, Object payload, Object cb) {
		if(!DFJsUtil.isJsFunction(cb)){
			return -1;
		}
		final ScriptObjectMirror mirCb = (ScriptObjectMirror) cb;
		final Cb cbWrap = new Cb() {
			@Override
			public int onFailed(int code) {
				mirCb.call(_js, code==0?"timeout":"call failed");
				return 0;
			}
			@Override
			public int onCallback(int cmd, Object payload) {
				mirCb.call(_js, null, cmd, payload);
				return 0;
			}
		};
		if(dst instanceof String){ //dst name
			return sys.call((String)dst, cmd, payload, cbWrap);
		}else{ //dst id
			int dstId = 0;
			if(dst instanceof Double){ dstId = ((Double)dst).intValue();
			}else{ dstId = (int) dst; }
			return sys.call(dstId, cmd, payload, cbWrap);
		}
	}
	@Override
	public int ret(int cmd, Object payload) {
		return sys.ret(cmd, payload);
	}
	@Override
	public int toNode(String dstNode, String dstActor, int cmd, Object payload) {
		if(payload != null){
			if(payload instanceof String){
				return sys.toNode(dstNode, dstActor, cmd, (String)payload);
			}else if(payload instanceof IScriptBuffer){
				return sys.toNode(dstNode, dstActor, cmd, ((DFJsBuffer)payload).getBuf());
			}else if(payload instanceof Builder){  //protobuf
				byte[] b = ((Builder<?>)payload).build().toByteArray();
				return sys.toNode(dstNode, dstActor, cmd, b);
			}else{  //unknown
				return -1;
			}
		}else{
			byte[] b = null;
			return sys.toNode(dstNode, dstActor, cmd, b);
		}
	}
	@Override
	public int toTypeNode(String type, String dstActor, int cmd, Object payload){
		if(payload != null){
			if(payload instanceof String){
				return sys.toNodeByType(type, dstActor, cmd, (String)payload);
			}else if(payload instanceof IScriptBuffer){
				return sys.toNodeByType(type, dstActor, cmd, ((DFJsBuffer)payload).getBuf());
			}else if(payload instanceof Builder){  //protobuf
				byte[] b = ((Builder<?>)payload).build().toByteArray();
				return sys.toNodeByType(type, dstActor, cmd, b);
			}else{  //unknown
				return -1;
			}
		}else{
			byte[] b = null;
			return sys.toNodeByType(type, dstActor, cmd, b);
		}
	}
	@Override
	public int toAllNode(String dstActor, int cmd, Object payload){
		if(payload != null){
			if(payload instanceof String){
				return sys.toAllNode(dstActor, cmd, (String)payload);
			}else if(payload instanceof IScriptBuffer){
				return sys.toAllNode(dstActor, cmd, ((DFJsBuffer)payload).getBuf());
			}else if(payload instanceof Builder){  //protobuf
				byte[] b = ((Builder<?>)payload).build().toByteArray();
				return sys.toAllNode(dstActor, cmd, b);
			}else{  //unknown
				return -1;
			}
		}else{
			byte[] b = null;
			return sys.toAllNode(dstActor, cmd, b);
		}
	}
	@Override
	public int rpcNode(String dstNode, String dstActor, String dstMethod, int cmd, Object payload, Object cb){
		try{
			final ScriptObjectMirror mirCb = (cb==null||ScriptObjectMirror.isUndefined(cb))?null:(ScriptObjectMirror)cb;
			RpcFuture future = null;
			if(payload != null){
				if(payload instanceof String){
					future = sys.rpcNode(dstNode, dstActor, dstMethod, cmd, (String)payload);
				}else if(payload instanceof IScriptBuffer){
					future = sys.rpcNode(dstNode, dstActor, dstMethod, cmd, ((DFJsBuffer)payload).getBuf());
				}else if(payload instanceof Builder){
					byte[] b = ((Builder<?>)payload).build().toByteArray();
					future = sys.rpcNode(dstNode, dstActor, dstMethod, cmd, b);
				}else{
					return -2;
				}
			}else{
				byte[] b = null;
				future = sys.rpcNode(dstNode, dstActor, dstMethod, cmd, b);
			}
			if(future != null && mirCb != null){
				future.addListener(new Cb() {
					@Override
					public int onFailed(int code) {
						mirCb.call(_js, code+"");
						return 0;
					}
					@Override
					public int onCallback(int cmd, Object payload) {
						mirCb.call(_js, null, cmd, payload);
						return 0;
					}
				}, 60000);
			}
			return 0;
		}catch(Throwable e){
			e.printStackTrace();
		}
		return -1;
	}
	
	@Override
	public void timeout(int delay, Object requestId) {
		if(requestId == null || ScriptObjectMirror.isUndefined(requestId)) timer.timeout(delay, 0);
		else  timer.timeout(delay, (Integer)requestId);
		
	}
	@Override
	public void logV(Object msg) {
		log.verb(msg.toString());
	}
	@Override
	public void logD(Object msg) {
		log.debug(msg.toString());
	}
	@Override
	public void logI(Object msg) {
		log.info(msg.toString());
	}
	@Override
	public void logW(Object msg) {
		log.warn(msg.toString());
	}
	@Override
	public void logE(Object msg) {
		log.error(msg.toString());
	}
	@Override
	public void logF(Object msg) {
		log.fatal(msg.toString());
	}

	@Override
	public boolean tcpSend(Integer channelId, Object msg) {
		if(_mapChJsFunc != null){
			JsTcpChannel ch = _mapChJsFunc.get(channelId);
			if(ch != null){ //online
				Object msgOut = msg;
				if(msg instanceof IScriptBuffer){  //trans to bytebuf
					msgOut = ((DFJsBuffer)msg).getBuf();
				}else if(msg instanceof Builder){  //protobuf builder
					byte[] bytes = ((Builder<?>)msg).build().toByteArray();
					int len = bytes.length;
					msgOut = PooledByteBufAllocator.DEFAULT.ioBuffer(len);
					((ByteBuf)msgOut).writeBytes(bytes);
				}
				return ch.channel.write(msgOut)==0?true:false;
			}
		}
		return false;
	}
	@Override
	public void tcpChange(Integer channelId, Object msgHandler, Object statusHandler) {
		if(_mapChJsFunc != null){
			JsTcpChannel ch = _mapChJsFunc.get(channelId);
			if(ch != null){ //online
				if(msgHandler != null){
					int actorId = 0;
					if(msgHandler instanceof Integer) actorId = (Integer)msgHandler;
					else if(msgHandler instanceof String) actorId = DFActorManager.get().getActorIdByName((String)msgHandler);
					if(actorId > 0) ch.channel.setMessageActor(actorId);
				}
				if(statusHandler != null){
					int actorId = 0;
					if(statusHandler instanceof Integer) actorId = (Integer)statusHandler;
					else if(statusHandler instanceof String) actorId = DFActorManager.get().getActorIdByName((String)statusHandler);
					if(actorId > 0) ch.channel.setStatusActor(actorId);
				}
			}
		}
	}

	@Override
	public String bufToStr(Object buf) {
		if(buf instanceof IScriptBuffer){
			ByteBuf b = ((DFJsBuffer)buf).getBuf();
			return (String) b.readCharSequence(b.readableBytes(), CharsetUtil.UTF_8);
		}else if(buf instanceof String){
			return (String) buf;
		}else if(buf instanceof ByteBuf){
			ByteBuf b = (ByteBuf) buf;
			return (String) b.readCharSequence(b.readableBytes(), CharsetUtil.UTF_8);
		}
		return null;
	}

	@Override
	public IScriptBuffer strToBuf(String src) {
		try{
			byte[] bytes = src.getBytes(CharsetUtil.UTF_8);
			ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(bytes.length);
			buf.writeBytes(bytes);
			return DFJsBuffer.newBuffer(buf);
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void exit() {
		this.exit();
	}

	@Override
	public int rpc(Object dstActor, String dstMethod, int cmd, Object payload, Object cb) {
		try{
			if(cb != null && ScriptObjectMirror.isUndefined(cb)){
				cb = null;
			}
			final ScriptObjectMirror mirCb = cb==null?null:(ScriptObjectMirror)cb;
			final Cb cbWrap = mirCb==null?null:new Cb() {
				@Override
				public int onFailed(int code) {
					mirCb.call(_js, code==0?"timeout":"rpc failed");
					return 0;
				}
				@Override
				public int onCallback(int cmd, Object payload) {
					mirCb.call(_js, null, cmd, payload);
					return 0;
				}
			};
			int ret = 0;
			RpcFuture future = null;
			if(dstActor instanceof String){
				future = sys.rpc((String)dstActor, dstMethod, cmd, payload);
			}else{
				int dstId = 0;
				if(dstActor instanceof Double){
					dstId = ((Double)dstActor).intValue();
				}else{
					dstId = (Integer)dstActor;
				}
				future = sys.rpc(dstId, dstMethod, cmd, payload);
			}
			if(!future.isSendSucc()){
				ret = 1;
			}else if(cbWrap != null){
				future.addListener(cbWrap, 60000);
			}
			return ret;
		}catch(Throwable e){
			e.printStackTrace(); 
			return 1;
		}
	}
	@Override
	public DFHttpCliReq newHttpReq() {
		return DFHttpReqBuilder.build();
	}
	@Override
	public void httpCli(Object cfg, Object cb) {
		try{
			ScriptObjectMirror mirCfg = (ScriptObjectMirror) cfg;
			int port = (Integer)mirCfg.get("port");
			String host = (String) mirCfg.get("host");
			DFHttpCliReq req = (DFHttpCliReq) mirCfg.get("req");
			int connTimeout = mirCfg.containsKey("timeout")?(Integer)(mirCfg.get("timeout")):0;
			final ScriptObjectMirror mirCb = DFJsUtil.isJsFunction(cb)?(ScriptObjectMirror) cb:null;
			//
			req.end();
			DFTcpClientCfg cfgCli = DFTcpClientCfg.newCfg(host, port)
					.setReqData(req);
			if(connTimeout > 0){
				cfgCli.setConnTimeout(connTimeout);
			}	
			Object objSsl = mirCfg.get("ssl");
			if(objSsl != null && !ScriptObjectMirror.isUndefined(objSsl)){ //has ssl config
				if((Boolean)objSsl){
					cfgCli.setSslCfg(DFSSLConfig.newCfg());
				}
			}
			net.httpCli(cfgCli, mirCb==null?null:new CbHttpClient() {
				@Override
				public int onHttpResponse(Object msg, boolean isSucc, String errMsg) {
					if(mirCb != null){
						if(isSucc){
							DFHttpCliRsp rsp = (DFHttpCliRsp) msg;
							mirCb.call(_js, null, rsp);
						}else{
							mirCb.call(_js, errMsg, null);
						}
					}
					return 0;
				}
			});
		}catch(Throwable e){
			e.printStackTrace();
		}
	}
	
	private static int[] s_arrWebActor = null;
	private static final String WEB_ACTOR_LOCK = "wIOQ%&^BBkKLBKJ127uu";
	@Override
	public boolean httpSvr(Object cfg, Object cb) {
		try{
			ScriptObjectMirror mirCfg = (ScriptObjectMirror) cfg;
			int port = (Integer)mirCfg.get("port");
			ScriptObjectMirror mirCb = null;
			if(cb != null && !ScriptObjectMirror.isUndefined(cb)){
				mirCb = (ScriptObjectMirror) cb;
				if(!mirCb.isFunction()){
					mirCb = null;
				}
			}
			DFTcpServerCfg cfgSvr = null;
			if(mirCfg.containsKey("worker") && mirCfg.containsKey("boss")){
				cfgSvr = new DFTcpServerCfg(port, (Integer)(mirCfg.get("worker")), (Integer)(mirCfg.get("boss")));
			}else{
				cfgSvr = new DFTcpServerCfg(port);
			}
			Object obj = mirCfg.get("ssl");
			if(obj != null && !ScriptObjectMirror.isUndefined(obj)){ //has ssl config
				ScriptObjectMirror mirSslCfg = (ScriptObjectMirror) obj;
				DFSSLConfig cfgSsl = DFSSLConfig.newCfg()
							.certPath((String)mirSslCfg.get("cert"))
							.pemPath((String)mirSslCfg.get("key"));
				cfgSvr.setSslConfig(cfgSsl);
			}
			//检测是否web类型
			obj = mirCfg.get("web");
			final boolean isWeb = obj==null?false:true;
			final boolean isMulti;
			final int webThNum;
			final int[] arrWebActor;
			final AtomicInteger webReqCount = new AtomicInteger(0);
			final DFVirtualHost vHost;
			if(isWeb){
				final String strInitJs = _loadWebInitJs(_mgrActorJs.getAbsRunDir()
						+File.separator+"script"+File.separator+"Init_web.js");
				DFVirtualHost tmpVHost = new DFVirtualHost(port, obj, _mgrActorJs.getAbsRunDir(), strInitJs);
				if(!tmpVHost.init(log)){
					return false;
				}
				vHost = tmpVHost;
				//check web thread status
				synchronized (WEB_ACTOR_LOCK) {
					int logicThNum = 0, blockThNum = 0;
					blockThNum = DFActorManager.get().getBlockThreadNum();
					logicThNum = DFActorManager.get().getLogicThreadNum();
					webThNum = blockThNum + logicThNum;
					if(s_arrWebActor == null){
						s_arrWebActor = new int[webThNum];
						for(int i=0; i<webThNum; ++i){
							ActorProp prop = ActorProp.newProp()
									.classz(DFJsWebActor.class)
									.consumeType(DFActorDefine.CONSUME_ALL)
									.blockActor(i<logicThNum?false:true);
							HashMap<String,String> mapParam = new HashMap<>();
//							mapParam.put("webRoot", vHost.getWebRoot());
//							mapParam.put("sfx", vHost.getSfx());
							mapParam.put("initJs", strInitJs);
							prop.param(mapParam);
							s_arrWebActor[i] = sys.createActor(prop);
						}
					}
					arrWebActor = s_arrWebActor;
				}
				isMulti = false;
			}else{
				vHost = null;
				webThNum = 0;
				arrWebActor = null;
				obj = mirCfg.get("multi");
				isMulti = obj==null?false:true;
			}
			cfgSvr.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP);
			final ScriptObjectMirror jsCb = mirCb;
			net.httpSvr(cfgSvr, new CbHttpServer() {
				@Override
				public void onListenResult(boolean isSucc, String errMsg) {
					if(jsCb != null){
						_jsEvent.type = "ret";
						if(isSucc){  
							_jsEvent.succ = true;
							if(isWeb){
								vHost.start();
								DFVirtualHostManager.get().addHost(vHost);
							}
						}else{ 
							_jsEvent.succ = false; _jsEvent.err = errMsg; 
						}
						jsCb.call(_js, _jsEvent);
					}
				}
				@Override
				public int onHttpRequest(Object msg) {
					if(jsCb != null){
						DFHttpSvrReq req = (DFHttpSvrReq) msg;
						DFJsHttpSvrReq reqWrap = new DFJsHttpSvrReq(req);
						_jsEvent.type = "req";
						jsCb.call(_js, _jsEvent, (IScriptHttpSvrReq)reqWrap);
					}
					return 0;
				}
			}, new DFHttpDispatcher() {
				@Override
				public int onQueryMsgActorId(int port, InetSocketAddress addrRemote, Object msg) {
					if(isWeb){
						return arrWebActor[webReqCount.incrementAndGet()%webThNum];
					}else if(isMulti && jsCb != null){
						DFJsHttpSvrReq reqWrap = new DFJsHttpSvrReq((DFHttpSvrReq)msg);
						_jsEvent.type = "multi";
						return (Integer)jsCb.call(_js, _jsEvent, (IScriptHttpSvrReq)reqWrap);
					}
					return id;
				}
			});
		}catch(Throwable e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean isNode() {
		return sys.isClusterEnable();
	}

	@Override
	public String getNodeName() {
		return DFActorManager.get().getNodeName();
	}

	@Override
	public String getNodeType() {
		return DFActorManager.get().getNodeType();
	}

	@Override
	public int listenNode(String type, Object val, Object cb) {
		try{
			Object objCb = null;
			String strVal = null;
			if(type.equalsIgnoreCase("all")){
				objCb = val;
			}else if(type.equalsIgnoreCase("type")){
				objCb = cb;
				strVal = (String) val;
			}else if(type.equalsIgnoreCase("name")){
				strVal = (String) val;
				objCb = cb;
			}else{
				return 2;
			}
			if(objCb == null || ScriptObjectMirror.isUndefined(objCb)){
				return 3;
			}
			final ScriptObjectMirror mirCb = (ScriptObjectMirror) objCb;
			if(!mirCb.isFunction()){
				return 4;
			}
			CbNode cbWrap = new CbNode() {
				@Override
				public void onNodeRemove(DFNode node) {
					mirCb.call(_js, "off", node);
				}
				@Override
				public void onNodeAdd(DFNode node) {
					mirCb.call(_js, "on", node);
				}
			};
			if(type.equalsIgnoreCase("all")){
				sys.listenNodeAll(cbWrap);
			}else if(type.equalsIgnoreCase("type")){
				sys.listenNodeByType(strVal, cbWrap);
			}else if(type.equalsIgnoreCase("name")){
				sys.listenNodeByName(strVal, cbWrap);
			}
			return 0;
		}catch(Throwable e){
			e.printStackTrace();
		}
		return 1;
	}

	@Override
	public boolean isNodeOnline(String nodeName) {
		return sys.isNodeOnline(nodeName);
	}

	@Override
	public int cpuNum() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public int mysqlInitPool(Object cfg) {
		try{
			ScriptObjectMirror mirCfg = (ScriptObjectMirror) cfg;
			DFDbCfg dbCfg = DFDbCfg.newCfg(
					(String)mirCfg.get("host"), (Integer)mirCfg.get("port"), 
					(String)mirCfg.get("db"), (String)mirCfg.get("user"), (String)mirCfg.get("pwd"));
			if(mirCfg.containsKey("initSize")) dbCfg.setInitSize((Integer)mirCfg.get("initSize"));
			if(mirCfg.containsKey("maxActive")) dbCfg.setMaxActive((Integer)mirCfg.get("maxActive"));
			if(mirCfg.containsKey("minIdle")) dbCfg.setMinIdle((Integer)mirCfg.get("minIdle"));
			if(mirCfg.containsKey("maxIdle")) dbCfg.setMaxIdle((Integer)mirCfg.get("maxIdle"));
			int dbId = db.initPool(dbCfg);
			return dbId;
		}catch(Throwable e){
			e.printStackTrace();
		}
		return 0;
	}
	@Override
	public int mysqlGetConn(int poolId, Object cb) {
		Connection conn = null;
		try{
			conn = db.getConn(poolId);
			ScriptObjectMirror mirCb = (ScriptObjectMirror) cb;
			if(conn == null){ //error
				mirCb.call(_js, "no conn available");
			}else{ //succ
				mirCb.call(_js, null, conn);
			}
		}catch(Throwable e){
			e.printStackTrace();
		}finally{
			if(conn != null){
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return -1;
	}

	@Override
	public int redisInitPool(Object cfg) {
		try{
			ScriptObjectMirror mirCfg = (ScriptObjectMirror) cfg;
			DFRedisCfg cfgRedis = DFRedisCfg.newCfg((String)mirCfg.get("host"), (Integer)mirCfg.get("port"), 
						(String)mirCfg.get("auth"));
			if(mirCfg.containsKey("maxTotal")) cfgRedis.setMaxTotal((Integer)mirCfg.get("maxTotal"));
			if(mirCfg.containsKey("minIdle")) cfgRedis.setMinIdle((Integer)mirCfg.get("minIdle"));
			if(mirCfg.containsKey("maxIdle")) cfgRedis.setMaxIdle((Integer)mirCfg.get("maxIdle"));
			int redisId = redis.initPool(cfgRedis);
			return redisId;
		}catch(Throwable e){
			e.printStackTrace();
		}
		return 0;
	}
	@Override
	public int redisGetConn(int poolId, Object cb) {
		Jedis conn = null;
		try{
			conn = redis.getConn(poolId);
			ScriptObjectMirror mirCb = (ScriptObjectMirror) cb;
			if(conn == null){ //error
				mirCb.call(_js, "no conn available");
			}else{ //succ
				mirCb.call(_js, null, conn);
			}
		}catch(Throwable e){
			e.printStackTrace();
		}finally{
			if(conn != null){
				conn.close();
			}
		}
		return -1;
	}

	@Override
	public int mongoInitPool(Object cfg) {
		try{
			ScriptObjectMirror mirCfg = (ScriptObjectMirror) cfg;
			DFMongoCfg cfgMongo = DFMongoCfg.newCfg()
					.addAddress((String)mirCfg.get("host"),    //mongodb host
							(Integer)mirCfg.get("port")); //mongodb port
			if(mirCfg.containsKey("auth")){
				ScriptObjectMirror mirAuth = (ScriptObjectMirror) mirCfg.get("auth");
				cfgMongo.setCredential((String)mirAuth.get("db"), (String)mirAuth.get("user"), 
						(String)mirAuth.get("pwd"));
			}
			int poolId = mongo.initPool(cfgMongo);
			return poolId;
		}catch(Throwable e){
			e.printStackTrace();
		}
		return 0;
	}
	@Override
	public int mongoGetDb(int poolId, String dbName, Object cb) {
		MongoDatabase db = null;
		try{
			db = mongo.getDatabase(poolId, dbName);
			ScriptObjectMirror mirCb = (ScriptObjectMirror) cb;
			if(db == null){ //error
				mirCb.call(_js, "no db available");
			}else{ //succ
				mirCb.call(_js, null, db);
			}
		}catch(Throwable e){
			e.printStackTrace();
		}finally{
			
		}
		return -1;
	}

	@Override
	public Object objByName(String name) {
		try {
			Class<?> clz = _mgrActorJs.getExtLibClass(name);
			if(clz != null){
				Constructor<?> ctor = clz.getDeclaredConstructor();
				ctor.setAccessible(true);
				return ctor.newInstance();
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object callObjMethod(Object obj, String method, Object... param) {
		Class<?> clz = obj.getClass();
		try {
			int size = param.length;
			if(size==1 && ScriptObjectMirror.isUndefined(param[0])){ //no param
				Method m = clz.getDeclaredMethod(method);
				return m.invoke(obj);
			}else{
				Class<?>[] arrPType = new Class[size];
				for(int i=0; i<size; ++i){
					Class<?> c = param[i].getClass();
					String n = c.getSimpleName();
					if(n.equals("Integer")){
						c = int.class;
					}else if(n.equals("Boolean")){
						c = boolean.class;
					}
					arrPType[i] = c;
				}
				Method m = clz.getDeclaredMethod(method, arrPType);
				return m.invoke(obj, param);
			}
		} catch (IllegalArgumentException | NoSuchMethodException | SecurityException 
				| IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String _loadWebInitJs(String strDir){
		File dir = new File(strDir);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(dir));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while( (line=br.readLine()) != null ){
				sb.append(line).append("\n");
			}
			return sb.toString();
		}catch(Throwable e){
			e.printStackTrace();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}

