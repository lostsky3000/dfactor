package fun.lib.actor.example;

import fun.lib.actor.api.cb.CallHereContext;
import fun.lib.actor.api.cb.CbCallHereBlock;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;

/**
 * 利用异步回调机制+框架提供的BlockActor，简化io操作代码，在业务actor中编写io操作代码
 * 根据此例，可改写MysqlTest.java, MongodbTest.java, RedisTest,java等例子，
 * 实现在业务actor里编写block操作代码，增加代码可读性可维护性
 * @author lostsky
 *
 */
public final class CallHereBlock {

	public static void main(String[] args) {
		DFActorManager.get().start(EntryActor.class);
	}
	
	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			timer.timeout(1000, 0);
		}
		private int count = 0;
		@Override
		public void onTimeout(int requestId) {
			final int userId = 168;  //模拟业务线程要处理一个user信息
			sys.callHereBlock(userId, 1001, "reqData_"+(++count), new CbCallHereBlock() {
				@Override
				public void inBlockActor(int cmd, Object payload, CallHereContext ctx) {
					//由框架block线程回调，此处禁止操作业务actor中的数据，会引发数据安全问题
					ctx.getLog().info("blockActorRecvData: cmd="+cmd+", payload="+payload+", userId="+userId+", curThread="+Thread.currentThread().getName());
					//执行数据库io等操作
					//向业务actor返回结果
					ctx.callback(1002, "rspData: ori="+payload);
				}
				@Override
				public void onCallback(int cmd, Object payload) {
					//业务actor收到数据库操作结果
					log.info("recvBlockRsp, cmd="+cmd+", payload="+payload+", curThread="+Thread.currentThread().getName());
				}
			});
			//
			timer.timeout(1000, 0);
		}
	}

}
