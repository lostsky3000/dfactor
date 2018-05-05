package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbMsgReq;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;

/**
 * actor通信api sendback使用示例
 * @author lostsky
 *
 */
public final class Sendback {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动入口actor，开始消息循环		
		mgr.start(EntryActor.class);
	}
	
	private static class EntryActor extends DFActor{
		private int backId = 0;
		@Override
		public void onStart(Object param) {
			backId = sys.createActor(BackActor.class);
			//timeout
			timer.timeout(1000, 1);
		}
		@Override
		public void onTimeout(int requestId) {
			//send req to backActor
			sys.send(backId, 1001, new String("reqData"));
			//continue timeout
			timer.timeout(1000, 1);
		}	
		@Override
		public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
			log.info("recv back msg, cmd="+cmd);
			return MSG_AUTO_RELEASE;
		}
		
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
	}
	
	//
	private static class BackActor extends DFActor{
		@Override
		public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
			log.info("recv req msg, cmd="+cmd+", data="+payload);
			//sendback
			sys.sendback(1002, null);
			return MSG_AUTO_RELEASE;
		}
		public BackActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
	}

}
