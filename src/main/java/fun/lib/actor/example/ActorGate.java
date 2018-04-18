package fun.lib.actor.example;

import java.util.HashMap;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;

public final class ActorGate extends DFActor{

	protected ActorGate(Integer id, String name, Integer consumeType, Boolean isIoActor) {
		super(id, name, consumeType, isIoActor);
		// TODO Auto-generated constructor stub
	}

	private int agentCount = 0;
	private final HashMap<Integer, Integer> mapSessionActor = new HashMap<>(); //tcpSessionId <=> actorId
	@Override
	public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
		
		return DFActorDefine.MSG_AUTO_RELEASE;
	}

	private final int listenPort = 13500;
	@Override
	public void onStart(Object param) {
		// TODO Auto-generated method stub
		DFTcpServerCfg cfg = new DFTcpServerCfg(listenPort, 1, 1)
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_LENGTH);
		net.doTcpListen(cfg, 1);
		//
		sys.timeout(100, 1981);
	}
	
	@Override
	public void onTimeout(int requestId) {
		log.debug("onTimeout, requestId="+requestId);
//		doTcpListenClose(listenPort);
	}

}
