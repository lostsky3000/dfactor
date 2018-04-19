package fun.lib.actor.demo;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;

public class ActorConnTest {

	public static void main(String[] args) {
		//
		final DFActorManager mgr = DFActorManager.get();
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				//.setTimerThreadNum(1)  //定时器线程数，默认为1
				//.setLogLevel(DFActorLogLevel.DEBUG)   //框架日志级别，默认为debug
				//.setBlockWorkerThreadNum(0) //设置阻塞线程的数量(一般用于阻塞io，如数据库io等)，默认为0，不启动
				//.setUseSysLog(useSysLog)   //设置是否使用框架log，默认为true
				//.setClientIoThreadNum(1)     //设置作为客户端向外连接时，通信层使用的线程数
				.setLogicWorkerThreadNum(2);  //设置处理逻辑的线程数量
				
		//启动入口actor，开始事件循环		
		mgr.start(cfg, "ActorConnTestA", ActorConnTestA.class, null, 0, DFActorDefine.CONSUME_AUTO);

	}
	
	static class ActorConnTestA extends DFActor{
		public ActorConnTestA(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}

		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			log.debug("onMessage, srcId="+srcId+", requestId="+requestId+", cmd="+cmd
					+", payload="+payload);
			return 0;
		}
		
		@Override
		public void onStart(Object param) {
			log.debug("ActorA on start");
			//创建actorB
			sys.createActor("ActorConnTestB", ActorConnTestB.class, new Integer(this.id));
			//启动定时器   1秒触发一次
			final int delay = (int) (1000/DFActor.TIMER_UNIT_MILLI);
			sys.timeout(delay, 10000);
		}
		@Override
		public void onTimeout(int requestId) {
			sys.send("ActorConnTestB", 10001, 10002, new String("msg from actorA, "+System.currentTimeMillis()));
			//启动下一个定时器
			final int delay = (int) (1000/DFActor.TIMER_UNIT_MILLI);
			sys.timeout(delay, requestId);
		}
	}
	
	static class ActorConnTestB extends DFActor{
		public ActorConnTestB(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			log.debug("onMessage, srcId="+srcId+", requestId="+requestId+", cmd="+cmd
					+", payload="+payload);
			return 0;
		}

		private int _actorAId = 0;
		@Override
		public void onStart(Object param) {
			log.debug("ActorB on start");
			_actorAId = (Integer)param;
			// TODO Auto-generated method stub
			final int delay = (int) (1000/DFActor.TIMER_UNIT_MILLI);
			sys.timeout(delay, 10000);
		}
		@Override
		public void onTimeout(int requestId) {
			//发送消息给actorA
			sys.send(_actorAId, 20001, 20002, new String("msg from actorB, "+System.currentTimeMillis()));
			
			//启动下一个定时器
			final int delay = (int) (1000/DFActor.TIMER_UNIT_MILLI);
			sys.timeout(delay, requestId);
		}
		
	}
}
