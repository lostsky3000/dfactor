package fun.lib.actor.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;
import fun.lib.actor.po.DFTcpClientCfg;
import io.netty.buffer.ByteBuf;
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
//	private volatile List<LoopWorker> _lsLoopWorker = null;
	
	private volatile List<LoopRejectMonitor> _lsLoopRejectMonitor = null;
	//
	
	private volatile List<DFHashWheelTimer> _lsTimer = null;
	private volatile List<LoopTimer> _lsLoopTimer = null;
	private volatile CountDownLatch _cdWorkerStop = null;
	private volatile int _timerThNum = 1;
	private final AtomicInteger _timerIdxCount = new AtomicInteger(0);
	//
	private volatile LinkedBlockingQueue<Runnable> _queueLogicActor = null;
	private final ConcurrentLinkedQueue<DFActorWrap> _queueRejectLogicActor = new ConcurrentLinkedQueue<>();
	private final Lock _lockQueueActor = new ReentrantLock();
	private final Condition _condQueueActor = _lockQueueActor.newCondition();
	private ThreadPoolExecutor _poolLogic = null;
	private final RejectedExecutionHandler _rejectHandlerLogic = new RejectedExecutionHandler() {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			try{
				_queueRejectLogicActor.offer((DFActorWrap)r);
				notifyRejectQueueLogic();
			}catch(Throwable e){
				e.printStackTrace();
			}
			
		}
	};
	protected void addToQueueLogic(DFActorWrap wrap){
		_poolLogic.execute(wrap);
	}
	protected void notifyRejectQueueLogic(){
		_lockQueueActor.lock();
		try{
			_condQueueActor.signal();
		}finally{
			_lockQueueActor.unlock();
		}
	}
	//
	private volatile LinkedBlockingQueue<Runnable> _queueBlockActor = null;
	private final ConcurrentLinkedQueue<DFActorWrap> _queueRejectBlockActor = new ConcurrentLinkedQueue<>();
	private final Lock _lockQueueBlockActor = new ReentrantLock();
	private final Condition _condQueueBlockActor = _lockQueueBlockActor.newCondition();
	private ThreadPoolExecutor _poolBlock = null;
	private final RejectedExecutionHandler _rejectHandlerBlock = new RejectedExecutionHandler() {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			_queueRejectBlockActor.offer((DFActorWrap)r);
			notifyRejectQueueBlock();
		}
	};
	protected void addToQueueBlock(DFActorWrap wrap){
		_poolBlock.execute(wrap);
	}
	protected void notifyRejectQueueBlock(){
		_lockQueueBlockActor.lock();
		try{
			_condQueueBlockActor.signal();
		}finally{
			_lockQueueBlockActor.unlock();
		}
	}
	//
	private final LinkedBlockingQueue<Runnable> _queueClusterActor = new LinkedBlockingQueue<>(10);
	private final ConcurrentLinkedQueue<DFActorWrap> _queueRejectClusterActor = new ConcurrentLinkedQueue<>();
	private final Lock _lockQueueClusterActor = new ReentrantLock();
	private final Condition _condQueueClusterActor = _lockQueueClusterActor.newCondition(); 
	private ThreadPoolExecutor _poolCluster = null;
	private final RejectedExecutionHandler _rejectHandlerCluster = new RejectedExecutionHandler() {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			_queueRejectClusterActor.offer((DFActorWrap)r);
			notifyRejectQueueCluster();
		}
	};
	protected void addToQueueCluster(DFActorWrap wrap){
		_poolCluster.execute(wrap);
	}
	protected void notifyRejectQueueCluster(){
		_lockQueueClusterActor.lock();
		try{
			_condQueueClusterActor.signal();
		}finally{
			_lockQueueClusterActor.unlock();
		}
	}
	//
	private class DFThreadFactory implements ThreadFactory{
		private final String thName;
		private final AtomicInteger count = new AtomicInteger(0);
		private DFThreadFactory(String thName){
			this.thName = thName;
		}
		@Override
		public Thread newThread(Runnable r) {
			Thread th = new Thread(r);
			th.setName(thName + (count.incrementAndGet()));
			return th;
		}
	}
	private ThreadPoolExecutor _createThreadPool(int coreSize, int maxSize, int aliveTime, 
				LinkedBlockingQueue<Runnable> queue,
				DFThreadFactory threadFactory, RejectedExecutionHandler rejectHandler){
		ThreadPoolExecutor pool = new ThreadPoolExecutor(coreSize, maxSize, aliveTime, TimeUnit.SECONDS, 
				queue, threadFactory, rejectHandler);
		return pool;
	}
	
	//
	private volatile CountDownLatch _cdInit = null;
	private volatile DFActorManagerConfig _initCfg = null;
	//
	private volatile String _entryName = null;
	private volatile Class<? extends DFActor> _entryClassz = null;
	private volatile Object _entryParam = null;
	private volatile int _entryScheduleUnit = 0;
	private volatile int _entryScheduleMilli = 0;
	private volatile boolean _entryIsBlock = false;
	private volatile int _entryConsumeType = DFActorDefine.CONSUME_AUTO;
	private volatile EventLoopGroup _clientIoGroup = null;
	private volatile EventLoopGroup _clusterIoGroup = null;
	
	private volatile boolean _isClusterEnable = false;
	private volatile String _nodeName = null;
	private volatile String _nodeType = null;
	private int _blockThNum = 0;
	private int _workerThNum = 0;
	private int _clusterThNum = 0;
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
	 * @param prop 入口actor配置
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
			_entryIsBlock = prop.isBlock();
			_entryScheduleMilli = prop.getScheduleMilli();
			_entryScheduleUnit = DFActor.transTimeRealToTimer(_entryScheduleMilli);
			int entryConsumeType = prop.getConsumeType();
			if(entryConsumeType>=DFActorDefine.CONSUME_AUTO &&
					entryConsumeType<=DFActorDefine.CONSUME_ALL){
				_entryConsumeType = entryConsumeType;
			}
			if(cfg.getClusterConfig() != null){ //has cluster
				int ioThNum = Math.max(1, cfg.getClusterConfig().getIoThreadNum());
				_clusterThNum = 1;  //use one thread for cluster biz
				_clusterIoGroup = DFSysUtil.isLinux()?new EpollEventLoopGroup(ioThNum):new NioEventLoopGroup(ioThNum);
				_isClusterEnable = true;
			}else{
				_clusterThNum = 0;
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
			_workerThNum = logicWorkerThNum;
			int blockThNumMax = cfg.getBlockWorkerThreadNumMax();
			int logicThNumMax = cfg.getLogicWorkerThreadNumMax();
			_arrSysBlockId = new int[_blockThNum];
			//
			_timerThNum = cfg.getTimerThreadNum();
			int tmpThNum = (logicWorkerThNum>0?1:0) + (blockWorkerThNum>0?1:0) 
					+ (_clusterThNum>0?1:0) + (_timerThNum>0?1:0);
			_cdInit = new CountDownLatch(tmpThNum); //worker + timer
			_cdWorkerStop = new CountDownLatch(tmpThNum);
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
			//
			ArrayList<String> lsThName = new ArrayList<>();
			_lsLoopRejectMonitor = new ArrayList<>();
			if(_clusterThNum > 0){ //has cluster
				DFThreadFactory tf = new DFThreadFactory("thread-cluster-");
				_poolCluster = _createThreadPool(_clusterThNum, _clusterThNum, 0, _queueClusterActor, tf, _rejectHandlerCluster);
				LoopRejectMonitor loop = new LoopRejectMonitor(_poolCluster, _queueRejectClusterActor, 
											_lockQueueClusterActor, _condQueueClusterActor, _queueClusterActor);
				_lsLoopRejectMonitor.add(loop);
				lsThName.add("thread-monitor-cluster");
			}
			if(_blockThNum > 0){ //has block
				DFThreadFactory tf = new DFThreadFactory("thread-block-");
				_queueBlockActor = new LinkedBlockingQueue<>(cfg.getBlockQueueWait());
				_poolBlock = _createThreadPool(_blockThNum, blockThNumMax, 120, _queueBlockActor, tf, _rejectHandlerBlock);
				LoopRejectMonitor loop = new LoopRejectMonitor(_poolBlock, _queueRejectBlockActor, 
											_lockQueueBlockActor, _condQueueBlockActor, _queueBlockActor);
				_lsLoopRejectMonitor.add(loop);
				lsThName.add("thread-monitor-block");
			}
			if(_workerThNum > 0){ //logic
				DFThreadFactory tf = new DFThreadFactory("thread-logic-");
				_queueLogicActor = new LinkedBlockingQueue<>(cfg.getLogicQueueWait());
				_poolLogic = _createThreadPool(_workerThNum, logicThNumMax, 120, _queueLogicActor, tf, _rejectHandlerLogic);
				LoopRejectMonitor loop = new LoopRejectMonitor(_poolLogic, _queueRejectLogicActor, 
											_lockQueueActor, _condQueueActor, _queueLogicActor);
				_lsLoopRejectMonitor.add(loop);
				lsThName.add("thread-monitor-logic");
			}
			final Thread[] arrWorkerTh = new Thread[_lsLoopRejectMonitor.size()];
			for(int i=0; i<arrWorkerTh.length; ++i){
				Thread th = new Thread(_lsLoopRejectMonitor.get(i));
				th.setName(lsThName.get(i));
				arrWorkerTh[i] = th;
			}
			
//			//start worker thread
//			_lsLoopWorker = new ArrayList<>(logicWorkerThNum + blockWorkerThNum + _clusterThNum);
//			//logic worker thread
//			final Thread[] arrWorkerTh = new Thread[logicWorkerThNum + blockWorkerThNum + _clusterThNum];
//			for(int i=0; i<arrWorkerTh.length; ++i){
//				LoopWorker loop = null;
//				Thread th = null;
//				if(i < logicWorkerThNum){  //logic worker thread
//					loop = new LoopWorker(i+1, i==0?true:false, _queueGlobalActor, _lockQueueActor, _condQueueActor);
//					th = new Thread(loop);
//					th.setName("thread-logic-worker-"+i);
//				}else if(i < logicWorkerThNum + blockWorkerThNum){   //block worker thread
//					loop = new LoopWorker(i+1, false, _queueGlobalBlockActor, _lockQueueBlockActor, _condQueueBlockActor);
//					th = new Thread(loop);
//					th.setName("thread-block-worker-"+(i-logicWorkerThNum));
//				}else{ //cluster thread
//					loop = new LoopWorker(i+1, false, _queueGlobalClusterActor, _lockQueueClusterActor, _condQueueClusterActor);
//					th = new Thread(loop);
//					th.setName("thread-cluster-worker-"+(i-logicWorkerThNum-blockWorkerThNum));
//				}
//				_lsLoopWorker.add(loop);
//				arrWorkerTh[i] = th;
//			}
			
			for(int i=0; i<arrWorkerTh.length; ++i){
				arrWorkerTh[i].start();
			}
			
			try {
				_cdInit.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//do logic init task
			_poolLogic.submit(new Runnable(){
				@Override
				public void run() {
					//init system actor
					if(_initCfg.isUseSysLog()){ //use system log
						createActor(DFActorDefine.ACTOR_NAME_LOG, ActorLog.class, null, 
								0, _initCfg.getSysLogConsumeType(), false);
					}
					if(_blockThNum > 0){   //create sys block actor
						for(int i=0; i<_blockThNum; ++i){
							int tmpId = createActor(null, DFSysBlockActor.class, null, 0, DFActorDefine.CONSUME_ALL, true);
							_arrSysBlockId[i] = tmpId;
						}
					}
					//create entry actor
					DFActorClusterConfig clusterCfg = _initCfg.getClusterConfig();
					if(clusterCfg == null){
						createActor(_entryName, _entryClassz, _entryParam, _entryScheduleUnit, 
								_entryConsumeType, _entryIsBlock);
					}else{  //use cluster
						ActorProp propEntry = ActorProp.newProp()
								.name(_entryName).classz(_entryClassz).param(_entryParam)
								.scheduleMilli(_entryScheduleMilli).consumeType(DFActorDefine.CONSUME_ALL);
						HashMap<String,Object> mapParam = new HashMap<>();
						mapParam.put("entry", propEntry);
						mapParam.put("cluster", clusterCfg);
						//
						createActor(DFClusterActor.NAME, DFClusterActor.class, mapParam, 0, 
								DFActorDefine.CONSUME_ALL, true, false);
					}
				}
			});
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
	 * @return 是否成功
	 */
	public boolean startAsDaemon(DFActorManagerConfig cfg, String dirJar, String entryActorFullName, Object params){
		boolean bRet = false;
		do {
			HashMap<String,Class<?>> mapClz = new HashMap<>();
			if(!this.loadJars(dirJar, mapClz)){
				break;
			}
			//
			Class clzEntry = mapClz.get(entryActorFullName);
			if(clzEntry != null){ //find entryActor
				ActorProp prop = ActorProp.newProp()
						.classz(clzEntry)
						.param(params);
				return this.start(cfg, prop);
			}else{
				return false;
			}
		} while (false);
		return bRet;
	}
	
	protected boolean loadJars(String dirJar, Map<String,Class<?>> map){
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
				log.W("no jar found in dir: "+dirJar);
				return true;
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
//					String jarPath = "jar:file:/"+arrJarFile[i].getAbsolutePath()+"!/";
//					URL urlJar = new URL(jarPath);
//					JarURLConnection jarConn = (JarURLConnection) urlJar.openConnection();
//					JarFile jar = jarConn.getJarFile();
					
					JarFile jar = new JarFile(arrJarFile[i]);
					
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
							if(map != null){
								map.put(clzName, clz);
							}
						}
					}
					jar.close();
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
//					for(LoopWorker w : _lsLoopWorker){
//						w.stop();
//					}
//					_lsLoopWorker.clear(); _lsLoopWorker = null;
					for(LoopRejectMonitor loop : _lsLoopRejectMonitor){
						loop.stop();
					}
					_lsLoopRejectMonitor.clear(); _lsLoopRejectMonitor = null;
					if(_workerThNum > 0){
						_poolLogic.shutdown();
					}
					if(_blockThNum > 0){
						_poolBlock.shutdown();
					}
					if(_clusterThNum > 0){
						_poolCluster.shutdown();
					}
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
				if(_clusterIoGroup != null){
					_clusterIoGroup.shutdownGracefully();
					_clusterIoGroup = null;
				}
				//
				DFActorManagerJs.get().shutdown();
				DFVirtualHostManager.get().shutdown();
				//
				svcShutdown.shutdown();
			}
		});
	}
	
	protected int doUdpSend(ByteBuf buf, InetSocketAddress addrDst){
		return DFSocketManager.get().doUdpSend(_clientIoGroup, buf, addrDst);
	}
	protected int doUdpSendViaCluster(ByteBuf buf, InetSocketAddress addrDst){
		return DFSocketManager.get().doUdpSend(_clusterIoGroup, buf, addrDst);
	}
	protected int doUdpSendViaCluster(ByteBuf buf, InetSocketAddress addrDst, InetSocketAddress addrSender){
		return DFSocketManager.get().doUdpSend(_clusterIoGroup, buf, addrDst, addrSender);
	}
	protected EventLoopGroup getClusterIoGroup(){
		return _clusterIoGroup;
	}
	
	protected int doTcpConnectViaCluster(final DFTcpClientCfg cfg, final int srcActorId, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, srcActorId, _clusterIoGroup, requestId);
	}
	protected int doTcpConnectViaCluster(final DFTcpClientCfg cfg, final int srcActorId, final DFActorTcpDispatcher dispatcher, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, srcActorId, dispatcher, _clusterIoGroup, requestId);
	}
	
	protected int doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, srcActorId, _clientIoGroup, requestId);
	}
	protected int doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId, final DFActorTcpDispatcher dispatcher, final int requestId){
		return DFSocketManager.get().doTcpConnect(cfg, srcActorId, dispatcher, _clientIoGroup, requestId);
	}
	//
	private HashMap<Integer,String> _mapActor = new HashMap<>();
	private ConcurrentHashMap<Integer, DFActorWrap> _cmapActor = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, DFActorWrap> _cmapActorName = new ConcurrentHashMap<>();
	private HashSet<String> _setActorName = new HashSet<>();
	//
	private final ReentrantReadWriteLock _lockMapActor = new ReentrantReadWriteLock();
	private final ReadLock _lockActorRead = _lockMapActor.readLock();
	private final WriteLock _lockActorWrite = _lockMapActor.writeLock();
	
	private int _actorNum = 0;
	private int _idCount = DFActorDefine.ACTOR_ID_APP_BEGIN;
	private final String _idLock = "_idLockLostskysadfihui23234#@#$";
	
	protected int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType, boolean isBlockActor){
		return this.createActor(name, classz, param, scheduleUnit, consumeType, false, isBlockActor);
	}
	
	protected int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType, boolean isClusterActor, boolean isBlockActor){
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
		final DFActorWrap wrap = new DFActorWrap(actor, isClusterActor);
		//
		_lockActorWrite.lock();
		try{
			if(_setActorName.contains(name)){ //name duplicated
				return -3;
			}
			_setActorName.add(name);
			_mapActor.put(id, name);
			++_actorNum;
		}finally{
			_lockActorWrite.unlock();
		}
		_cmapActor.put(id, wrap);
		_cmapActorName.put(name, wrap);
		//need schedule
		if(scheduleUnit > 0){ 
			final int idxTimer = Math.abs(_timerIdxCount.incrementAndGet())%_timerThNum;
			_lsTimer.get(idxTimer).addSchedule(scheduleUnit*DFActor.TIMER_UNIT_MILLI, 
					new DFActorScheduleWrapper(wrap.getActorId()), getTimerNowNano());
		}
		//send actor start event
		try{
			if(wrap.pushMsg(0, 0, DFActorDefine.SUBJECT_START, 0, param, null, false, null, false, null, null) == 0){ //add to global queue
				if(wrap.isClusterActor()){
//					_queueGlobalClusterActor.offer(wrap);
//					_doGlobalClusterQueueNotify();
					_poolCluster.execute(wrap);
				}else if(wrap.isBlockActor()){
//					_queueGlobalBlockActor.offer(wrap);
//					_doGlobalBlockQueueNotify();
					_poolBlock.execute(wrap);
				}else{
//					_queueGlobalActor.offer(wrap);
//					_doGlobalQueueNotify();
					_poolLogic.execute(wrap);
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
		//
		String name = null;
		_lockActorWrite.lock();
		try{
			name = _mapActor.remove(id);
			if(name != null){
				--_actorNum;
				_setActorName.remove(name);
			}
		}finally{
			_lockActorWrite.unlock();
		}
		if(name != null){
			_cmapActorName.remove(name);
		}
		DFActorWrap wrap = _cmapActor.remove(id);
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
			int subject, int cmd, Object payload, boolean addTail, Object context, Object userHandler){
		return send(srcId, dstId, requestId, subject, cmd, payload, addTail, context, userHandler, true, null, null);
	}
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, boolean addTail){
		return send(srcId, dstId, requestId, subject, cmd, payload, addTail, null, null, false, null, null);
	}
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, boolean addTail, Object context, Object userHandler, boolean isCb){
		return send(srcId, dstId, requestId, subject, cmd, payload, addTail, context, userHandler, isCb, null, null);
	}
	protected int send(int srcId, int dstId, int requestId, 
			int subject, int cmd, Object payload, boolean addTail, Object context, Object userHandler, boolean isCb
			, Object payload2, String method){
		DFActorWrap wrap = _cmapActor.get(dstId); 
		if(wrap != null){
			if(wrap.pushMsg(srcId, requestId, subject, cmd, payload, context, addTail, userHandler, isCb, payload2, method) == 0){ //add to global queue
				if(wrap.isClusterActor()){
//					_queueGlobalClusterActor.offer(wrap);
//					_doGlobalClusterQueueNotify();
					_poolCluster.execute(wrap);
				}else if(wrap.isBlockActor()){
//					_queueGlobalBlockActor.offer(wrap);
//					_doGlobalBlockQueueNotify();
					_poolBlock.execute(wrap);
				}else{
//					_queueGlobalActor.offer(wrap);
//					_doGlobalQueueNotify();
					_poolLogic.execute(wrap);
				}
			} 
			return 0;
		}
		return 1;
	}
	protected int send(int srcId, String dstName, int requestId, 
			int subject, int cmd, Object payload, boolean addTail, Object context, Object userHandler){
		return send(srcId, dstName, requestId, subject, cmd, payload, addTail, context, userHandler, null, null, false);
	}
	protected int send(int srcId, String dstName, int requestId, 
			int subject, int cmd, Object payload, boolean addTail, Object context, Object userHandler, 
			Object payload2, String method){
		return send(srcId, dstName, requestId, subject, cmd, payload, addTail, context, userHandler, payload2, method, false);
	}
	protected int send(int srcId, String dstName, int requestId, 
			int subject, int cmd, Object payload, boolean addTail, Object context, Object userHandler, 
			Object payload2, String method, boolean isCb){
		DFActorWrap wrap = _cmapActorName.get(dstName);
		if(wrap != null){
			if(wrap.pushMsg(srcId, requestId, subject, cmd, payload, context, addTail, userHandler, isCb, payload2, method) == 0){ //add to global queue
				if(wrap.isClusterActor()){
//					_queueGlobalClusterActor.offer(wrap);
//					_doGlobalClusterQueueNotify();
					_poolCluster.execute(wrap);
				}else if(wrap.isBlockActor()){
//					_queueGlobalBlockActor.offer(wrap);
//					_doGlobalBlockQueueNotify();
					_poolBlock.execute(wrap);
				}else{
//					_queueGlobalActor.offer(wrap);
//					_doGlobalQueueNotify();
					_poolLogic.execute(wrap);
				}
			}
			return 0;
		}
		return 1;
	}
	
	protected int getActorIdByName(String name){
		DFActorWrap wrap = _cmapActorName.get(name);
		if(wrap != null){
			return wrap.getActorId();
		}
		return 0;
	}
	
	protected int callSysBlockActor(int srcId, int shardId, int cmd, Object payload, Object userHandler){
		if(_blockThNum < 1){ //no sys block
			return -1;
		}
		int dstId = _arrSysBlockId[shardId%_blockThNum];
		return this.send(srcId, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, userHandler, false, null, null);
	}
	
	//actor message pool
	private final ThreadLocal<LinkedList<DFActorMessage>> _actorMsgPool = new ThreadLocal<LinkedList<DFActorMessage>>(){
		protected java.util.LinkedList<DFActorMessage> initialValue() {
			return new LinkedList<>();
		};
	};
	protected DFActorMessage newActorMessage(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context, Object userHandler, boolean isCb, 
			Object payload2, String method){
		final DFActorMessage msg = _actorMsgPool.get().poll();
		if(msg == null){
			return new DFActorMessage(srcId, dstId, sessionId, subject, cmd, payload, context, userHandler, isCb, payload2, method);
		}else{
			msg.reset(srcId, dstId, sessionId, subject, cmd, payload, context, userHandler, isCb, payload2, method);
		}
		return msg;
	}
	protected void recycleActorMessage(final DFActorMessage msg){
		msg.payload = null;
		_actorMsgPool.get().offer(msg);
	}
	
	protected void addTimeout(int srcId, int delay, int subject, int requestId, CbTimeout cb){
		final int idxTimer = Math.abs(_timerIdxCount.incrementAndGet())%_timerThNum;
		_lsTimer.get(idxTimer).addTimeout(delay*DFActor.TIMER_UNIT_MILLI, 
				new DFActorTimeoutWrapper(srcId, subject, requestId, cb));
	}
	protected long getTimerStartNano(){
		return _lsLoopTimer.get(0).getTimerStart();
	}
	protected long getTimerNowNano(){
		return _lsLoopTimer.get(0).getTimerNow();
	}
	
	protected int getLogicThreadNum(){
		return _initCfg.getLogicWorkerThreadNum();
	}
	protected int getBlockThreadNum(){
		return _initCfg.getBlockWorkerThreadNum();
	}
	protected DFActorManagerConfig getInitCfg(){
		return _initCfg;
	}
	
	private class LoopRejectMonitor implements Runnable{
		private final ConcurrentLinkedQueue<DFActorWrap> _queueRejectActor;
		private final LinkedBlockingQueue<Runnable> _queueActor;
		private final Lock _lockActor;
		private final Condition _condActor;
		private final ThreadPoolExecutor _pool;
		public LoopRejectMonitor(ThreadPoolExecutor pool, ConcurrentLinkedQueue<DFActorWrap> queueRejectActor, 
				Lock lockActor, Condition condActor, LinkedBlockingQueue<Runnable> queueActor) {
			this._pool = pool;
			this._queueRejectActor = queueRejectActor;
			this._lockActor = lockActor;
			this._condActor = condActor;
			this._queueActor = queueActor;
		}
		@Override
		public void run() {
			final String thName = Thread.currentThread().getName();
			int maxThread = _pool.getCorePoolSize() + _pool.getMaximumPoolSize();
			_cdInit.countDown();
			_onLoop = true;
			DFActorWrap wrap = null;
			while(_onLoop){
				try{
					wrap = _queueRejectActor.poll();
					if(wrap != null){
						if(!_queueActor.offer(wrap, 2000, TimeUnit.MILLISECONDS)){
							_queueRejectActor.offer(wrap);
						}
					}else{
						_lockActor.lock();
						try{
							_condActor.await(2000, TimeUnit.MILLISECONDS);
						}finally{
							_lockActor.unlock();
						}
					}
				}catch(Throwable e){
					e.printStackTrace();
				}
			}
			_debugLog("LoopRejectMonitorDone: "+thName);
			_cdWorkerStop.countDown();
		}
		private volatile boolean _onLoop = false;
		protected void stop(){
			_onLoop = false;
		}
		
	}
	private class LoopWorker implements Runnable{
		protected final int id;
		private final int _consumeType;
		private final boolean _initSysActor;
		//
		private final ConcurrentLinkedQueue<DFActorWrap> _queueActor;
		private final Lock _lockActor;
		private final Condition _condActor;
		
		protected LoopWorker(int id, boolean initSysActor,
				ConcurrentLinkedQueue<DFActorWrap> queueActor, Lock lockActor, Condition condActor) {
			this.id = id;
			this._initSysActor = initSysActor;
			this._queueActor = queueActor;
			this._lockActor = lockActor;
			this._condActor = condActor;
//			if(id < 3){
//				_consumeType = DFActorDefine.CONSUME_HALF;
//			}else{
//				_consumeType = DFActorDefine.CONSUME_ALL;
//			}
			_consumeType = DFActorDefine.CONSUME_ALL;
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
				if(_blockThNum > 0){   //create sys block actor
					for(int i=0; i<_blockThNum; ++i){
						int tmpId = createActor(null, DFSysBlockActor.class, null, 0, DFActorDefine.CONSUME_ALL, true);
						_arrSysBlockId[i] = tmpId;
					}
				}
				//create entry actor
				DFActorClusterConfig clusterCfg = _initCfg.getClusterConfig();
				if(clusterCfg == null){
					createActor(_entryName, _entryClassz, _entryParam, _entryScheduleUnit, 
							_entryConsumeType, _entryIsBlock);
				}else{  //use cluster
					ActorProp propEntry = ActorProp.newProp()
							.name(_entryName).classz(_entryClassz).param(_entryParam)
							.scheduleMilli(_entryScheduleMilli).consumeType(_consumeType);
					HashMap<String,Object> mapParam = new HashMap<>();
					mapParam.put("entry", propEntry);
					mapParam.put("cluster", clusterCfg);
					//
					createActor(DFClusterActor.NAME, DFClusterActor.class, mapParam, 0, 
							DFActorDefine.CONSUME_ALL, true, false);
				}
			}
			final String thName = Thread.currentThread().getName();
			DFActorWrap wrap = null;
			while(_onLoop){
				try{
					wrap = _queueActor.poll();
					if(wrap != null){
						if(wrap.consumeMsg(_consumeType) == 0){ //back to global queue
							_queueActor.offer(wrap);
							//notify worker thread
							_lockActor.lock();
							try{
								_condActor.signal();
							}finally{
								_lockActor.unlock();
							}
						}
					}else{  //no msg now, wait
						//make worker thread wait
						_lockActor.lock();
						try{
							_condActor.await(1000, TimeUnit.MILLISECONDS);
						}finally{
							_lockActor.unlock();
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
	//
	private void _doGlobalQueueNotify(){
		_lockQueueActor.lock();
		try{
			_condQueueActor.signal();
		}finally{
			_lockQueueActor.unlock();
		}
	}
	//
	private void _doGlobalBlockQueueNotify(){
		_lockQueueBlockActor.lock();
		try{
			_condQueueBlockActor.signal();
		}finally{
			_lockQueueBlockActor.unlock();
		}
	}
	//
	private void _doGlobalClusterQueueNotify(){
		_lockQueueClusterActor.lock();
		try{
			_condQueueClusterActor.signal();
		}finally{
			_lockQueueClusterActor.unlock();
		}
	}
	//
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
		private byte subject;
		protected DFActorTimeoutWrapper(int srcId, int subject, int requestId, CbTimeout cb) {
			this.srcId = srcId;
			this.requestId = requestId;
			this.cb = cb;
			this.subject = (byte) subject;
		}
		@Override
		public void onTimeout() {
			send(0, srcId, requestId, subject, 0, null, false, null, cb, false, null, null);
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
	protected boolean isClusterEnable(){
		return _isClusterEnable;
	}
	protected String getNodeName(){
		return _nodeName;
	}
	protected void setNodeName(String nodeName){
		_nodeName = nodeName;
	}
	protected void setNodeType(String nodeType){
		_nodeType = nodeType;
	}
	protected String getNodeType(){
		return _nodeType;
	}
	
	static final Map<String,Integer> s_mapSysActorNameId = new HashMap<>();
	static{
		s_mapSysActorNameId.put(DFActorDefine.ACTOR_NAME_LOG, DFActorDefine.ACTOR_ID_LOG);
	}
}
