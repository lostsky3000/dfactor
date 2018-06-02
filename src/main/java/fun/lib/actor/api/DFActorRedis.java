package fun.lib.actor.api;

import fun.lib.actor.po.DFRedisCfg;
import redis.clients.jedis.Jedis;

public interface DFActorRedis {
	/**
	 * 初始化redis连接池
	 * @param cfg redis连接池牌配置
	 * @return 大于 有效的连接池id   小于等于 创建连接池失败
	 */
	public int initPool(DFRedisCfg cfg);
	
	/**
	 * 获取一个redis连接
	 * @param id 连接池id
	 * @return 连接
	 */
	public Jedis getConn(int id);
	
	/**
	 * 关闭连接
	 * @param conn 连接对象
	 */
	public void closeConn(Jedis conn);
	
	/**
	 * 关闭指定连接池并释放资源
	 * @param id 连接池id
	 */
	public void closePool(int id);
}
