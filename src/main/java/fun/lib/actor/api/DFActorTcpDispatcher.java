package fun.lib.actor.api;

import java.net.InetSocketAddress;

/**
 * tcp事件分发器
 * @author lostsky
 *
 */
public interface DFActorTcpDispatcher {
	/**
	 * 创建连接通知
	 * @param addrRemote 远程连接地址
	 * @return 接收创建连接事件的actorId, 0则不分发
	 */
	public int onConnActive(InetSocketAddress addrRemote);
	/**
	 * 连接断开通知
	 * @param addrRemote 远程连接地址
	 * @return 接收连接断开事件的actorId, 0则不分发
	 */
	public int onConnInactive(InetSocketAddress addrRemote);
	/**
	 * 收到消息通知
	 * @param addrRemote 远程连接地址
	 * @param msg
	 * @return 接收消息的actorId, 0则不分发
	 */
	public int onMessage(InetSocketAddress addrRemote, Object msg);
}
