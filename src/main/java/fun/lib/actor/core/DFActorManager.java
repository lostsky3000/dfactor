package fun.lib.actor.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.locks.StampedLock;

import com.funtag.util.log.DFLogFactory;
import com.funtag.util.log.DFLogger;
import com.funtag.util.system.DFSysUtil;
import com.funtag.util.timer.DFHashWheelTimer;
import com.funtag.util.timer.DFScheduleTick;
import com.funtag.util.timer.DFTimeout;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.helper.ActorLog;
import fun.lib.actor.helper.DFActorLogLevel;
import fun.lib.actor.helper.DFSysBlockActor;
import fun.lib.actor.po.ActorProp;
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
	private LinkedBlockingQueue<DFActorWrap> _queueGlobalActor = new LinkedBlockingQueue<>();
	private LinkedBlockingQueue<DFActorWrap> _queueGlobalBlockActor = new LinkedBlockingQueue<>();
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
	
	private int _blockThNum = 0;
	private int[] _arrSysBlockId = null;
	
	private int logLevel = DFActorLogLevel.DEBUG;
	public int getLogLevel(){
		return logLevel;
	}
	
	/**
	 * 启动dfactor
	 * @param prop actor配置
	 * @return 创建成功or失败
	 */
	public boolean start(ActorProp prop){
		return start(new DFActorManagerConfig(), prop);
	}
	/**
	 * 启动dfactor
	 * @param entryClassz 入口actor class
	 * @return 创建成功or失败
	 */
	public boolean start(Class<? extends DFActor> entryClassz){
		ActorProp prop = ActorProp.newProp()
				.classz(entryClassz);
		return start(new DFActorManagerConfig(), prop);
	}
	/**
	 * 启动dfactor
	 * @param entryName 入口actor名字(全局唯一)
	 * @param entryClassz 入口actor class
	 * @return 创建成功or失败
	 */
	public boolean start(String entryName, Class<? extends DFActor> entryClassz){
		ActorProp prop = ActorProp.newProp().name(entryName)
				.classz(entryClassz);
		return start(new DFActorManagerConfig(), prop);
	}
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param entryName 入口actor名字(全局唯一)
	 * @param entryClassz 入口actor class
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, String entryName, Class<? extends DFActor> entryClassz){
		ActorProp prop = ActorProp.newProp()
				.name(entryName).classz(entryClassz);
		return start(cfg, prop);
	}
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param entryClassz 入口actor class
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, Class<? extends DFActor> entryClassz){
		ActorProp prop = ActorProp.newProp()
				.classz(entryClassz);
		return start(cfg, prop);
	}
	
	/**
	 * 启动dfactor
	 * @param cfg 启动配置
	 * @param 入口actor配置
	 * @return 创建成功or失败
	 */
	public boolean start(DFActorManagerConfig cfg, ActorProp prop){
		boolean bRet = false;
		do {
			DFLogFactory.setLogLevel(cfg.getLogLevel());
			logLevel = cfg.getLogLevel();
			if(_hasStarted){
				break;
			}
			_dumpInitInfo();
			//
			_entryName = prop.getName();
			_entryClassz = prop.getClassz();
			_entryParam = prop.getParam();
			_entryScheduleUnit = DFActor.transTimeRealToTimer(prop.getScheduleMilli());
			int entryConsumeType = prop.getConsumeType();
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
			_blockThNum = blockWorkerThNum;
			_arrSysBlockId = new int[_blockThNum];
			
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
					th.setName("thread-block-worker-"+(i-logicWorkerThNum));
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
	
	/**
	 * 启动dfactor为daemon模式，加载外部jar
	 * @param cfg 启动配置
	 * @param dirJar 外部jar文件所在目录
	 * @param entryActorFullName 启动actor全路径名
	 * @param params 启动actor参数
	 * @return
	 */
	public boolean startAsDaemon(DFActorManagerConfig cfg, String dirJar, String entryActorFullName, Object params){
		boolean bRet = false;
		do {
			File dir = new File(dirJar);
			if(!dir.isDirectory()){ //not dir
				log.E("dirJar invalid: "+dirJar);
				break;
			}
			//filter jar file
			File[] arrJarFile = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					File tmpFile = new File(dir.getAbsolutePath()+File.separator + name);
					if(tmpFile.isFile() && name.endsWith(".jar")){
						return true;
					}
					return false;
				}
			});
			//check jar num
			int size = arrJarFile.length;
			if(size < 1){
				log.E("no jar found in dir: "+dirJar);
				break;
			}
			//load jar
			URL[] urls = new URL[size];
			try {
				for(int i=0; i<size; ++i){
					urls[i] = arrJarFile[i].toURI().toURL();	
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				break;
			}
			//
			Class clzEntry = null;
			Class clz = null;
			ClassLoader defLoader = Thread.currentThread().getContextClassLoader();
			try {
				clz = defLoader.loadClass(DFActor.class.getName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			URLClassLoader urlCl = new URLClassLoader(urls, defLoader);
			try {
				for(int i=0; i<size; ++i){
					String jarPath = "jar:file:/"+arrJarFile[i].getAbsolutePath()+"!/";
					URL urlJar = new URL(jarPath);
					JarURLConnection jarConn = (JarURLConnection) urlJar.openConnection();
					JarFile jar = jarConn.getJarFile();
					Enumeration<JarEntry> enumJar = jar.entries();
					while(enumJar.hasMoreElements()){
						JarEntry en = enumJar.nextElement();
						String name = en.getName();
						if(!en.isDirectory() && name.endsWith(".class")){
							String clzName = name.substring(0, name.length() - 6).replaceAll("/", ".");
							clz = urlCl.loadClass(clzName);
							if(clz == null){
								continue;
							}
//							Type[] arrType = clz.getGenericInterfaces();
//							Type superType = clz.getGenericSuperclass();
//							superType.getClass().getName();
//							boolean primitive = clz.isPrimitive();
//							if(!primitive && clz.isAssignableFrom(DFActor.class)){
//								
//							}
							if(clzEntry == null && clzName.equals(entryActorFullName)){
								clzEntry = clz;
							}
						}
					}
					jar.close();
				}
				if(clzEntry != null){ //find entryActor
					ActorProp prop = ActorProp.newProp()
							.classz(clzEntry)
							.param(params);
					return this.start(cfg, prop);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				break;
			}finally{
				try {
					urlCl.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
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
				//close all db conn
				DFDbManager.get().closeAllPool();
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
	protected int doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId, final DFActorTcpDispatcher dispatcher, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, srcActorId, dispatcher, _clientIoGroup, requestId);
	}
	//
	private Map<Integer, DFActorWrap> _mapActor = new HashMap<>();
	private Map<String, DFActorWrap> _mapActorName = new HashMap<>();
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
		int id = 0;
		if(name != null){
			id = _getSysActorIdByName(name);
		}else{  //没有名字 
			
		}
		if(id < 1){  //not sys actor
			synchronized (_idLock) { //gen id
				id = _idCount;
				if(++_idCount >= Integer.MAX_VALUE){
					_idCount = DFActorDefine.ACTOR_ID_APP_BEGIN;
				}
			}
		}
		if(name == null){ //没有名字
			name = DFActorDefine.ACTOR_NAME_DEF_PFX + id;
		}
		try {
			Class[] paramsType = {Integer.class, String.class, Boolean.class};
			Object[] params = {new Integer(id), name, new Boolean(isBlockActor)};
			Constructor<? extends DFActor> ctor = classz.getDeclaredConstructor(paramsType);
			ctor.setAccessible(true);
			actor = ctor.newInstance(params);
			actor.setConsumeType(consumeType);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException 
				| SecurityException | IllegalArgumentException | InvocationTargetException e1) {
			e1.printStackTrace();
			return -2;
		}
		final DFActorWrap wrapper = new DFActorWrap(actor);
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
		//need schedule
		if(scheduleUnit > 0){ 
			final int idxTimer = Math.abs(_timerIdxCount.incrementAndGet())%_timerThNum;
			_lsTimer.get(idxTimer).addSchedule(scheduleUnit*DFActor.TIMER_UNIT_MILLI, 
					new DFActorScheduleWrapper(wrapper.getActorId()), getTimerNowNano());
		}
		//send actor start event
		try{
			if(wrapper.pushMsg(0, 0, DFActorDefine.SUBJECT_START, 0, param, null, false, null, false) == 0){ //add to global queue
				if(wrapper.isLogicActor()){
					_queueGlobalActor.offer(wrapper);
				}else{
					_queueGlobalBlockActor.offer(wrapper);
				}
			}
		}catch(Throwable e){
			e.printStackTrace();
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
		DFActorWrap wrap = null;
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
	
	protected int sendCallback(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, final boolean addTail, Object context, Object userHandler){
		return send(srcId, dstId, requestId, subject, cmd, payload, addTail, context, userHandler, true);
	}
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, final boolean addTail){
		return send(srcId, dstId, requestId, subject, cmd, payload, addTail, null, null, false);
	}
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, final boolean addTail, Object context, Object userHandler, boolean isCb){
		DFActorWrap wrap = null;
		//
		_lockActorRead.lock();
		try{
			wrap = _mapActor.get(dstId);
		}finally{
			_lockActorRead.unlock();
		}
		if(wrap != null){
			if(wrap.pushMsg(srcId, requestId, subject, cmd, payload, context, addTail, userHandler, isCb) == 0){ //add to global queue
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
			int subject, int cmd, Object payload, final boolean addTail, Object context, Object userHandler){
		DFActorWrap wrap = null;
		_lockActorRead.lock();
		try{
			wrap = _mapActorName.get(dstName);
		}finally{
			_lockActorRead.unlock();
		}
		if(wrap != null){
			if(wrap.pushMsg(srcId, requestId, subject, cmd, payload, context, addTail, userHandler, false) == 0){ //add to global queue
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
	
	protected int callSysBlockActor(int srcId, int shardId, int cmd, Object payload, Object userHandler){
		if(_blockThNum < 1){ //no sys block
			return -1;
		}
		int dstId = _arrSysBlockId[shardId%_blockThNum];
		return this.send(srcId, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, userHandler, false);
	}
	
	//actor message pool
	private final ThreadLocal<LinkedList<DFActorMessage>> _actorMsgPool = new ThreadLocal<LinkedList<DFActorMessage>>(){
		protected java.util.LinkedList<DFActorMessage> initialValue() {
			return new LinkedList<>();
		};
	};
	protected DFActorMessage newActorMessage(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context, Object userHandler, boolean isCb){
		final DFActorMessage msg = _actorMsgPool.get().poll();
		if(msg == null){
			return new DFActorMessage(srcId, dstId, sessionId, subject, cmd, payload, context, userHandler, isCb);
		}else{
			msg.reset(srcId, dstId, sessionId, subject, cmd, payload, context, userHandler, isCb);
		}
		return msg;
	}
	protected void recycleActorMessage(final DFActorMessage msg){
		msg.payload = null;
		_actorMsgPool.get().offer(msg);
	}
	
	protected void addTimeout(int srcId, int delay, int requestId, CbTimeout cb){
		final int idxTimer = Math.abs(_timerIdxCount.incrementAndGet())%_timerThNum;
		_lsTimer.get(idxTimer).addTimeout(delay*DFActor.TIMER_UNIT_MILLI, new DFActorTimeoutWrapper(srcId, requestId, cb));
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
				
				if(_blockThNum > 0){   //create sys block actor
					for(int i=0; i<_blockThNum; ++i){
						int tmpId = createActor(null, DFSysBlockActor.class, null, 0, DFActorDefine.CONSUME_SINGLE, true);
						_arrSysBlockId[i] = tmpId;
					}
				}
			}
			final String thName = Thread.currentThread().getName();
			while(_onLoop){
				try{
					if(_isLogicActorThread){  //logic actor
						final DFActorWrap wrap = _queueGlobalActor.poll(1, TimeUnit.SECONDS);
						if(wrap != null){
							if(wrap.consumeMsg(_consumeType) == 0){ //back to global queue
								_queueGlobalActor.offer(wrap);
							}
						}
					}else{	//block actor
						final DFActorWrap wrap = _queueGlobalBlockActor.poll(1, TimeUnit.SECONDS);
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
		private CbTimeout cb;
		protected DFActorTimeoutWrapper(int srcId, int requestId, CbTimeout cb) {
			this.srcId = srcId;
			this.requestId = requestId;
			this.cb = cb;
		}
		protected void reset(int srcId, int requestId){
			this.srcId = srcId;
			this.requestId = requestId;
		}
		@Override
		public void onTimeout() {
			send(0, srcId, 0, DFActorDefine.SUBJECT_TIMER, requestId, null, false, null, cb, false);
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
