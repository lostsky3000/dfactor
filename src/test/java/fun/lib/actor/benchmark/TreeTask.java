package fun.lib.actor.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorManagerConfig;

/**
 * 树状actor压力测试，从根节点开始依次创建 MAX_DEPTH 层深的树
 * 每一层的节点数量为 LEAF_NUM
 * 每次任务从根节点开始，向下请求，到达最底层节点时，向上返回结果消息，直到到达根节点
 * 默认树深度为6，每层节点数为10，总共创建 1111111(N) 个actor
 * 一次消息请求，所有子节点都会收到一次请求一次响应(最底层只收到一次请求)
 * 每个节点收到请求和收到响应都会调用_doTask，模拟业务逻辑的消耗(数组的随机打乱+排序)
 * 使用jvisualvm 观察程序运行时，线程和gc情况，如果出现fullGC，需增加jvm分配内存
 * 
 * 可以做下试验：
 * 将_doTask()内实现注释掉，观察框架任务调度本身的消耗
 * 调整启动参数的setLogicWorkerThreadNum()，调整逻辑线程数
 * 观察只有任务调度消耗和包含了逻辑处理消耗时，不同的逻辑线程数的性能
 * 
 * @author lostsky
 *
 */
public final class TreeTask {
	/**
	 * 任务树的深度
	 */
	private static final int MAX_DEPTH = 6;
	/**
	 * 任务树每层节点数量
	 */
	private static final int LEAF_NUM = 10;
	
	public static void main(String[] args) {
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setBlockWorkerThreadNum(0)  //不需要创建block线程池
				.setClientIoThreadNum(0)     //不需要创建对外网络连接线程池
				.setLogicWorkerThreadNum(4); //设置逻辑线程数量   Runtime.getRuntime().availableProcessors());
		ActorProp prop = ActorProp.newProp()
				.classz(TaskActor.class)
				.param(new TaskCfg(1, 0, 0));
		DFActorManager.get().start(cfg, prop);
	}

	private static class TaskActor extends DFActor{
		public TaskActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private TaskCfg _cfg = null;
		private int[] _arrNodeId = null;  //当前全部子节点的actorId
		@Override
		public void onStart(Object param) {
			_cfg = (TaskCfg) param;
			_arrNodeId = new int[LEAF_NUM];
			if(_cfg.depth <= MAX_DEPTH){  //还未到最底层，继续创建子节点
				int root = 0;
				if(_cfg.depth == 1){ //root
					root = this.id;
				}
				int nextDepth = _cfg.depth + 1; //子节点的深度
				for(int i=0; i<LEAF_NUM; ++i){
					_arrNodeId[i] = sys.createActor(TaskActor.class, new TaskCfg(nextDepth, this.id, root));
				}
			}else{ //到达最底层，向上通知节点创建完毕
				sys.send(_cfg.parent, CMD_LEAF_START, 1);
			}
		}
		//
		private long _tmTaskBegin = 0;  //任务开始时间
		private long _tmTaskEnd = 0;    //任务结束时间
		//
		private int _leafDoneCount = 0;
		private int _childrenCount = 0;
		//
		@Override
		public int onMessage(int srcId, int cmd, Object payload, CbActorReq cb) {
			if(cmd == CMD_LEAF_START){
				_childrenCount += (int) payload;
				if(++_leafDoneCount >= LEAF_NUM){
					if(_cfg.depth == 1){ //当前是根节点
						log.info("all node start done, allNodeNum="+(_childrenCount+1));
						//开始新的任务
						_reqNewTask();
					}else{ //非根节点，继续向上报告节点创建完成
						sys.send(_cfg.parent, CMD_LEAF_START, _childrenCount+1);
					}
				}
			}else if(cmd == CMD_REQ){ //收到父节点任务请求，向子节点转发
				_doTask();  //模拟逻辑处理消耗cpu
				if(_cfg.depth <= MAX_DEPTH){ //还未到底部，继续向下分发任务
					_leafDoneCount = 0;
					int len = _arrNodeId.length;
					for(int i=0; i<len; ++i){
						sys.send(_arrNodeId[i], cmd, payload);
					}
				}else{ //到达底部，向上返回结果
					sys.send(_cfg.parent, CMD_RSP, payload);
				}
			}else if(cmd == CMD_RSP){  //收到子节点任务响应
				_doTask();  //模拟逻辑处理消耗cpu
				if(++_leafDoneCount >= LEAF_NUM){  //所有直属子节点都返回了结果
					if(_cfg.depth > 1){ //当前不是根节点，继续向上返回结果
						sys.send(_cfg.parent, CMD_RSP, payload);
					}else{ //已到达根节点，统计耗时
						_tmTaskEnd = System.currentTimeMillis();
						log.info("all task done, tmCost="+(_tmTaskEnd - _tmTaskBegin)+"ms");
						//开始新的任务请求
						_reqNewTask();
					}
				}
			}
			return 0;
		}
		
		/**
		 * 开始新的任务，从根节点逐级向下请求
		 */
		private void _reqNewTask(){
			_tmTaskBegin = System.currentTimeMillis();  //记录任务开始时间
			_leafDoneCount = 0;
			log.info("start new task...");
			int len = _arrNodeId.length;
			for(int i=0; i<len; ++i){   //向直属子节点派发任务
				sys.send(_arrNodeId[i], CMD_REQ, 1);
			}
		}
		
		//
		private ArrayList<Byte> _lsTaskNum = new ArrayList<>(30);
		private Random _rand = new Random();
		//模拟逻辑处理 消耗cpu
		private void _doTask(){
			if(_lsTaskNum.isEmpty()){  //队列为空，填充随机数据（只执行一次）
				for(int i=0; i<30; ++i){
					_lsTaskNum.add((byte)_rand.nextInt());
				}
			}else{	//队列不为空  随机打乱次序
				int idx = 0;
				byte tmp = 0;
				for(int i=1; i<30; ++i){
					idx = i + _rand.nextInt(30 - i);
					tmp = _lsTaskNum.get(i-1);
					_lsTaskNum.set(i-1, _lsTaskNum.get(idx));
					_lsTaskNum.set(idx, tmp);
				}
			}
			//队列排序
			Collections.sort(_lsTaskNum, _cmpTask);
		}
		private final Comparator<Byte> _cmpTask = new Comparator<Byte>() {
			@Override
			public int compare(Byte o1, Byte o2) {
				if(o1 < o2){
					return -1;
				}else if(o1 > o2){
					return 1;
				}
				return 0;
			}
		};
		
	}
	
	/**
	 * 
	 * @author lostsky
	 *
	 */
	private static class TaskCfg{
		private int depth = 0;   //当前actor所处深度
		private int parent = 0;  //当前actor父节点的actorId
		private int root = 0;    //根节点的actorId
		public TaskCfg(int depth, int parent, int root) {
			this.depth = depth;
			this.parent = parent;
			this.root = root;
		}
	}
	
	/**
	 * 直属子节点创建完毕消息
	 */
	private static final int CMD_LEAF_START = 10001;
	/**
	 * 任务请求消息
	 */
	private static final int CMD_REQ = 10002;
	/**
	 * 任务响应消息
	 */
	private static final int CMD_RSP = 10003;
	
	
}
