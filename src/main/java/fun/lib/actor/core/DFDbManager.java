package fun.lib.actor.core;

import java.sql.Connection;
import java.sql.SQLException;

import com.mongodb.client.MongoDatabase;

import fun.lib.actor.po.DFDbCfg;
import fun.lib.actor.po.DFMongoCfg;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

public final class DFDbManager {

	private static final DFDbManager instance = new DFDbManager();
	
	private final DFRedisManager redisMgr;
	private final DFMysqlManager mysqlMgr;
	private final DFMongoManager mongoMgr;
	
	private DFDbManager(){
		redisMgr = new DFRedisManager();
		mysqlMgr = new DFMysqlManager();
		mongoMgr = new DFMongoManager();
	}
	protected static DFDbManager get(){
		return instance;
	}
	
	protected void closeAllPool(){
		this.closeAllRedisPool();
		this.closeAllDbPool();
		this.closeAllMongoPool();
	}
	
	//mongodb
	protected int initMongoPool(DFMongoCfg cfg){
		return mongoMgr.initPool(cfg);
	}
	protected MongoDatabase getMongoDatabase(int id, String dbName){
		return mongoMgr.getDatabase(id, dbName);
	}
	protected void closeMongoPool(int id){
		mongoMgr.closePool(id);
	}
	protected void closeAllMongoPool(){
		mongoMgr.closeAllPool();
	}	
	
	//db(mysql)
	protected int initDbPool(DFDbCfg cfg) throws Throwable{
		return mysqlMgr.initPool(cfg);
	}
	protected Connection getDbConn(int id) throws SQLException{
		return mysqlMgr.getConn(id);
	}
	protected void closeDbPool(int id){
		mysqlMgr.closePool(id);
	}
	protected void closeAllDbPool(){
		mysqlMgr.closeAllPool();
	}
	
	//redis
	protected int initRedisPool(DFRedisCfg cfg) throws Throwable{
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
