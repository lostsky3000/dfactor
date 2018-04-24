package fun.lib.actor.example;

import fun.lib.actor.api.DFActorMsgCallback;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;

/**
 * 演示call的用法，异步回调完成actor间通信
 * @author admin
 *
 */
public final class Call {

	public static void main(String[] args) {
		DFActorManager.get().start("Man", Man.class);
	}
	
	//人，发起计算请求
	private static class Man extends DFActor{
		@Override
		public void onStart(Object param) {
			//create computer
			sys.createActor("Computer", Computer.class);
			//
			sys.timeout(DFActor.transTimeRealToTimer(1000), 1);
		}
		@Override
		public void onTimeout(int requestId) {
			//start calc query
			log.info("ask to calc 100 + 50 = ? ...");
			sys.call("Computer", 1001, new String("add,100,50"), new DFActorMsgCallback() {
				@Override
				public int onCallback(int cmd, Object payload) {
					if(cmd == 1002){
						log.info("recv result: 100 + 5 = "+payload);
					}
					return 0;
				}
			});
		}
		
		public Man(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		
	}
	
	
	//计算机，运算并返回结果
	private static class Computer extends DFActor{
		@Override
		public int onMessage(int srcId, int cmd, Object payload) {
			if(cmd == 1001){
				String[] arrReq = ((String)payload).split(",");
				if(arrReq[0].equals("add")){
					int result = Integer.parseInt(arrReq[1]) + Integer.parseInt(arrReq[2]);
					sys.callback(1002, new Integer(result));
				}
			}
			return DFActorDefine.MSG_AUTO_RELEASE;
		}
		public Computer(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		
	}
}
