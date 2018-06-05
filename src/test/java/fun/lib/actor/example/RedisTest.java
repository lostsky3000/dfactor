package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.api.cb.CbActorRsp;
import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

/**
 * 配合BlockActor操作redis示例
 * @author lostsky
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
			//init redisPool
			int poolId = redis.initPool(cfg);
			//create ioActor for redis operation
			final int ioActor = sys.createActor(IoActor.class, poolId);
			//timeout
			CbTimeout cb = new CbTimeout() {
				@Override
				public void onTimeout() {
					//send redis task to ioActor
					sys.call(ioActor, 0, null, new Cb() {
						@Override
						public int onCallback(int cmd, Object payload) {
							log.info(payload.toString());
							return 0;
						}
						@Override
						public int onFailed(int code) {
							log.error("call failed: "+code);
							return 0;
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
		public int onMessage(int cmd, Object payload, int srcId) {
			//do redis stuff
			Jedis j = null;
			try{
				j = redis.getConn(poolId);
				if(j != null){
					String rsp = j.ping();
					sys.ret(cmd, "recv pong from redis: "+rsp);
				}
			}finally{
				redis.closeConn(j);
			}
			return MSG_AUTO_RELEASE;
		}
	}
}
