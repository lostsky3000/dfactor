package fun.lib.actor.core;

import java.sql.Connection;

import fun.lib.actor.po.DFDbCfg;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

public final class DFDbManager {

	private static final DFDbManager instance = new DFDbManager();
	
	private final DFRedisManager redisMgr;
	private final DFMysqlManager mysqlMgr;
	
	private DFDbManager(){
		redisMgr = new DFRedisManager();
		mysqlMgr = new DFMysqlManager();
	}
	protected static DFDbManager get(){
		return instance;
	}
	
	protected void closeAllPool(){
		this.closeAllRedisPool();
		this.closeAllDbPool();
	}
	
	//db(mysql)
	protected int initDbPool(DFDbCfg cfg){
		return mysqlMgr.initPool(cfg);
	}
	protected Connection getDbConn(int id){
		return mysqlMgr.getConn(id);
	}
	protected void closeDbPool(int id){
		mysqlMgr.closePool(id);
	}
	protected void closeAllDbPool(){
		mysqlMgr.closeAllPool();
	}
	
	//redis
	protected int initRedisPool(DFRedisCfg cfg){
		return redisMgr.initPool(cfg);
	}
	protected Jedis getRedisConn(int id){
		return redisMgr.getConn(id);
	}
	protected void closeRedisPool(int id){
		redisMgr.closePool(id);
	}
	protected void closeAllRedisPool(){
		redisMgr.closeAllPool();
	}
	
}
