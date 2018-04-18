package com.funtag.util.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class DFRedisUtil {

	public static JedisPool createJedisPool(String host, int port, String auth,
			int maxTotal, int maxIdle, int minIdle){
		JedisPoolConfig config = new JedisPoolConfig();  
        //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。  
        config.setMaxTotal(maxTotal); 
        //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。  
        config.setMaxIdle(maxIdle);  
        config.setMinIdle(minIdle);
        //表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；  
        config.setMaxWaitMillis(1000*15);
        //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；  
        config.setTestOnBorrow(true);  
        return new JedisPool(config, host, port, 5000, auth);
	}
}
