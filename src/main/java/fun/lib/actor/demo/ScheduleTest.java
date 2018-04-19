package fun.lib.actor.demo;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;

public class ScheduleTest {

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
		
		//1秒schedule回调一次
		final int scheduleUnit = (int) (1000/DFActor.TIMER_UNIT_MILLI);
		mgr.start(cfg, "ActorScheduleTest", ActorScheduleTest.class, null, scheduleUnit, DFActorDefine.CONSUME_AUTO);

	}
	
	static class ActorScheduleTest extends DFActor{

		public ActorScheduleTest(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}

		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void onStart(Object param) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void onSchedule(long dltMilli) {
			log.debug("onSchedule, dltMilli="+dltMilli);
		}
	}

}
