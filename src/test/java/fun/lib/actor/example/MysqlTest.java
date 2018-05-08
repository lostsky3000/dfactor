package fun.lib.actor.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import fun.lib.actor.api.cb.CbMsgReq;
import fun.lib.actor.api.cb.CbMsgRsp;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFDbCfg;

/**
 * 配合BlockActor操作mysql示例
 * @author lostsky
 *
 */
public final class MysqlTest {

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
			DFDbCfg cfg = DFDbCfg.newCfg(
					"127.0.0.1",   //db host 
					3306,    //db port
					"db_test", //db name
					"user",   //db username
					"pwd");   //db password
			int poolId = db.initPool(cfg);
			//create io actor
			final int ioActor = sys.createActor(IoActor.class, poolId);
			//start timer
			CbTimeout cb = new CbTimeout() {
				@Override
				public void onTimeout() {
					//send db task to ioActor
					sys.call(ioActor, 0, null, new CbMsgRsp() {
						@Override
						public int onCallback(int cmd, Object payload) {
							log.info(payload.toString());
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
	
	//ioActor, use for db operation 
	private static class IoActor extends DFActor{
		public IoActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private int poolId = 0;
		@Override
		public void onStart(Object param) {
			poolId = (int) param;
		}
		@Override
		public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
			//do db stuff
			String rsp = null;
			Connection conn = db.getConn(poolId);
			try{
				if(conn != null){
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT NOW()");
					rs.next();
					String dt = rs.getString(1);
					rsp = "recv rsp from db, now=" + dt;
				}
			}catch(Throwable e){
				e.printStackTrace();
			}finally{
				db.closeConn(conn);
			}
			//callback logicActor
			cb.callback(0, rsp);
			return 0;
		}
	}
}
