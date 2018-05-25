package fun.lib.actor.po;

import fun.lib.actor.api.DFUdpDecoder;
import io.netty.channel.EventLoopGroup;

public final class DFUdpServerCfg {
	public final int port;
	public final int ioThreadNum;
	public final boolean isBroadcast;
	public final EventLoopGroup ioGroup;
	
	private int soRecvBuf = 1024*20;
	private int soSendBuf = 1024*20;
	
	private volatile DFUdpDecoder decoder = null;
	
	public DFUdpServerCfg(int port, int ioThreadNum, boolean isBroadcast) {
		this.port = port;
		ioThreadNum = Math.max(1, ioThreadNum);
		this.ioThreadNum = ioThreadNum;
		this.isBroadcast = isBroadcast;
		ioGroup = null;
	}
	public DFUdpServerCfg(int port, boolean isBroadcast, EventLoopGroup ioGroup){
		this.port = port;
		this.ioGroup = ioGroup;
		this.isBroadcast = isBroadcast;
		this.ioThreadNum = 0;
	}
	
	public int getSoSendBuf(){
		return soSendBuf;
	}
	public DFUdpServerCfg setSoSendBuf(int soSendBuf){
		this.soSendBuf = soSendBuf;
		return this;
	}
	
	public int getSoRecvBuf(){
		return soRecvBuf;
	}
	public DFUdpServerCfg setSoRecvBuf(int soRecvBuf){
		this.soRecvBuf = soRecvBuf;
		return this;
	}

	public DFUdpDecoder getDecoder() {
		return decoder;
	}

	public DFUdpServerCfg setDecoder(DFUdpDecoder decoder) {
		this.decoder = decoder;
		return this;
	}
	
}




