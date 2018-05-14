package fun.lib.actor.example;

import fun.lib.actor.api.cb.CallHereContext;
import fun.lib.actor.api.cb.CbCallHere;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;

/**
 * 利用异步回调机制，在一个actor中编写另一个actor业务代码的示例
 * @author lostsky
 *
 */
public final class CallHere {

	public static void main(String[] args) {
		DFActorManager.get().start(EntryActor.class);
	}
	
	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		
		@Override
		public void onStart(Object param) {
			sys.createActor("OtherActor", OtherActor.class);
			//
			timer.timeout(1000, 0);
		}
		@Override
		public void onTimeout(int requestId) {
			final int reqId = 2409;
			sys.callHere("OtherActor", 1001, "reqData", new CbCallHere() {
				@Override
				public void inOtherActor(int cmd, Object payload, CallHereContext ctx) {
					//由OtherActor的当前调用线程回调，此时不能修改EntryActor数据
					ctx.getLog().info("CurActor="+ctx.getActorName()+", reqId="+reqId+", cmd="+cmd+", payload="+payload);
					ctx.callback(1002, "rspData");
				}
				@Override
				public void onCallback(int cmd, Object payload) {
					//由EntryActor当前调用线程回调
					log.info("RecvRsp: cmd="+cmd+", payload="+payload);
				}
			});
		}
	}
	
	private static class OtherActor extends DFActor{
		public OtherActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		
	}
}
