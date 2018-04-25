package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbMsgReq;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
import fun.lib.actor.po.ActorProp;

/**
 * block类型actor示例
 * @author lostsky
 *
 */
public final class BlockActor {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动配置参数
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setBlockWorkerThreadNum(1);  //设置block线程数量
		//启动入口actor，开始消息循环	
		mgr.start(cfg, ActorProp.newProp()
						.name("LogicActor")
						.classz(LogicActor.class)
						.scheduleMilli(1000)); //开启schedule,周期1000ms
	}
	//逻辑actor
	private static class LogicActor extends DFActor{
		public LogicActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			log.info("LogicActor onStart");
			//create block actor
			sys.createActor(ActorProp.newProp()
					.name("DbActor")
					.classz(DbActor.class)
					.blockActor(true));
		}
		@Override
		public void onSchedule(long dltMilli) {
			log.info("LogicActor send io task,  curThread="+Thread.currentThread().getName());
			//send io task
			sys.send("DbActor", 1001, new Integer(1999));
		}
	}
	
	//block actor
	private static class DbActor extends DFActor{
		public DbActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			log.info("BlockActor onStart");
		}
		@Override
		public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
			if(cmd == 1001){ //io操作 比如数据库操作
				int param = (Integer)payload;
				// do io
				log.info("do io work! param="+param+",  curThread="+Thread.currentThread().getName());
			}
			return 0;
		}
	}

}
