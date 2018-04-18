package fun.lib.actor.kcp;

import io.netty.channel.socket.DatagramPacket;

public interface KcpListener {

	public void onOutput(DatagramPacket pack);
	
	public void onInput(DatagramPacket pack, KcpChannel kcpChannel);
	
	public void onChannelActive(KcpChannel kcpChannel, int connId);
	public void onChannelInactive(KcpChannel kcpChannel, int code);
}
