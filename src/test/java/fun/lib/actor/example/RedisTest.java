package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbMsgReq;
import fun.lib.actor.api.cb.CbMsgRsp;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

/**
 * 配合BlockActor操作redis示例
 * @author admin
 *
 */
public final class RedisTest {

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
			DFRedisCfg cfg = DFRedisCfg.newCfg(
					"127.0.0.1",  //redis host 
					7396, 		  //redis port
					"redisauth"); //redis password
			//get redisPoolId
			int poolId = redis.initPool(cfg);
			//create ioActor for redis operation
			final int ioActor = sys.createActor(IoActor.class, poolId);
			//timeout
			CbTimeout cb = new CbTimeout() {
				@Override
				public void onTimeout() {
					//send redis task to ioActor
					sys.call(ioActor, 0, null, new CbMsgRsp() {
						@Override
						public int onCallback(int cmd, Object payload) {
							//recv redis task from ioActor
							log.info(payload.toString());
							return MSG_AUTO_RELEASE;
						}
					});
					timer.timeout(1000, this);
				}
			};
			timer.timeout(1000, cb);
		}
	}
	
	private static class IoActor extends DFActor{
		public IoActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private int poolId = 0;
		@Override
		public void onStart(Object param) {
			//get redis poolId
			poolId = (int) param;
		}
		@Override
		public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
			//do redis stuff
			Jedis j = null;
			try{
				j = redis.getConn(poolId);
				if(j != null){
					String rsp = j.ping();
					cb.callback(cmd, "recv pong from redis: "+rsp);
				}
			}finally{
				if(j != null){
					j.close();
				}
			}
			return MSG_AUTO_RELEASE;
		}
	}
}
