package fun.lib.actor.api.http;

import java.net.InetSocketAddress;

public interface DFHttpDispatcher {

	/**
	 * 获取要转发到的actorId(io线程中回调)
	 * @param port 监听的端口号
	 * @param addrRemote 远程连接地址
	 * @param msg 消息
	 * @return 接收消息的actorId, 0则不分发
	 */
	public int onQueryMsgActorId(int port, InetSocketAddress addrRemote, Object msg);
}
