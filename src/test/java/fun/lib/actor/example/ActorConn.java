package fun.lib.actor.example;

import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;

/**
 * actor间通信示例
 * @author lostsky
 *
 */
public final class ActorConn {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		mgr.start(Ask.class);
	}
	
	private static class Ask extends DFActor{
		public Ask(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			sys.createActor("answer", Answer.class);
			timer.timeout(1000, 0);
		}
		private int count = 0;
		@Override
		public void onTimeout(int requestId) {
			if(++count%2 == 0){  //发送消息 无回调
				sys.to("answer", count, "lostsky3000");
			}else{  //发送消息  有回调
				sys.call("answer", count, "lostsky3000", new Cb() {
					@Override
					public int onCallback(int cmd, Object payload) {
						log.info("hasCb: " + payload);
						return 0;
					}
					@Override
					public int onFailed(int code) {
						return 0;
					}
				});
			}
			timer.timeout(1000, 0);
		}
		@Override
		public int onMessage(int cmd, Object payload, int srcId) {
			log.info("noCb: " + payload);
			return 0;
		}
	}
	
	private static class Answer extends DFActor{
		public Answer(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public int onMessage(int cmd, Object payload, int srcId) {
			sys.ret(0, cmd+", hello "+payload+", "+System.currentTimeMillis());
			return 0;
		}
		
	}
}
