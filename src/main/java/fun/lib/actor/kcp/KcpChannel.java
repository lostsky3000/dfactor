package fun.lib.actor.kcp;

import io.netty.buffer.ByteBuf;

public interface KcpChannel {

	public int write(ByteBuf bufSend);
	public int getConnId();
	public void close();
}
