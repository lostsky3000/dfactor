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
	private int redisIdCount = 1;
	protected int initPool(DFRedisCfg cfg){
		JedisPool pool = DFRedisUtil.createJedisPool(
				cfg.getHost(), cfg.getPort(), cfg.getAuth(),
				cfg.getMaxTotal(), cfg.getMaxIdle(), cfg.getMinIdle(),
				cfg.getConnTimeoutMilli(), cfg.getBorrowTimeoutMilli());
		RedisPoolWrap wrap = new RedisPoolWrap(pool);
		lockRedisWrite.lock();
		int curId = redisIdCount;
		try{
			mapRedis.put(curId, wrap);
			if(redisIdCount >= Integer.MAX_VALUE){
				redisIdCount = 1;
			}else{
				++redisIdCount;
			}
		}finally{
			lockRedisWrite.unlock();
		}
		return curId;
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
		}finally{
			lockRedisWrite.unlock();
		}
	}
	
	class RedisPoolWrap{
		private JedisPool pool = null;
		public RedisPoolWrap(JedisPool p) {
			pool = p;
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
