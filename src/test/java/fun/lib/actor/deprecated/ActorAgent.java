package fun.lib.actor.deprecated;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import io.netty.buffer.ByteBuf;

public final class ActorAgent extends DFActor{

	protected ActorAgent(Integer id, String name, Integer consumeType, Boolean isIoActor) {
		super(id, name, consumeType, isIoActor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
		
		return DFActorDefine.MSG_AUTO_RELEASE;
	}

	private DFTcpChannel tcpSession = null;
	@Override
	public void onStart(Object param) {
		tcpSession = (DFTcpChannel) param;
		
		log.debug("onStart, remote="+tcpSession.getRemoteHost()+":"+tcpSession.getRemotePort());
	}

}
