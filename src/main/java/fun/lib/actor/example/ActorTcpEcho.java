package fun.lib.actor.example;

import java.util.HashMap;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public final class ActorTcpEcho extends DFActor{

	protected ActorTcpEcho(Integer id, String name, Integer consumeType, Boolean isIoActor) {
		super(id, name, consumeType, isIoActor);
		// TODO Auto-generated constructor stub
	}
	@Override
	public void onStart(Object param) {
		// TODO Auto-generated method stub
		sys.timeout(20, 1);
	}
	
	@Override
	public void onTimeout(int requestId) {
		log.debug("onTimeout");
		final DFTcpServerCfg cfg = new DFTcpServerCfg(13500, 2, 1)
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_RAW);
		net.doTcpListen(cfg, 1);
	}

	private final HashMap<Integer, DFTcpChannel> mapChannel = new HashMap<>();
	@Override
	public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
		
		return DFActorDefine.MSG_AUTO_RELEASE;
	}
}
