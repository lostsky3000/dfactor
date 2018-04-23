package fun.lib.actor.api;

import java.net.InetSocketAddress;

/**
 * tcp事件分发器
 * @author lostsky
 *
 */
public interface DFActorTcpDispatcher {
	/**
	 * 创建连接通知(io线程中回调)
	 * @param requestId 创建连接时的唯一标识
	 * @param channelId 会话id
	 * @param addrRemote 远程连接地址
	 * @return 接收创建连接事件的actorId, 0则不分发
	 */
	public int onConnActiveUnsafe(int requestId, int channelId, InetSocketAddress addrRemote);
	/**
	 * 连接断开通知(io线程中回调)
	 * @param requestId 创建连接时的唯一标识
	 * @param channelId 会话id
	 * @param addrRemote 远程连接地址
	 * @return 接收连接断开事件的actorId, 0则不分发
	 */
	public int onConnInactiveUnsafe(int requestId, int channelId, InetSocketAddress addrRemote);
	/**
	 * 收到消息通知(io线程中回调)
	 * @param requestId 创建连接时的唯一标识
	 * @param channelId 会话id
	 * @param addrRemote 远程连接地址
	 * @param msg
	 * @return 接收消息的actorId, 0则不分发
	 */
	public int onMessageUnsafe(int requestId, int channelId, InetSocketAddress addrRemote, Object msg);
}
