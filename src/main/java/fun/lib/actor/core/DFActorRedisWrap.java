package fun.lib.actor.core;

import fun.lib.actor.api.DFActorRedis;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

public final class DFActorRedisWrap implements DFActorRedis{
	private final DFDbManager dbMgr;
	
	private String lastError = null;
	
	protected DFActorRedisWrap() {
		dbMgr = DFDbManager.get();
	}
	@Override
	public int initPool(DFRedisCfg cfg) {
		try {
			return dbMgr.initRedisPool(cfg);
		} catch (Throwable e) {
			lastError = e.getMessage();
		}
		return -1;
	}
	@Override
	public Jedis getConn(int id) {
		return dbMgr.getRedisConn(id);
	}
	@Override
	public void closePool(int id) {
		dbMgr.closeRedisPool(id);
	}
	@Override
	public void closeConn(Jedis conn) {
		if(conn != null){
			conn.close();
		}
	}
	@Override
	public String getLastError() {
		return lastError;
	}

}
