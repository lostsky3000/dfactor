package fun.lib.actor.core;

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
import fun.lib.actor.api.cb.CbActorReq;
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
		// TODO Auto-generated constructor stub
	}
	private DFActorManagerJs _mgrActorJs = null;
	private ScriptObjectMirror _js = null;
	
	private boolean _regOnStart = true;
	private boolean _regOnMessage = true;
	private boolean _regOnTimeout = true;
	private boolean _regOnSchedule = true;
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
		if(!_checkFunction("onMessage")) _regOnMessage = false;
		if(!_checkFunction("onTimeout")) _regOnTimeout = false;
		if(!_checkFunction("onSchedule")) _regOnSchedule = false;
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
	public int onMessage(int srcId, int cmd, Object payload, CbActorReq cb) {
		if(_regOnMessage){
			_lastSrcId = srcId;
			_js.callMember("onMessage", cmd, payload, srcId);
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
			_js.callMember("onSchedule", dltMilli);
		}
	}
	
	private DFJsEvent _jsEvent = null;
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
		ScriptObjectMirror cb = _mapTcpCliJsFunc.get(requestId);
		if(cb != null){ //has cb
			if(!isSucc){
				_mapTcpCliJsFunc.remove(requestId);
			}
			_jsEvent.type = "ret"; _jsEvent.succ = isSucc; _jsEvent.err = errMsg;
			cb.call(0, _js, _jsEvent);
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
		int chId = channel.getChannelId();
		JsTcpChannel chWrap = _mapChJsFunc.remove(chId);
		if(chWrap != null){ //notify js
			_jsEvent.type = "close";
			chWrap.cbFunc.call(0, _js, _jsEvent, chId);
		}
	}
	@Override
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
		int chId = channel.getChannelId();
		JsTcpChannel chWrap = _mapChJsFunc.get(chId);
		if(chWrap != null){ //notify js
			_jsEvent.type = "msg";
			Object msgOut = msg;
			if(msg instanceof ByteBuf){
				msgOut = DFJsBuffer.newBuffer((ByteBuf)msg);
			}
			chWrap.cbFunc.call(0, _js, _jsEvent, chId, msgOut);
		}
		return 0;
	}
	//
	private HashMap<Integer, ScriptObjectMirror> _mapTcpCliJsFunc = null;
	private HashMap<Integer, ScriptObjectMirror> _mapTcpSvrJsFunc = null;
	private HashMap<Integer, JsTcpChannel> _mapChJsFunc = null;
	@Override
	public boolean doTcpServer(Object cfg, Object func) {
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
	public boolean doTcpConnect(Object cfg, Object func) {
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
			int len = bytes.length;
			ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(len);
			buf.writeBytes(bytes, 0, len);
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
	public int send(Object dst, int cmd, Object payload) {
		return _mgrActorJs.send(this.id, dst, cmd, payload);
	}
	@Override
	public int ret(int cmd, Object payload) {
		return _mgrActorJs.send(this.id, _lastSrcId, cmd, payload);
	}
	@Override
	public void timeout(int delay, int requestId) {
		timer.timeout(delay, requestId);
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
	public String bufToString(Object buf) {
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

	
}

