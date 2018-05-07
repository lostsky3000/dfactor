package fun.lib.actor.core;

import java.sql.Connection;
import java.sql.SQLException;

import fun.lib.actor.api.DFActorDb;
import fun.lib.actor.po.DFDbCfg;

public final class DFActorDbWrap implements DFActorDb{
	private final DFDbManager dbMgr;
	
	protected DFActorDbWrap() {
		dbMgr = DFDbManager.get();
	}
	@Override
	public int initPool(DFDbCfg cfg) {
		return dbMgr.initDbPool(cfg);
	}
	@Override
	public Connection getConn(int id) {
		return dbMgr.getDbConn(id);
	}
	@Override
	public void closePool(int id) {
		dbMgr.closeDbPool(id);
	}
	@Override
	public void closeConn(Connection conn) {
		if(conn != null){
			try {
				conn.close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

}
