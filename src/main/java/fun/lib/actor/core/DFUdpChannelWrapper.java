package fun.lib.actor.core;

import fun.lib.actor.api.DFUdpChannel;
import fun.lib.actor.kcp.KcpServer;
import io.netty.channel.Channel;

public final class DFUdpChannelWrapper implements DFUdpChannel{

	private volatile Channel channel = null;
	protected DFUdpChannelWrapper(){
		
	}
	private volatile KcpServer kcpServer;
	protected KcpServer getKcpServer(){
		return this.kcpServer;
	}
	protected void setKcpServer(KcpServer kcpServer){
		if(this.kcpServer != null){
			return ;
		}
		this.kcpServer = kcpServer;
	}
	
	protected void onChannelActive(Channel channel){
		if(this.channel != null){
			return ;
		}
		this.channel = channel;
	}
	
	@Override
	public int write(Object msg) {
		if(channel != null){
			channel.writeAndFlush(msg);
			return 0;
		}
		return 1;
	}

}
