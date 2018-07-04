package fun.lib.actor.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import com.funtag.util.cache.DFRedisUtil;
import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public final class DFRedisManager {
	protected DFRedisManager() {
		// TODO Auto-generated constructor stub
	}
	
	private final ReentrantReadWriteLock lockRedis = new ReentrantReadWriteLock();
	private final ReadLock lockRedisRead = lockRedis.readLock();
	private final WriteLock lockRedisWrite = lockRedis.writeLock();
	private final HashMap<Integer,RedisPoolWrap> mapRedis = new HashMap<>();
	private final HashMap<String,Integer> mapRedisId = new HashMap<>();
	
	private int redisIdCount = 1;
	protected int initPool(DFRedisCfg cfg) throws Throwable{
		lockRedisRead.lock();
		try{
			Integer existId = mapRedisId.get(cfg.strId);
			if(existId != null){  //pool exist
				return existId;
			}
		}finally{
			lockRedisRead.unlock();
		}
		//
		JedisPool pool = DFRedisUtil.createJedisPool(
				cfg.getHost(), cfg.getPort(), cfg.getAuth(),
				cfg.getMaxTotal(), cfg.getMaxIdle(), cfg.getMinIdle(),
				cfg.getConnTimeoutMilli(), cfg.getBorrowTimeoutMilli());
		//test connection
		boolean testFail = false;
		Jedis conn = null;
		Throwable eOut = null;
		try{
			conn = pool.getResource();
			conn.ping();
		}catch(Throwable e){
			testFail = true;
			eOut = e;
		}finally{
			if(conn != null){
				conn.close();
			}
			if(testFail && pool != null){
				pool.close();
			}
		}
		if(eOut != null){
			throw eOut;
		}
		//
		RedisPoolWrap wrap = new RedisPoolWrap(pool, cfg.strId);
		lockRedisWrite.lock();
		try{
			int curId = redisIdCount;
			mapRedis.put(curId, wrap);
			mapRedisId.put(cfg.strId, curId);
			if(redisIdCount >= Integer.MAX_VALUE){
				redisIdCount = 1;
			}else{
				++redisIdCount;
			}
			return curId;
		}finally{
			lockRedisWrite.unlock();
		}
	}
	protected Jedis getConn(int id){
		Jedis j = null;
		lockRedisRead.lock();
		try{
			final RedisPoolWrap pool = mapRedis.get(id);
			if(pool != null){
				j = pool.getConn();
			}
		}catch(Throwable e){
			e.printStackTrace();
		}
		finally{
			lockRedisRead.unlock();
		}
		return j;
	}
	protected void closePool(int id){
		RedisPoolWrap wrap = null;
		lockRedisWrite.lock();
		try{
			wrap = mapRedis.remove(id);	
			if(wrap != null){
				mapRedisId.remove(wrap.strId);
			}
		}finally{
			lockRedisWrite.unlock();
		}
		if(wrap != null){
			wrap.close();
		}
	}
	protected void closeAllPool(){
		lockRedisWrite.lock();
		try{
			Iterator<RedisPoolWrap> it = mapRedis.values().iterator();
			while(it.hasNext()){
				RedisPoolWrap wrap = it.next();
				wrap.close();
			}
			mapRedis.clear();
			mapRedisId.clear();
		}finally{
			lockRedisWrite.unlock();
		}
	}
	
	class RedisPoolWrap{
		private JedisPool pool = null;
		private final String strId;
		public RedisPoolWrap(JedisPool p, String strId) {
			pool = p;
			this.strId = strId;
		}
		private Jedis getConn(){
			Jedis j = null;
			if(pool != null){
				try{
					j = pool.getResource();
				}catch(Throwable e){
					throw e;
				}
			}
			return j;
		}
		private void close(){
			if(pool != null){
				pool.close();
				pool = null;
			}
		}
	}
		
}
