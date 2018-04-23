package fun.lib.actor.api;

public interface DFActorUdpDispatcher {
	
	/**
	 * 获取要转发到的actorId(io线程中回调)
	 * @param pack udp包
	 * @return
	 */
	public int onQueryMsgActorId(Object msg);
}
