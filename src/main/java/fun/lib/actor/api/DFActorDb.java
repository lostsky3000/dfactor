package fun.lib.actor.api;

import java.sql.Connection;

import fun.lib.actor.po.DFDbCfg;

public interface DFActorDb {
	/**
	 * 初始化db连接池
	 * @param cfg db连接池牌配置
	 * @return >0 有效的连接池id   <=0 创建连接池失败
	 */
	public int initPool(DFDbCfg cfg);
	
	/**
	 * 获取一个db连接
	 * @param id 连接池id
	 * @return
	 */
	public Connection getConn(int id);
	/**
	 * 关闭连接
	 * @param conn 连接对象
	 */
	public void closeConn(Connection conn);
	/**
	 * 关闭指定连接池并释放资源
	 * @param id 连接池id
	 */
	public void closePool(int id);
}
