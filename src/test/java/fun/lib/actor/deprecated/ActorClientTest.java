package fun.lib.actor.deprecated;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import fun.lib.actor.po.DFTcpClientCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public final class ActorClientTest extends DFActor{

	protected ActorClientTest(Integer id, String name, Integer consumeType, Boolean isIoActor) {
		super(id, name, consumeType, isIoActor);
		// TODO Auto-generated constructor stub
	}

	private DFTcpChannel tcpSession = null;
	@Override
	public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
		
		return DFActorDefine.MSG_AUTO_RELEASE;
	}

	@Override
	public void onStart(Object param) {
		log.debug("onStart, param="+param);
		//client connect test
		final DFTcpClientCfg cfg = new DFTcpClientCfg("www.baidu.com", 443);
		net.doTcpConnect(cfg, 1);
	}
	
	@Override
	public void onTimeout(int requestId) {
		
	}

}
