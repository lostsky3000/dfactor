package fun.lib.actor.api;

import io.netty.channel.socket.DatagramPacket;

public interface DFUdpDispatcher {
	public int queryMsgActorId(DatagramPacket pack);
}
