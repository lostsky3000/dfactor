package fun.lib.actor.api;

public interface DFTcpChannel {
	/**
	 * 获取远程连接主机地址
	 * @return
	 */
	public String getRemoteHost();
	/**
	 * 获取远程连接端口
	 * @return
	 */
	public int getRemotePort();
	/**
	 * 发送消息
	 * @param msg
	 * @return
	 */
	public int write(Object msg);
	
	/**
	 * 返回http错误响应
	 * @param statusCode 状态码，如404,200等
	 * @return
	 */
	public int writeHttpResponse(int statusCode);
	
	/**
	 * 关闭连接
	 */
	public void close();
	/**
	 * 连接是否关闭
	 * @return
	 */
	public boolean isClosed();
	/**
	 * 获取当前连接的id
	 * @return
	 */
	public int getChannelId();
	/**
	 * 获取连接建立时间
	 * @return
	 */
	public long getOpenTime();
	
	
	//
	public void setStatusActor(int actorId);
	public void setMessageActor(int actorId);
}
