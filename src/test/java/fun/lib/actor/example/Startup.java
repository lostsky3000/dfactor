package fun.lib.actor.example;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
/**
 * 快速启动一个dfactor示例
 * @author lostsky
 *
 */
public final class Startup {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动入口actor，开始消息循环		
		mgr.start(EntryActor.class);
	}

	/**
	 * 入口actor
	 * @author lostsky
	 *
	 */
	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			//使用自带日志打印
			log.info("EntryActor start, curThread="+Thread.currentThread().getName());
		}
		
	}
}
