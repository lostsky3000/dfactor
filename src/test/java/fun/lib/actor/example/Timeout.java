package fun.lib.actor.example;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
/**
 * 定时器使用示例
 * @author lostsky
 *
 */
public final class Timeout {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动配置参数
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setLogicWorkerThreadNum(2);  //设置逻辑线程数量
		//启动入口actor，开始消息循环		
		mgr.start(cfg, "EntryActor", EntryActor.class);
	}

	/**
	 * 入口actor
	 * @author lostsky
	 *
	 */
	static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			//使用自带日志打印
			log.info("EntryActor start, curThread="+Thread.currentThread().getName());
			//启动定时器
			int delay = DFActor.transTimeRealToTimer(1000); //延时1秒(1000毫秒)
			sys.timeout(delay, 1);
		}
		
		private int timeoutCount = 0;  //计数器
		@Override
		public void onTimeout(int requestId) {
			log.info("onTimeout, count="+(++timeoutCount)+", requestId="+requestId+", curThread="+Thread.currentThread().getName());
			int delay = DFActor.transTimeRealToTimer(1000); //延时1秒(1000毫秒)
			sys.timeout(delay, 1);
		}
		
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			// TODO Auto-generated method stub
			return 0;
		}

		
		
	}
}
