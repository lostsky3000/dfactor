package fun.lib.actor.core;

import fun.lib.actor.api.DFActorRedis;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

public final class DFActorRedisWrap implements DFActorRedis{
	private final DFDbManager dbMgr;
	
	protected DFActorRedisWrap() {
		dbMgr = DFDbManager.get();
	}
	@Override
	public int initPool(DFRedisCfg cfg) {
		return dbMgr.initRedisPool(cfg);
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

}
