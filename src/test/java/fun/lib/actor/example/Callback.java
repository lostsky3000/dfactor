package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbMsgRsp;
import fun.lib.actor.api.cb.CbMsgReq;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.ActorProp;

/**
 * 演示call的用法，异步回调完成actor间通信
 * @author lostsky
 *
 */
public final class Callback {

	public static void main(String[] args) {
		DFActorManager.get().start(Man.class);
	}
	
	//人，发起计算请求
	private static class Man extends DFActor{
		@Override
		public void onStart(Object param) {
			//create computer
			sys.createActor("Computer", Computer.class);
			//
			timer.timeout(1000, 1);
		}
		
		@Override
		public void onTimeout(int requestId) {
			//start calc query
			log.info("ask to calc 100 + 50 = ? ...");
			sys.call("Computer", 1001, new String("add,100,50"), new CbMsgRsp() {
				@Override
				public int onCallback(int cmd, Object payload) {
					log.info("recv result: 100 + 50 = "+payload+", curThread="+Thread.currentThread().getName());
					return MSG_AUTO_RELEASE;
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
		public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
			if(cb != null){
				log.info("recv req, curThread="+Thread.currentThread().getName());
				String[] arrReq = ((String)payload).split(",");
				if(arrReq[0].equals("add")){
					int result = Integer.parseInt(arrReq[1]) + Integer.parseInt(arrReq[2]);
					cb.callback(1002, new Integer(result));
				}
			}
			return MSG_AUTO_RELEASE;
		}
		public Computer(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		
	}
}
