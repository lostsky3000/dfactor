package fun.lib.actor.example;

import com.mongodb.client.MongoDatabase;

import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.api.cb.CbActorRsp;
import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFMongoCfg;

/**
 * 配合BlockActor操作mongodb示例
 * @author lostsky
 *
 */
public final class MongodbTest {

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
			DFMongoCfg cfg = DFMongoCfg.newCfg()
					.addAddress("127.0.0.1",    //mongodb host
							27017);    //mongodb port
			int poolId = mongo.initPool(cfg);
			//create dbActor to do dbStuff
			final int dbActor = sys.createActor(DbActor.class, poolId);
			
			//timeout
			CbTimeout cb = new CbTimeout() {
				@Override
				public void onTimeout() {
					//send db stuff to dbActor
					sys.call(dbActor, 0, null, new Cb() {
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
					//
					timer.timeout(2000, this);
				}
			};
			timer.timeout(1000, cb);
		}
	}
	
	//use for db stuff
	private static class DbActor extends DFActor{
		public DbActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private int poolId = 0;
		@Override
		public void onStart(Object param) {
			poolId = (int) param;
		}
		@Override
		public int onMessage(int cmd, Object payload, int srcId) {
			MongoDatabase db = mongo.getDatabase(poolId, "db_test");
			sys.ret(0, "recv mongodb rsp, dbName="+db.getName());
			return 0;
		}
	}
}
