package fun.lib.actor.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.concurrent.locks.StampedLock;

import com.funtag.util.log.DFLogFactory;
import com.funtag.util.log.DFLogger;
import com.funtag.util.system.DFSysUtil;
import com.funtag.util.timer.DFHashWheelTimer;
import com.funtag.util.timer.DFScheduleTick;
import com.funtag.util.timer.DFTimeout;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.helper.ActorLog;
import fun.lib.actor.helper.DFActorLogLevel;
import fun.lib.actor.po.DFTcpClientCfg;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public final class DFActorManager {

	
	private static final long TIMER_TICK_DURATION = 10;
	private static final long TIMER_REAL_DURATION_MILLI = 10;
	private static final int SCHEDULE_TICK_INIT_RANGE = (int) (1000/TIMER_TICK_DURATION);
	
	private static final DFActorManager instance = new DFActorManager();
	
	private final DFLogger log = DFLogFactory.create(DFActorManager.class);
	
	private DFActorManager() {
		// TODO Auto-generated constructor stub
	}
	
	public static DFActorManager get(){
		return instance;
	}
	
	private void _debugLog(String msg){
		System.out.println(msg);
	}
	
	private volatile boolean _hasStarted = false;
	private volatile List<LoopWorker> _lsLoopWorker = null;
	//
	
	private volatile List<DFHashWheelTimer> _lsTimer = null;
	private volatile List<LoopTimer> _lsLoopTimer = null;
	private volatile CountDownLatch _cdWorkerStop = null;
	private volatile int _timerThNum = 1;
	private final AtomicInteger _timerIdxCount = new AtomicInteger(0);
	//
	private LinkedBlockingQueue<DFActorWrapper> _queueGlobalActor = new LinkedBlockingQueue<>();
	private LinkedBlockingQueue<DFActorWrapper> _queueGlobalBlockActor = new LinkedBlockingQueue<>();
	//
	private volatile CountDownLatch _cdInit = null;
	private volatile DFActorManagerConfig _initCfg = null;
	//
	private volatile String _entryName = null;
	private volatile Class<? extends DFActor> _entryClassz = null;
	private volatile Object _entryParam = null;
	private volatile int _entryScheduleUnit = 0;
	private volatile int _entryConsumeType = DFActorDefine.CONSUME_AUTO;
	private volatile EventLoopGroup _clientIoGroup = null;
	
	private int logLevel = DFActorLogLevel.DEBUG;
	public int getLogLevel(){
		return logLevel;
	}
	
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param entryName 入口actor名字(全局唯一)
	 * @param entryClassz 入口actor class
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, String entryName, Class<? extends DFActor> entryClassz){
		return start(cfg, entryName, entryClassz, null, 0, DFActorDefine.CONSUME_AUTO);
	}
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param entryName 入口actor名字(全局唯一)
	 * @param entryClassz 入口actor class
	 * @param entryParam 入口actor传入参数
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, String entryName, Class<? extends DFActor> entryClassz,
			Object entryParam){
		return start(cfg, entryName, entryClassz, entryParam, 0, DFActorDefine.CONSUME_AUTO);
	}
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param entryName 入口actor名字(全局唯一)
	 * @param entryClassz 入口actor class
	 * @param entryParam 入口actor传入参数
	 * @param entryScheduleUnit schedule周期
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, String entryName, Class<? extends DFActor> entryClassz,
			Object entryParam, int entryScheduleUnit){
		return start(cfg, entryName, entryClassz, entryParam, entryScheduleUnit, DFActorDefine.CONSUME_AUTO);
	}
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param entryName 入口actor名字(全局唯一)
	 * @param entryClassz 入口actor class
	 * @param entryParam 入口actor传入参数
	 * @param entryScheduleUnit schedule周期
	 * @param entryConsumeType 消息消费策略
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, String entryName, 
			Class<? extends DFActor> entryClassz, Object entryParam, 
			int entryScheduleUnit, int entryConsumeType){
		boolean bRet = false;
		do {
			DFLogFactory.setLogLevel(cfg.getLogLevel());
			logLevel = cfg.getLogLevel();
			if(_hasStarted){
				break;
			}
			_dumpInitInfo();
			//
			_entryName = entryName;
			_entryClassz = entryClassz;
			_entryParam = entryParam;
			_entryScheduleUnit = entryScheduleUnit;
			if(entryConsumeType>=DFActorDefine.CONSUME_AUTO &&
					entryConsumeType<=DFActorDefine.CONSUME_ALL){
				_entryConsumeType = entryConsumeType;
			}
			_initCfg = cfg;
			if(cfg.getClientIoThreadNum() > 0){
				if(DFSysUtil.isLinux()){
					_clientIoGroup = new EpollEventLoopGroup(cfg.getClientIoThreadNum());
				}else{
					_clientIoGroup = new NioEventLoopGroup(cfg.getClientIoThreadNum());
				}
			}
			//
			int logicWorkerThNum = cfg.getLogicWorkerThreadNum();
			int blockWorkerThNum = cfg.getBlockWorkerThreadNum();
			_timerThNum = cfg.getTimerThreadNum();
			_cdInit = new CountDownLatch(logicWorkerThNum + blockWorkerThNum + _timerThNum); //worker + timer
			_cdWorkerStop = new CountDownLatch(logicWorkerThNum + blockWorkerThNum + _timerThNum);
			//start timer thread
			_lsTimer = new ArrayList<>(_timerThNum);
			_lsLoopTimer = new ArrayList<>(_timerThNum);
			for(int i=0; i<_timerThNum; ++i){
				final DFHashWheelTimer timer = new DFHashWheelTimer(TIMER_TICK_DURATION, 
						cfg.getTimerTickPerWheel(),
						SCHEDULE_TICK_INIT_RANGE);
				_lsTimer.add(timer);
				final LoopTimer loop = new LoopTimer(timer);
				_lsLoopTimer.add(loop);
				final Thread th = new Thread(loop);
				th.setName("thread-timer-"+i);
				th.setPriority(Thread.MAX_PRIORITY);
				th.start();
			}
			//start worker thread
			_lsLoopWorker = new ArrayList<>(logicWorkerThNum + blockWorkerThNum);
			//logic worker thread
			final Thread[] arrWorkerTh = new Thread[logicWorkerThNum + blockWorkerThNum];
			for(int i=0; i<arrWorkerTh.length; ++i){
				LoopWorker loop = null;
				Thread th = null;
				if(i < logicWorkerThNum){  //logic worker thread
					loop = new LoopWorker(i+1, i==0?true:false, true);
					th = new Thread(loop);
					th.setName("thread-logic-worker-"+i);
				}else{   //io worker thread
					loop = new LoopWorker(i+1, false, false);
					th = new Thread(loop);
					th.setName("thread-io-worker-"+(i-logicWorkerThNum));
				}
				_lsLoopWorker.add(loop);
				arrWorkerTh[i] = th;
			}
			for(int i=0; i<arrWorkerTh.length; ++i){
				arrWorkerTh[i].start();
			}
			try {
				_cdInit.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//
			_hasStarted = true;
			bRet = true;
		} while (false);
		
		return bRet;
	}
	
	private void _dumpInitInfo(){
		log.I("OS="+DFSysUtil.getOSType()+" (linux="+DFSysUtil.OS_LINUX 
				+" win="+DFSysUtil.OS_WINDOWS+" mac="+DFSysUtil.OS_MAC
				+" unknown="+DFSysUtil.OS_UNKNOWN+")");
	}
	
	private volatile boolean _onShutdown = false;
	public void shutdown(){
		if(_onShutdown){
			return ;
		}
		_onShutdown = true;
		final ExecutorService svcShutdown = Executors.newFixedThreadPool(1);
		svcShutdown.submit(new Runnable() {
			@Override
			public void run() {
				//close all listen socket
				DFSocketManager.get().doTcpListenCloseAll();
				//
				if(_cdWorkerStop != null){
					//stop timer thread
					for(LoopTimer t : _lsLoopTimer){
						t.stop();
					}
					_lsLoopTimer.clear(); _lsLoopTimer = null;
					//stop worker thread 
					for(LoopWorker w : _lsLoopWorker){
						w.stop();
					}
					_lsLoopWorker.clear(); _lsLoopWorker = null;
					//
					try {
						_cdWorkerStop.await(30000, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					_cdWorkerStop = null;
				}
				if(_clientIoGroup != null){
					_clientIoGroup.shutdownGracefully();
					_clientIoGroup = null;
				}
				//
				svcShutdown.shutdown();
			}
		});
	}
	
	protected int doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, srcActorId, _clientIoGroup, requestId);
	}
	protected int doTcpConnect(final DFTcpClientCfg cfg, final DFActorTcpDispatcher dispatcher, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, dispatcher, _clientIoGroup, requestId);
	}
	//
	private Map<Integer, DFActorWrapper> _mapActor = new HashMap<>();
	private Map<String, DFActorWrapper> _mapActorName = new HashMap<>();
	//
	private final ReentrantReadWriteLock _lockMapActor = new ReentrantReadWriteLock();
	private final ReadLock _lockActorRead = _lockMapActor.readLock();
	private final WriteLock _lockActorWrite = _lockMapActor.writeLock();
	
	private int _actorNum = 0;
	private int _idCount = DFActorDefine.ACTOR_ID_APP_BEGIN;
	private final String _idLock = "_idLockLostskysadfihui23234#@#$";
	
	protected int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType, boolean isBlockActor){
		DFActor actor = null;
		int id = _getSysActorIdByName(name);
		if(id < 1){  //not sys actor
			synchronized (_idLock) { //gen id
				id = _idCount;
				if(++_idCount >= Integer.MAX_VALUE){
					_idCount = DFActorDefine.ACTOR_ID_APP_BEGIN;
				}
			}
		}
		try {
			Class[] paramsType = {Integer.class, String.class, Integer.class, Boolean.class};
			Object[] params = {new Integer(id), name, new Integer(consumeType), new Boolean(isBlockActor)};
			Constructor<? extends DFActor> ctor = classz.getDeclaredConstructor(paramsType);
			ctor.setAccessible(true);
			actor = ctor.newInstance(params);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException 
				| SecurityException | IllegalArgumentException | InvocationTargetException e1) {
			e1.printStackTrace();
			return -2;
		}
		final DFActorWrapper wrapper = new DFActorWrapper(actor);
		//
		_lockActorWrite.lock();
		try{
			if(_mapActorName.containsKey(name)){ //name duplicated
				return -3;
			}
			_mapActorName.put(name, wrapper);
			_mapActor.put(id, wrapper);
			++_actorNum;
		}finally{
			_lockActorWrite.unlock();
		}
		//call actor start
		try{
			actor.onStart(param);
		}catch(Throwable e){
			e.printStackTrace();
		}
		if(scheduleUnit > 0){ //need schedule
			final int idxTimer = Math.abs(_timerIdxCount.incrementAndGet())%_timerThNum;
			_lsTimer.get(idxTimer).addSchedule(scheduleUnit*DFActor.TIMER_UNIT_MILLI, 
					new DFActorScheduleWrapper(wrapper.getActorId()), getTimerNowNano());
		}
		return id;
	}
	private int _getSysActorIdByName(final String name){
		final Integer curId = s_mapSysActorNameId.get(name);
		if(curId == null){
			return 0;
		}
		return curId;
	}
	
	protected int removeActor(int id){
		DFActorWrapper wrap = null;
		//
		_lockActorWrite.lock();
		try{
			wrap = _mapActor.remove(id);
			if(wrap != null){
				_mapActorName.remove(wrap.getActorName());
				--_actorNum;
			}
		}finally{
			_lockActorWrite.unlock();
		}
		//
		if(wrap != null){
			wrap.markRemoved();
		}
		return wrap==null?1:0;
	}
	public int getActorNum(){
		_lockActorRead.lock();
		try{
			return _actorNum;
		}finally{
			_lockActorRead.unlock();
		}
	}
	
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, final boolean addTail){
		return send(srcId, dstId, requestId, subject, cmd, payload, addTail, null);
	}
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, final boolean addTail, Object context){
		DFActorWrapper wrap = null;
		//
		_lockActorRead.lock();
		try{
			wrap = _mapActor.get(dstId);
		}finally{
			_lockActorRead.unlock();
		}
		if(wrap != null){
			if(wrap.pushMsg(srcId, requestId, subject, cmd, payload, context, addTail) == 0){ //add to global queue
				if(wrap.isLogicActor()){
					_queueGlobalActor.offer(wrap);
				}else{
					_queueGlobalBlockActor.offer(wrap);
				}
			} 
			return 0;
		}
		return 1;
	}
	protected int send(int srcId, String dstName, int requestId, 
			int subject, int cmd, Object payload, final boolean addTail, Object context){
		DFActorWrapper wrap = null;
		_lockActorRead.lock();
		try{
			wrap = _mapActorName.get(dstName);
		}finally{
			_lockActorRead.unlock();
		}
		if(wrap != null){
			if(wrap.pushMsg(srcId, requestId, subject, cmd, payload, context, addTail) == 0){ //add to global queue
				if(wrap.isLogicActor()){
					_queueGlobalActor.offer(wrap);
				}else{
					_queueGlobalBlockActor.offer(wrap);
				}
			}
			return 0;
		}
		return 1;
	}
	
	//actor message pool
	private final ThreadLocal<LinkedList<DFActorMessage>> _actorMsgPool = new ThreadLocal<LinkedList<DFActorMessage>>(){
		protected java.util.LinkedList<DFActorMessage> initialValue() {
			return new LinkedList<>();
		};
	};
	protected DFActorMessage newActorMessage(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context){
		final DFActorMessage msg = _actorMsgPool.get().poll();
		if(msg == null){
			return new DFActorMessage(srcId, dstId, sessionId, subject, cmd, payload, context);
		}else{
			msg.reset(srcId, dstId, sessionId, subject, cmd, payload, context);
		}
		return msg;
	}
	protected void recycleActorMessage(final DFActorMessage msg){
		msg.payload = null;
		_actorMsgPool.get().offer(msg);
	}
	
	protected void addTimeout(int srcId, int delay, final int requestId){
		final int idxTimer = Math.abs(_timerIdxCount.incrementAndGet())%_timerThNum;
		_lsTimer.get(idxTimer).addTimeout(delay*DFActor.TIMER_UNIT_MILLI, new DFActorTimeoutWrapper(srcId, requestId));
	}
	protected long getTimerStartNano(){
		return _lsLoopTimer.get(0).getTimerStart();
	}
	protected long getTimerNowNano(){
		return _lsLoopTimer.get(0).getTimerNow();
	}
	
	private class LoopWorker implements Runnable{
		protected final int id;
		private final int _consumeType;
		private final boolean _initSysActor;
		private final boolean _isLogicActorThread;
		protected LoopWorker(int id, boolean initSysActor, boolean isLogicActorThread) {
			this.id = id;
			this._initSysActor = initSysActor;
			this._isLogicActorThread = isLogicActorThread;
			if(id < 3){
				_consumeType = DFActorDefine.CONSUME_SINGLE;
			}else if(id < 5){
				_consumeType = DFActorDefine.CONSUME_HALF;
			}else{
				_consumeType = DFActorDefine.CONSUME_ALL;
			}
		}
		@Override
		public void run(){
			_cdInit.countDown();
			int count = 0;
			while(!_hasStarted && ++count < 10){
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			_onLoop = true;
			//
			if(_initSysActor){
				//init system actor
				if(_initCfg.isUseSysLog()){ //use system log
					createActor(DFActorDefine.ACTOR_NAME_LOG, ActorLog.class, null, 
							0, _initCfg.getSysLogConsumeType(), false);
				}
				//create entry actor
				createActor(_entryName, _entryClassz, _entryParam, _entryScheduleUnit, 
						_entryConsumeType, false);
			}
			final String thName = Thread.currentThread().getName();
			while(_onLoop){
				try{
					if(_isLogicActorThread){  //logic actor
						final DFActorWrapper wrap = _queueGlobalActor.poll(1, TimeUnit.SECONDS);
						if(wrap != null){
							if(wrap.consumeMsg(_consumeType) == 0){ //back to global queue
								_queueGlobalActor.offer(wrap);
							}
						}
					}else{	//block actor
						final DFActorWrapper wrap = _queueGlobalBlockActor.poll(1, TimeUnit.SECONDS);
						if(wrap != null){
							if(wrap.consumeMsg(_consumeType) == 0){ //back to global queue
								_queueGlobalBlockActor.offer(wrap);
							}
						}
					}
				}catch(Throwable e){
					e.printStackTrace();
				}
			}
			//
			_debugLog("WorkLoopDone: "+thName);
			_cdWorkerStop.countDown();
		}
		private volatile boolean _onLoop = false;
		protected void stop(){
			_onLoop = false;
		}
	}
	private class LoopTimer implements Runnable{
		private final DFHashWheelTimer timer;
		private volatile long tmStart = 0;
		private volatile long tmNow = 0;
		public LoopTimer(final DFHashWheelTimer timer) {
			this.timer = timer;
			tmStart = System.currentTimeMillis(); //System.nanoTime();
			tmNow = tmStart;
		}
		
		@Override
		public void run(){
			_onLoop = true;
			tmStart = System.currentTimeMillis(); //System.nanoTime();
			timer.start(tmStart);
			_cdInit.countDown();
			//
			tmNow = tmStart;
			long tmPre = tmNow;
			while(_onLoop){
				try{
					Thread.sleep(TIMER_REAL_DURATION_MILLI);
					tmNow = System.currentTimeMillis(); //System.nanoTime();
					timer.onTick(tmNow, tmNow - tmPre);
					tmPre = tmNow;
				}catch(Throwable e){
					e.printStackTrace();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
			//
			_debugLog("TimerLoopDone: "+Thread.currentThread().getName());
			_cdWorkerStop.countDown();
		}
		private volatile boolean _onLoop = false;
		protected void stop(){
			_onLoop = false;
		}
		
		protected long getTimerNow(){
			return tmNow;
		}
		protected long getTimerStart(){
			return tmStart;
		}
	}
	
	class DFActorTimeoutWrapper implements DFTimeout{
		protected int requestId;
		private int srcId;
		protected DFActorTimeoutWrapper(int srcId, int requestId) {
			this.srcId = srcId;
			this.requestId = requestId;
		}
		protected void reset(int srcId, int requestId){
			this.srcId = srcId;
			this.requestId = requestId;
		}
		@Override
		public void onTimeout() {
			send(0, srcId, 0, DFActorDefine.SUBJECT_TIMER, requestId, null, false);
		}
	}
	
	class DFActorScheduleWrapper implements DFScheduleTick{
		protected final int srcId;
		public DFActorScheduleWrapper(final int srcId) {
			this.srcId = srcId;
		}
		@Override
		public int onScheduleTick(long dlt) {
			final int ret = send(0, srcId, 0, DFActorDefine.SUBJECT_SCHEDULE, (int)dlt, null, false);
			if(ret == 0){
				return 0;
			}
			return 1;
		}
	}
	
	protected int getClientIoThreadNum(){
		return _initCfg.getClientIoThreadNum();
	}
	
	static final Map<String,Integer> s_mapSysActorNameId = new HashMap<>();
	static{
		s_mapSysActorNameId.put(DFActorDefine.ACTOR_NAME_LOG, DFActorDefine.ACTOR_ID_LOG);
	}
}
