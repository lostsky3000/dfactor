package fun.lib.actor.example;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
/**
 * 定时回调示例
 * @author lostsky
 *
 */
public final class Schedule {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动配置参数
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setLogicWorkerThreadNum(2);  //设置逻辑线程数量
		//启动入口actor，开始消息循环		
		int interval = DFActor.transTimeRealToTimer(1000); //1秒schedule一次
		mgr.start(cfg, "EntryActor", EntryActor.class, null, interval);
	}

	/**
	 * 入口actor
	 * @author lostsky
	 *
	 */
	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			//使用自带日志打印
			log.info("EntryActor start,   curThread="+Thread.currentThread().getName());
			
		}
		@Override
		public void onSchedule(long dltMilli) {
			log.info("onSchedule, dltMilli="+dltMilli+",  curThread="+Thread.currentThread().getName());
		}
		
	}
}
