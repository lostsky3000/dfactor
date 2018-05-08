package fun.lib.actor.api;

import com.mongodb.client.MongoDatabase;

import fun.lib.actor.po.DFMongoCfg;

public interface DFActorMongo {
	/**
	 * 初始化mongodb连接池
	 * @param cfg mongodb连接池牌配置
	 * @return >0 有效的连接池id   <=0 创建连接池失败
	 */
	public int initPool(DFMongoCfg cfg);
	
	/**
	 * 获取一个mongodb数据库
	 * @param id 连接池id
	 * @param dbName 数据库名字
	 * @return
	 */
	public MongoDatabase getDatabase(int id, String dbName);
	/**
	 * 关闭指定连接池并释放资源
	 * @param id 连接池id
	 */
	public void closePool(int id);
}
