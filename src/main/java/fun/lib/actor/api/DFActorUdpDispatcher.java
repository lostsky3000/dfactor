package fun.lib.actor.api;

public interface DFActorUdpDispatcher {
	
	/**
	 * 获取要转发到的actorId(io线程中回调)
	 * @param msg udp包
	 * @return 目标actorId
	 */
	public int onQueryMsgActorId(Object msg);
}
