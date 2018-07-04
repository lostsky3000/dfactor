package fun.lib.actor.core;

import java.sql.Connection;
import java.sql.SQLException;

import fun.lib.actor.api.DFActorDb;
import fun.lib.actor.po.DFDbCfg;

public final class DFActorDbWrap implements DFActorDb{
	private final DFDbManager dbMgr;
	
	private String lastError = null;
	
	protected DFActorDbWrap() {
		dbMgr = DFDbManager.get();
	}
	@Override
	public int initPool(DFDbCfg cfg) {
		try {
			return dbMgr.initDbPool(cfg);
		} catch (Throwable e) {
			lastError = e.getMessage();
		}
		return -1;
	}
	@Override
	public Connection getConn(int id) {
		try {
			return dbMgr.getDbConn(id);
		} catch (SQLException e) {
			lastError = e.getMessage();
		}
		return null;
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
				lastError = e.getMessage();
			}
		}
	}
	@Override
	public String getLastError() {
		return lastError;
	}

}
