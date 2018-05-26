package fun.lib.actor.api.cb;

public interface RpcContext {

	/**
	 * 向调用方返回结果
	 * @param cmd 消息码
	 * @param payload 消息体
	 */
	public void response(int cmd, Object payload);
	
	/**
	 * 是否来自集群内其它结点调用
	 * @return
	 */
	public boolean isRemote();
	
	/**
	 * 获取调用方结点名字(集群内远程调用时有值)
	 * @return
	 */
	public String getSrcNode();
	
	/**
	 * 调用方actor名字
	 * @return
	 */
	public String getSrcActor();
}
