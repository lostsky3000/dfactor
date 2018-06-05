package fun.lib.actor.core;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.funtag.util.script.DFJsUtil;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.GeneratedMessageV3.Builder;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.api.http.DFHttpCliReq;
import fun.lib.actor.api.http.DFHttpCliRsp;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

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
	//
	private int _lastSrcId = 0;
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
	
	private Object _curUserHandler = null;
	private int _curReqId = 0;
	@Override
	public int onMessage(int cmd, Object payload, int srcId) {
		if(_regOnMessage){
			_lastSrcId = srcId;
			_curUserHandler = null;
			_curReqId = 0;
			_js.callMember("onMsg", cmd, payload, srcId);
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
			cb.call(0, _js, _jsEvent);
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
				cb.call(0, _js, _jsEvent);
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
			cb.call(0, _js, _jsEvent, chId); //notify js
		}
	}
	@Override
	public void onTcpConnClose(int requestId, DFTcpChannel channel) {	
		if(_mapChJsFunc == null) _mapChJsFunc = new HashMap<>();
		int chId = channel.getChannelId();
		JsTcpChannel chWrap = _mapChJsFunc.remove(chId);
		if(chWrap != null){ //notify js
			_jsEvent.type = "close";
			chWrap.cbFunc.call(0, _js, _jsEvent, chId);
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
		}
		if(chWrap != null){ //notify js
			_jsEvent.type = "msg";
			chWrap.cbFunc.call(0, _js, _jsEvent, chId, msgOut);
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
				websocket = obj==null?websocket:(Boolean)obj;
				log.info("start try tcpSvr, port="+port+", worker="+workerTh+", boss="+bossTh+", isWebsocket="+websocket);
				DFTcpServerCfg svrCfg = new DFTcpServerCfg(port, workerTh, bossTh)
						.setTcpProtocol(websocket?DFActorDefine.TCP_DECODE_WEBSOCKET:DFActorDefine.TCP_DECODE_LENGTH);
				net.doTcpServer(svrCfg);
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
				DFTcpClientCfg cfgCli = new DFTcpClientCfg(host, port)
						.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
				Object obj = cfgWrap.get("timeout");
				if(obj != null) cfgCli.setConnTimeout((Integer)obj);
				net.doTcpConnect(cfgCli, reqId);
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
			funcWrap.call(0, _js);
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
			funcWrap.call(0, _js);
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
				_jsEvent.type = "err";
				_jsEvent.err = code + "";
				mirCb.call(0, _js, _jsEvent);
				return 0;
			}
			@Override
			public int onCallback(int cmd, Object payload) {
				_jsEvent.type = "rsp";
				mirCb.call(0, _js, _jsEvent, cmd, payload);
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
					_jsEvent.type = "err"; _jsEvent.err = code+"";
					mirCb.call(0, _js, _jsEvent);
					return 0;
				}
				@Override
				public int onCallback(int cmd, Object payload) {
					_jsEvent.type = "rsp";
					mirCb.call(0, _js, _jsEvent, cmd, payload);
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
			DFTcpClientCfg cfgCli = DFTcpClientCfg.newCfg(host, port)
					.setReqData(req);
			if(connTimeout > 0){
				cfgCli.setConnTimeout(connTimeout);
			}	
			net.doHttpClient(cfgCli, mirCb==null?null:new CbHttpClient() {
				@Override
				public int onHttpResponse(Object msg, boolean isSucc, String errMsg) {
					if(mirCb != null){
						DFHttpCliRsp rsp = (DFHttpCliRsp) msg;
						if(isSucc){
							_jsEvent.type = "rsp";
						}else{
							_jsEvent.type = "ret"; _jsEvent.succ = false; _jsEvent.err = errMsg;
						}
						mirCb.call(0, _js, _jsEvent, rsp);
					}
					return 0;
				}
			});
		}catch(Throwable e){
			e.printStackTrace();
		}
	}
	@Override
	public void httpSvr(Object cfg, Object cb) {
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
			cfgSvr.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP);
			final ScriptObjectMirror jsCb = mirCb;
			net.doHttpServer(cfgSvr, new CbHttpServer() {
				@Override
				public void onListenResult(boolean isSucc, String errMsg) {
					if(jsCb != null){
						_jsEvent.type = "ret";
						if(isSucc){  _jsEvent.succ = true;
						}else{ _jsEvent.succ = false; _jsEvent.err = errMsg; }
						jsCb.call(0, _js, _jsEvent);
					}
				}
				@Override
				public int onHttpRequest(Object msg) {
					if(jsCb != null){
						DFHttpSvrReq req = (DFHttpSvrReq) msg;
						DFJsHttpSvrReq reqWrap = new DFJsHttpSvrReq(req);
						_jsEvent.type = "req";
						jsCb.call(0, _js, _jsEvent, (IScriptHttpSvrReq)reqWrap);
					}
					return 0;
				}
			}, new DFHttpDispatcher() {
				@Override
				public int onQueryMsgActorId(int port, InetSocketAddress addrRemote, Object msg) {
					return id;
				}
			});
		}catch(Throwable e){
			e.printStackTrace();
		}
	}
	
	
	
}

