package fun.lib.actor.api;

public interface DFTcpChannel {
	/**
	 * 获取远程连接主机地址
	 * @return 地址
	 */
	public String getRemoteHost();
	/**
	 * 获取远程连接端口
	 * @return 端口
	 */
	public int getRemotePort();
	/**
	 * 发送消息
	 * @param msg 消息
	 * @return 0
	 */
	public int write(Object msg);
	
	/**
	 * 关闭连接
	 */
	public void close();
	/**
	 * 连接是否关闭
	 * @return 是/否
	 */
	public boolean isClosed();
	/**
	 * 获取当前连接的id
	 * @return id
	 */
	public int getChannelId();
	/**
	 * 获取连接建立时间
	 * @return 时间
	 */ 
	public long getOpenTime();
	
	
	//
	public void setStatusActor(int actorId);
	public void setMessageActor(int actorId);
}
